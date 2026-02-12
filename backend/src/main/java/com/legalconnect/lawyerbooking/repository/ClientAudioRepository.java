package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import com.legalconnect.lawyerbooking.entity.ClientAudio;
import java.util.List;

public interface ClientAudioRepository extends JpaRepository<ClientAudio, Long> {
    
    @org.springframework.data.jpa.repository.Query("SELECT ca FROM ClientAudio ca")
    List<ClientAudio> findAllAudio();

    @org.springframework.data.jpa.repository.Query("SELECT ca FROM ClientAudio ca WHERE ca.userId = :userId")
    List<ClientAudio> findForUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT ca FROM ClientAudio ca JOIN ca.caseEntity c " +
           "WHERE c.deleted = false AND c.lawyerId IS NULL AND c.caseType IN :specializations")
    List<ClientAudio> findForLawyer(@Param("lawyerId") Long lawyerId, 
                                    @Param("specializations") java.util.Collection<com.legalconnect.lawyerbooking.enums.CaseType> specializations);

    @org.springframework.data.jpa.repository.Query("SELECT ca FROM ClientAudio ca WHERE ca.caseId = :caseId")
    List<ClientAudio> findByCaseId(@Param("caseId") Long caseId);

    void deleteByCaseId(Long caseId);
}

