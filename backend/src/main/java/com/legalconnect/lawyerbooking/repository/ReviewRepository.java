package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLawyerId(Long lawyerId);
    List<Review> findByCaseId(Long caseId);
}
