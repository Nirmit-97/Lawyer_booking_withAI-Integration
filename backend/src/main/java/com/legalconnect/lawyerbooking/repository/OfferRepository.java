package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Offer;
import com.legalconnect.lawyerbooking.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    // Find all offers for a specific case, ordered by creation time
    List<Offer> findByCaseIdOrderByCreatedAtAsc(Long caseId);
    
    // Find all offers submitted by a specific lawyer
    List<Offer> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);
    
    // Find a specific offer by case and lawyer (enforces uniqueness)
    Optional<Offer> findByCaseIdAndLawyerId(Long caseId, Long lawyerId);
    
    // Find expired offers for scheduled cleanup
    List<Offer> findByStatusAndExpiresAtBefore(OfferStatus status, LocalDateTime now);
    
    // Count offers for a case with specific status
    Long countByCaseIdAndStatus(Long caseId, OfferStatus status);
    
    // Check if lawyer already submitted offer for a case
    boolean existsByCaseIdAndLawyerId(Long caseId, Long lawyerId);
    
    // Find all offers by status
    List<Offer> findByStatus(OfferStatus status);
    
    // Find offers for a case with specific status
    List<Offer> findByCaseIdAndStatus(Long caseId, OfferStatus status);
}
