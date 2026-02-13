package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    
    @Query("SELECT c FROM Case c WHERE c.userId = :userId AND c.deleted = false")
    List<Case> findByUserIdAndDeletedFalse(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Case c WHERE c.lawyerId = :lawyerId AND c.deleted = false")
    List<Case> findByLawyerIdAndDeletedFalse(@Param("lawyerId") Long lawyerId);
    
    @Query("SELECT c FROM Case c WHERE c.deleted = false AND " +
           "(c.lawyerId = :lawyerId OR (c.lawyerId IS NULL AND c.caseType IN :specializations))")
    List<Case> findForLawyer(@Param("lawyerId") Long lawyerId, 
                             @Param("specializations") Collection<CaseType> specializations);

    @Query("SELECT c FROM Case c WHERE c.lawyerId IS NULL AND c.deleted = false")
    List<Case> findUnassigned(); 

    @Query("SELECT c FROM Case c WHERE c.lawyerId IS NULL AND c.deleted = false AND c.caseType IN :types")
    List<Case> findUnassignedBySpecializations(@Param("types") Collection<CaseType> types);

    @Query("SELECT c FROM Case c WHERE c.deleted = false")
    List<Case> findAllByDeletedFalse();

    long countByDeletedFalse();
    
    long countByCaseStatus(CaseStatus caseStatus);
    
    long countByCaseType(CaseType caseType);

    long countByCaseTypeAndDeletedFalse(CaseType caseType);
    
    long countByCaseStatusAndDeletedFalse(CaseStatus caseStatus);
}
