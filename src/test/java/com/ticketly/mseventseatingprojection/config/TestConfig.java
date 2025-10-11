package com.ticketly.mseventseatingprojection.config;

import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsDTO;
import com.ticketly.mseventseatingprojection.service.GoogleAnalyticsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public GoogleAnalyticsService mockGoogleAnalyticsService() {
        return new GoogleAnalyticsService() {
            @Override
            public Mono<Integer> getEventTotalViews(String eventId) {
                // Return some mock data for testing
                return Mono.just(100);
            }

            @Override
            public Mono<EventViewsDTO> getEventViewsAnalytics(String eventId) {
                // Return some mock data for testing
                EventViewsDTO mockData = new EventViewsDTO();
                mockData.setTotalViews(100);
                
                // Add mock time series data
                List<EventViewsDTO.TimeSeriesData> timeSeriesData = new ArrayList<>();
                timeSeriesData.add(new EventViewsDTO.TimeSeriesData("2023-01-01", 10));
                timeSeriesData.add(new EventViewsDTO.TimeSeriesData("2023-01-02", 20));
                timeSeriesData.add(new EventViewsDTO.TimeSeriesData("2023-01-03", 30));
                timeSeriesData.add(new EventViewsDTO.TimeSeriesData("2023-01-04", 40));
                mockData.setViewsTimeSeries(timeSeriesData);
                
                return Mono.just(mockData);
            }
        };
    }
}