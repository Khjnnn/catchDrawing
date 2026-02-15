package com.portfolio.monitor.dto;

public class CursorEvent {

    private String senderId;
    private String nickname;
    private double x;
    private double y;
    private String color;

    public CursorEvent() {}

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
