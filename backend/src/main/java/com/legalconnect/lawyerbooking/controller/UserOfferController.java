package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.OfferDTO;
import com.legalconnect.lawyerbooking.security.UserPrincipal;
import com.legalconnect.lawyerbooking.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/cases")
public class UserOfferController {

    private final OfferService offerService;

    public UserOfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/{caseId}/offers")
    public ResponseEntity<List<OfferDTO>> getOffersForCase(
            @PathVariable Long caseId,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        List<OfferDTO> offers = offerService.getOffersForCase(caseId, userId);
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/{caseId}/offers/{offerId}/accept")
    public ResponseEntity<String> acceptOffer(
            @PathVariable Long caseId,
            @PathVariable Long offerId,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        offerService.acceptOffer(offerId, userId);
        return ResponseEntity.ok("Offer accepted successfully. Please proceed to payment.");
    }
}
