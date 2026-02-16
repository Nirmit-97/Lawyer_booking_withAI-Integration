package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.entity.Review;
import com.legalconnect.lawyerbooking.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> submitReview(@RequestBody Review review) {
        try {
            return ResponseEntity.ok(reviewService.saveReview(review));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/lawyer/{lawyerId}")
    public ResponseEntity<List<Review>> getLawyerReviews(@PathVariable("lawyerId") Long lawyerId) {
        return ResponseEntity.ok(reviewService.getReviewsForLawyer(lawyerId));
    }
}
