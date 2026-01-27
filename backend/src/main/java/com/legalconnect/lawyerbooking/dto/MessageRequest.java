package com.legalconnect.lawyerbooking.dto;

public class MessageRequest {
    private Long senderId;
    private Long receiverId;
    private String messageText; // Renamed from content
    private Long caseId;
    private String senderType;
    private String receiverType; // Added field

    // Getters and Setters
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String receiverType) { this.receiverType = receiverType; }
}
