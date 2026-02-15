package com.portfolio.monitor.dto;

public class EventLog {

    private String id;
    private String type;
    private String message;
    private String level;
    private String timestamp;

    public EventLog() {}

    public EventLog(String id, String type, String message, String level, String timestamp) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
