package com.portfolio.monitor.dto;

public class DrawEvent {

    private String senderId;
    private String tool;
    private String color;
    private int lineWidth;
    private double fromX;
    private double fromY;
    private double toX;
    private double toY;
    private String action;

    public DrawEvent() {}

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getLineWidth() { return lineWidth; }
    public void setLineWidth(int lineWidth) { this.lineWidth = lineWidth; }
    public double getFromX() { return fromX; }
    public void setFromX(double fromX) { this.fromX = fromX; }
    public double getFromY() { return fromY; }
    public void setFromY(double fromY) { this.fromY = fromY; }
    public double getToX() { return toX; }
    public void setToX(double toX) { this.toX = toX; }
    public double getToY() { return toY; }
    public void setToY(double toY) { this.toY = toY; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
