package com.recoverai.backend.dto;

public class WebhookIngestionResponse {

    private String eventId;
    private String eventType;
    private boolean processed;
    private String message;

    public WebhookIngestionResponse() {
    }

    public WebhookIngestionResponse(String eventId, String eventType, boolean processed, String message) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processed = processed;
        this.message = message;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
