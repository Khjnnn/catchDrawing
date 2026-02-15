package com.portfolio.monitor.scheduler;

import com.portfolio.monitor.dto.MetricsData;
import com.portfolio.monitor.service.MetricsService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricsPushScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final MetricsService metricsService;

    public MetricsPushScheduler(SimpMessagingTemplate messagingTemplate,
                                 MetricsService metricsService) {
        this.messagingTemplate = messagingTemplate;
        this.metricsService = metricsService;
    }

    @Scheduled(fixedRate = 1000)
    public void pushMetrics() {
        MetricsData metrics = metricsService.generateMetrics();
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }
}
