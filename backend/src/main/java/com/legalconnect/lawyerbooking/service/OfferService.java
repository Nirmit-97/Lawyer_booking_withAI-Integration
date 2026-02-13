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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final CaseRepository caseRepository;
    private final LawyerRepository lawyerRepository;
    private final UserRepository userRepository;

    @Value("${payment.max.offers.per.case:5}")
    private int maxOffersPerCase;

    public OfferService(OfferRepository offerRepository, 
                        CaseRepository caseRepository, 
                        LawyerRepository lawyerRepository,
                        UserRepository userRepository) {
        this.offerRepository = offerRepository;
        this.caseRepository = caseRepository;
        this.lawyerRepository = lawyerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OfferDTO submitOffer(Long caseId, CreateOfferRequest request, Long lawyerId) {
        // Validate case exists and is in correct state
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (caseEntity.getCaseStatus() != CaseStatus.PUBLISHED && 
            caseEntity.getCaseStatus() != CaseStatus.UNDER_REVIEW) {
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
        if (offerRepository.existsByCaseIdAndLawyerId(caseId, lawyerId)) {
            throw new RuntimeException("You have already submitted an offer for this case");
        }

        // Check offer limit
        Long offerCount = offerRepository.countByCaseIdAndStatus(caseId, OfferStatus.SUBMITTED);
        if (offerCount >= maxOffersPerCase) {
            throw new RuntimeException("Maximum offers limit reached for this case");
        }

        // Create offer
        Offer offer = new Offer();
        offer.setCaseId(caseId);
        offer.setLawyerId(lawyerId);
        offer.setProposedFee(request.getProposedFee());
        offer.setEstimatedTimeline(request.getEstimatedTimeline());
        offer.setProposalMessage(request.getProposalMessage());
        offer.setConsultationType(request.getConsultationType());
        offer.setMilestonePlan(request.getMilestonePlan());
        offer.setStatus(OfferStatus.SUBMITTED);

        Offer savedOffer = offerRepository.save(offer);

        // Update case status and offer count
        if (caseEntity.getCaseStatus() == CaseStatus.PUBLISHED) {
            caseEntity.setCaseStatus(CaseStatus.UNDER_REVIEW);
        }
        caseEntity.setOfferCount(caseEntity.getOfferCount() + 1);
        caseRepository.save(caseEntity);

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
        if (caseEntity.getCaseStatus() != CaseStatus.UNDER_REVIEW) {
            throw new RuntimeException("Case is not in UNDER_REVIEW status");
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
            if (!offer.isViewedByUser()) {
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

        offer.setStatus(OfferStatus.WITHDRAWN);
        offerRepository.save(offer);

        // Decrement case offer count
        caseRepository.findById(offer.getCaseId()).ifPresent(caseEntity -> {
            caseEntity.setOfferCount(Math.max(0, caseEntity.getOfferCount() - 1));
            // If it was UNDER_REVIEW and no more offers, maybe move back to PUBLISHED?
            // For now, keep it simple.
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
        dto.setViewedByUser(offer.isViewedByUser());
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
