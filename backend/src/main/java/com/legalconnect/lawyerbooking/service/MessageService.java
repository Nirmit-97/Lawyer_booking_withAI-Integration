package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.entity.Message;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.repository.MessageRepository;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private com.legalconnect.lawyerbooking.repository.UserRepository userRepository;

    @Autowired
    private com.legalconnect.lawyerbooking.repository.LawyerRepository lawyerRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public MessageDTO sendMessage(MessageRequest request) {
        // 1. Validate message text
        if (request.getMessageText() == null || request.getMessageText().trim().isEmpty()) {
            throw new BadRequestException("Message text cannot be empty");
        }

        // 2. Validate sender existence (Requirements: "Validating sender existence via UserRepository")
        Long senderId = request.getSenderId();
        String senderType = request.getSenderType();
        
        if (senderId == null || senderType == null) {
            throw new BadRequestException("Sender ID and Type must be provided");
        }

        boolean senderExists = false;
        if ("user".equalsIgnoreCase(senderType)) {
            senderExists = userRepository.existsById(senderId);
        } else if ("lawyer".equalsIgnoreCase(senderType)) {
            senderExists = lawyerRepository.existsById(senderId);
        }

        if (!senderExists) {
            throw new com.legalconnect.lawyerbooking.exception.UnauthorizedException("Sender not found: " + senderType + " ID " + senderId);
        }

        // 3. Strict Access Check (Phase 18)
        authorizationService.verifyMessageAccess(request.getCaseId());

        logger.info("Sending WebSocket message from {} {} to {} {} for case {}", 
                   senderType, senderId,
                   request.getReceiverType(), request.getReceiverId(),
                   request.getCaseId());

        Message message = new Message();
        message.setCaseId(request.getCaseId());
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setReceiverId(request.getReceiverId());
        message.setReceiverType(request.getReceiverType());
        message.setMessageText(request.getMessageText().trim());
        message.setIsRead(false);
        
        Message saved = messageRepository.save(message);
        MessageDTO dto = convertToDTO(saved);
        
        // Broadcast the message to the case topic
        messagingTemplate.convertAndSend("/topic/case/" + request.getCaseId(), dto);
        
        return dto;
    }

    public List<MessageDTO> getMessagesByCaseId(Long caseId) {
        // Enforce Phase 18 Access Gating
        authorizationService.verifyMessageAccess(caseId);
        
        List<Message> messages = messageRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageDTO> getMessagesByReceiver(Long receiverId, String receiverType) {
        List<Message> messages = messageRepository.findByReceiverIdAndReceiverType(receiverId, receiverType);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void markMessageAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    public long getUnreadMessageCount(Long receiverId, String receiverType) {
        return messageRepository.countByReceiverIdAndReceiverTypeAndIsRead(receiverId, receiverType, false);
    }

    // Removed resolvePrincipal as we now pass senderId in payload

    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getCaseId(),
            message.getSenderId(),
            message.getSenderType(),
            message.getReceiverId(),
            message.getReceiverType(),
            message.getMessageText(),
            message.getIsRead(),
            message.getCreatedAt()
        );
    }
}

