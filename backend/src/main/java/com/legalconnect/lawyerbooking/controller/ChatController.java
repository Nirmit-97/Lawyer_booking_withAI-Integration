package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, java.security.Principal principal) {
        System.out.println("Received WebSocket message for case " + request.getCaseId() + 
                           " from sender " + request.getSenderId() + " (" + request.getSenderType() + ")");
        
        // Forward request directly to MessageService, which will now use IDs from payload
        // Also pass the principal for authentication verification
        messageService.sendMessage(request, principal);
    }
}
