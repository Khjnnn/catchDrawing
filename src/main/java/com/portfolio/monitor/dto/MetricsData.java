package com.portfolio.monitor.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MetricsData {

    private double tps;
    private double responseTime;
    private int activeUsers;
    private long totalRequests;
    private double cpuUsage;
    private double memoryUsage;
    private List<ServiceStatus> services;
    private String timestamp;

    public static class ServiceStatus {
        private String name;
        private boolean up;
        private double responseTime;

        public ServiceStatus() {}

        public ServiceStatus(String name, boolean up, double responseTime) {
            this.name = name;
            this.up = up;
            this.responseTime = responseTime;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isUp() { return up; }
        public void setUp(boolean up) { this.up = up; }
        public double getResponseTime() { return responseTime; }
        public void setResponseTime(double responseTime) { this.responseTime = responseTime; }
    }

    public MetricsData() {}

    public double getTps() { return tps; }
    public void setTps(double tps) { this.tps = tps; }
    public double getResponseTime() { return responseTime; }
    public void setResponseTime(double responseTime) { this.responseTime = responseTime; }
    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    public List<ServiceStatus> getServices() { return services; }
    public void setServices(List<ServiceStatus> services) { this.services = services; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
