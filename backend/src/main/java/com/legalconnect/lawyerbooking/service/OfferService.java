package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.CreateOfferRequest;
import com.legalconnect.lawyerbooking.dto.OfferDTO;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.entity.Offer;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.OfferStatus;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.OfferRepository;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final CaseRepository caseRepository;
    private final LawyerRepository lawyerRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${payment.max.offers.per.case:5}")
    private int maxOffersPerCase;

    public OfferService(OfferRepository offerRepository, 
                        CaseRepository caseRepository, 
                        LawyerRepository lawyerRepository,
                        UserRepository userRepository,
                        SimpMessagingTemplate messagingTemplate) {
        this.offerRepository = offerRepository;
        this.caseRepository = caseRepository;
        this.lawyerRepository = lawyerRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public OfferDTO submitOffer(Long caseId, CreateOfferRequest request, Long lawyerId) {
        // Validate case exists and is in correct state
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (caseEntity.getCaseStatus() != CaseStatus.PUBLISHED && 
            caseEntity.getCaseStatus() != CaseStatus.UNDER_REVIEW &&
            caseEntity.getCaseStatus() != CaseStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Case is not accepting offers");
        }

        // Validate lawyer is not the case owner
        User caseOwner = userRepository.findById(caseEntity.getUserId())
                .orElseThrow(() -> new RuntimeException("Case owner not found"));
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        if (caseOwner.getUsername().equals(lawyer.getUsername())) {
            throw new RuntimeException("Cannot submit offer for your own case");
        }

        // Check if lawyer already submitted offer
        java.util.Optional<Offer> existingOffer = offerRepository.findByCaseIdAndLawyerId(caseId, lawyerId);
        Offer offer;
        boolean wasWithdrawn = false;
        
        if (existingOffer.isPresent()) {
            offer = existingOffer.get();
            if (offer.getStatus() == OfferStatus.WITHDRAWN) {
                wasWithdrawn = true;
            } else if (offer.getStatus() != OfferStatus.SUBMITTED) {
                throw new RuntimeException("Cannot update offer: Current status is " + offer.getStatus());
            }
        } else {
            // Check offer limit for NEW offers only
            Long offerCount = offerRepository.countByCaseIdAndStatus(caseId, OfferStatus.SUBMITTED);
            if (offerCount >= maxOffersPerCase) {
                throw new RuntimeException("Maximum offers limit reached for this case");
            }
            offer = new Offer();
            offer.setCaseId(caseId);
            offer.setLawyerId(lawyerId);
        }

        offer.setProposedFee(request.getProposedFee());
        offer.setEstimatedTimeline(request.getEstimatedTimeline());
        offer.setProposalMessage(request.getProposalMessage());
        offer.setConsultationType(request.getConsultationType());
        offer.setMilestonePlan(request.getMilestonePlan());
        offer.setStatus(OfferStatus.SUBMITTED);

        Offer savedOffer = offerRepository.save(offer);

        // Update case status and offer count (if new or re-submitted)
        if (existingOffer.isEmpty() || wasWithdrawn) {
            CaseStatus oldStatus = caseEntity.getCaseStatus();
            if (oldStatus == CaseStatus.PUBLISHED || oldStatus == CaseStatus.PENDING_APPROVAL) {
                caseEntity.setCaseStatus(CaseStatus.UNDER_REVIEW);
            }
            
            // Recalculate accurate count
            Long activeOffersCount = offerRepository.countByCaseIdAndStatus(caseId, OfferStatus.SUBMITTED);
            caseEntity.setOfferCount(activeOffersCount.intValue());
            caseRepository.save(caseEntity);

            // Broadcast update
            try {
                messagingTemplate.convertAndSend("/topic/lawyer/updates", Map.of(
                    "type", "CASE_UPDATED",
                    "caseId", caseId,
                    "status", caseEntity.getCaseStatus().name(),
                    "offerCount", caseEntity.getOfferCount()
                ));
            } catch (Exception e) {
                // Non-blocking
            }
        }

        return convertToDTO(savedOffer);
    }

    @Transactional
    public void acceptOffer(Long offerId, Long userId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        Case caseEntity = caseRepository.findById(offer.getCaseId())
                .orElseThrow(() -> new RuntimeException("Case not found"));

        // Validate user owns the case
        if (!caseEntity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Case does not belong to you");
        }

        // Validate case status
        if (caseEntity.getCaseStatus() != CaseStatus.UNDER_REVIEW && caseEntity.getCaseStatus() != CaseStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Case is not in a state where offers can be accepted");
        }

        // Validate offer status
        if (offer.getStatus() != OfferStatus.SUBMITTED) {
            throw new RuntimeException("Offer is no longer available");
        }

        // Accept this offer
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setAcceptedAt(LocalDateTime.now());
        offerRepository.save(offer);

        // Reject all other offers
        List<Offer> otherOffers = offerRepository.findByCaseIdAndStatus(offer.getCaseId(), OfferStatus.SUBMITTED);
        for (Offer otherOffer : otherOffers) {
            if (!otherOffer.getId().equals(offerId)) {
                otherOffer.setStatus(OfferStatus.REJECTED);
                otherOffer.setRejectedAt(LocalDateTime.now());
                offerRepository.save(otherOffer);
            }
        }

        // Update case
        caseEntity.setSelectedOfferId(offerId);
        caseEntity.setCaseStatus(CaseStatus.PAYMENT_PENDING);
        caseEntity.setLawyerId(offer.getLawyerId());
        caseRepository.save(caseEntity);
    }

    public List<OfferDTO> getOffersForCase(Long caseId, Long userId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (!caseEntity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        List<Offer> offers = offerRepository.findByCaseIdOrderByCreatedAtAsc(caseId);

        // Mark as viewed
        for (Offer offer : offers) {
            if (!Boolean.TRUE.equals(offer.getViewedByUser())) {
                offer.setViewedByUser(true);
                offerRepository.save(offer);
            }
        }

        return offers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<OfferDTO> getMyOffers(Long lawyerId) {
        List<Offer> offers = offerRepository.findByLawyerIdOrderByCreatedAtDesc(lawyerId);
        return offers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void withdrawOffer(Long offerId, Long lawyerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        if (!offer.getLawyerId().equals(lawyerId)) {
            throw new RuntimeException("Unauthorized: This offer does not belong to you");
        }

        if (offer.getStatus() != OfferStatus.SUBMITTED) {
            throw new RuntimeException("Only pending offers can be withdrawn");
        }

        Long caseId = offer.getCaseId();
        offer.setStatus(OfferStatus.WITHDRAWN);
        offerRepository.save(offer);

        // Force refresh of case status if this was the last offer
        caseRepository.findById(caseId).ifPresent(caseEntity -> {
            Long activeOffersCount = offerRepository.countByCaseIdAndStatus(caseId, OfferStatus.SUBMITTED);
            caseEntity.setOfferCount(activeOffersCount.intValue());
            
            // If no more active offers, revert from UNDER_REVIEW to PUBLISHED
            if (activeOffersCount == 0 && caseEntity.getCaseStatus() == CaseStatus.UNDER_REVIEW) {
                caseEntity.setCaseStatus(CaseStatus.PUBLISHED);
                
                // Broadcast that the case is available again
                try {
                    messagingTemplate.convertAndSend("/topic/lawyer/updates", Map.of(
                        "type", "CASE_UPDATED",
                        "caseId", caseId,
                        "status", "PUBLISHED"
                    ));
                } catch (Exception e) {
                    // Non-blocking error
                }
            }
            
            caseRepository.save(caseEntity);
        });
    }

    private OfferDTO convertToDTO(Offer offer) {
        OfferDTO dto = new OfferDTO();
        dto.setId(offer.getId());
        dto.setCaseId(offer.getCaseId());
        dto.setLawyerId(offer.getLawyerId());
        dto.setProposedFee(offer.getProposedFee());
        dto.setEstimatedTimeline(offer.getEstimatedTimeline());
        dto.setProposalMessage(offer.getProposalMessage());
        dto.setConsultationType(offer.getConsultationType());
        dto.setMilestonePlan(offer.getMilestonePlan());
        dto.setStatus(offer.getStatus());
        dto.setViewedByUser(Boolean.TRUE.equals(offer.getViewedByUser()));
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setExpiresAt(offer.getExpiresAt());
        dto.setAcceptedAt(offer.getAcceptedAt());

        // Fetch lawyer details
        lawyerRepository.findById(offer.getLawyerId()).ifPresent(lawyer -> {
            dto.setLawyerName(lawyer.getFullName());
            // Get first specialization as string
            if (lawyer.getSpecializations() != null && !lawyer.getSpecializations().isEmpty()) {
                dto.setLawyerSpecialization(lawyer.getSpecializations().iterator().next().toString());
            }
            dto.setLawyerExperience(lawyer.getYearsOfExperience());
            dto.setLawyerRating(lawyer.getRating());
        });

        return dto;
    }
}
