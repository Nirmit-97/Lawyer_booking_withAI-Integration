package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.entity.Review;
import com.legalconnect.lawyerbooking.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private LawyerService lawyerService;

    @Transactional
    public Review saveReview(Review review) {
        Review saved = reviewRepository.save(review);
        updateLawyerAverageRating(review.getLawyerId());
        return saved;
    }

    public List<Review> getReviewsForLawyer(Long lawyerId) {
        return reviewRepository.findByLawyerId(lawyerId);
    }

    private void updateLawyerAverageRating(Long lawyerId) {
        List<Review> reviews = reviewRepository.findByLawyerId(lawyerId);
        if (reviews.isEmpty()) {
            lawyerService.updateLawyerRating(lawyerId, 0.0);
            return;
        }

        double sum = reviews.stream().mapToInt(Review::getRating).sum();
        double average = sum / reviews.size();
        
        // Round to 1 decimal place
        average = Math.round(average * 10.0) / 10.0;
        
        lawyerService.updateLawyerRating(lawyerId, average);
    }
}
