package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.CreateOfferRequest;
import com.legalconnect.lawyerbooking.dto.OfferDTO;
import com.legalconnect.lawyerbooking.security.UserPrincipal;
import com.legalconnect.lawyerbooking.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lawyer/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/cases/{caseId}")
    public ResponseEntity<OfferDTO> submitOffer(
            @PathVariable("caseId") Long caseId,
            @Valid @RequestBody CreateOfferRequest request,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long lawyerId = userPrincipal.getUserId();
        OfferDTO offer = offerService.submitOffer(caseId, request, lawyerId);
        return ResponseEntity.ok(offer);
    }

    @GetMapping("/my-offers")
    public ResponseEntity<List<OfferDTO>> getMyOffers(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long lawyerId = userPrincipal.getUserId();
        List<OfferDTO> offers = offerService.getMyOffers(lawyerId);
        return ResponseEntity.ok(offers);
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> withdrawOffer(
            @PathVariable("offerId") Long offerId,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long lawyerId = userPrincipal.getUserId();
        offerService.withdrawOffer(offerId, lawyerId);
        return ResponseEntity.ok().build();
    }
}
