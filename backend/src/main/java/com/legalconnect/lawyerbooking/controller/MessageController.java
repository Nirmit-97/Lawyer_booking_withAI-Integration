package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.service.MessageService;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageRequest request) {
        try {
            // Service will verify access using SecurityContext
            authorizationService.verifyMessageAccess(request.getCaseId());
            
            // Override sender info from payload with actual authenticated user 
            // of course the service should ideally handle this, but for now we ensure 
            // the DTO passed to service matches the principal.
            // Actually, MessageService already takes MessageRequest. 
            // We'll update MessageService to also use SecurityContext.
            
            MessageDTO messageDTO = messageService.sendMessage(request);
            return ResponseEntity.ok(messageDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized message send attempt: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<MessageDTO>> getMessagesByCaseId(@PathVariable("caseId") Long caseId) {
        List<MessageDTO> messages = messageService.getMessagesByCaseId(caseId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/receiver/{receiverId}/{receiverType}")
    public ResponseEntity<List<MessageDTO>> getMessagesByReceiver(
            @PathVariable("receiverId") Long receiverId,
            @PathVariable("receiverType") String receiverType) {
        List<MessageDTO> messages = messageService.getMessagesByReceiver(receiverId, receiverType);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable("messageId") Long messageId) {
        try {
            messageService.markMessageAsRead(messageId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/unread-count/{receiverId}/{receiverType}")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount(
            @PathVariable("receiverId") Long receiverId,
            @PathVariable("receiverType") String receiverType) {
        long count = messageService.getUnreadMessageCount(receiverId, receiverType);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

