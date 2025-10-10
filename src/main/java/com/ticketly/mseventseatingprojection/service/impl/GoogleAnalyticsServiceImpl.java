package com.ticketly.mseventseatingprojection.service.impl;

import com.google.analytics.data.v1beta.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsDTO;
import com.ticketly.mseventseatingprojection.service.GoogleAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GoogleAnalyticsServiceImpl implements GoogleAnalyticsService {

    @Value("${google.analytics.property-id}")
    private String propertyId;

    @Value("${google.analytics.client-email}")
    private String clientEmail;

    @Value("${google.analytics.private-key}")
    private String privateKey;

    // 1. Add fields for the new properties
    @Value("${google.analytics.client-id}")
    private String clientId;

    @Value("${google.analytics.private-key-id}")
    private String privateKeyId;

    private BetaAnalyticsDataClient getAnalyticsClient() throws IOException {
        // 2. Validate all required configuration properties
        if (clientId == null || clientEmail == null || privateKeyId == null || privateKey == null || propertyId == null) {
            throw new IllegalStateException("Missing required Google Analytics configuration. " +
                    "Ensure client-id, client-email, private-key-id, private-key, and property-id are set.");
        }

        // Replace escaped newlines with actual newlines for the private key
        String formattedPrivateKey = privateKey.replace("\\n", "\n");

        try {
            // 3. Define the required scope for the Google Analytics Data API
            List<String> scopes = List.of("https://www.googleapis.com/auth/analytics.readonly");

            // 4. Use the ServiceAccountCredentials Builder for a safer and clearer construction
            GoogleCredentials credentials = ServiceAccountCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientEmail(clientEmail)
                    .setPrivateKeyId(privateKeyId)
                    .setPrivateKeyString(formattedPrivateKey)
                    .setScopes(scopes)
                    .build();

            // Build the analytics client with the credentials
            return BetaAnalyticsDataClient.create(
                    BetaAnalyticsDataSettings.newBuilder()
                            .setCredentialsProvider(() -> credentials)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to initialize Google Analytics client: {}", e.getMessage(), e);
            throw new IOException("Failed to initialize Google Analytics client", e);
        }
    }

    // The rest of your class (getEventTotalViews, getEventViewsAnalytics) can remain the same.
    // They will now work correctly because getAnalyticsClient() is fixed.

    @Override
    public Mono<Integer> getEventTotalViews(String eventId) {
        // This method does not need to be changed
        return Mono.fromCallable(() -> {
            try {
                BetaAnalyticsDataClient analyticsClient = getAnalyticsClient();
                RunReportRequest request = RunReportRequest.newBuilder()
                        .setProperty("properties/" + propertyId)
                        .addDateRanges(DateRange.newBuilder()
                                .setStartDate("30daysAgo")
                                .setEndDate("today")
                                .build())
                        .addDimensions(Dimension.newBuilder()
                                .setName("customEvent:event_id")
                                .build())
                        .addMetrics(Metric.newBuilder()
                                .setName("eventCount")
                                .build())
                        .setDimensionFilter(FilterExpression.newBuilder()
                                .setFilter(Filter.newBuilder()
                                        .setFieldName("customEvent:event_id")
                                        .setStringFilter(Filter.StringFilter.newBuilder()
                                                .setValue(eventId)
                                                .setMatchType(Filter.StringFilter.MatchType.EXACT)
                                                .build())
                                        .build())
                                .build())
                        .build();

                RunReportResponse response = analyticsClient.runReport(request);

                if (response.getRowsCount() > 0 &&
                        response.getRows(0).getMetricValuesCount() > 0) {
                    response.getRows(0).getMetricValues(0).getValue();
                    if (!response.getRows(0).getMetricValues(0).getValue().isEmpty()) {
                        return Integer.parseInt(response.getRows(0).getMetricValues(0).getValue());
                    }
                }
                return 0;

            } catch (Exception e) {
                log.error("Error fetching GA total views for eventId={}: {}", eventId, e.getMessage(), e);
                // The original exception was being swallowed, now it's correctly propagated by getAnalyticsClient
                throw new RuntimeException("Failed to fetch GA data", e);
            }
        });
    }

    @Override
    public Mono<EventViewsDTO> getEventViewsAnalytics(String eventId) {
        // This method also does not need to be changed
        return Mono.fromCallable(() -> {
            try {
                BetaAnalyticsDataClient analyticsClient = getAnalyticsClient();
                String formattedPropertyId = "properties/" + propertyId;
                DateRange dateRange = DateRange.newBuilder()
                        .setStartDate("30daysAgo")
                        .setEndDate("today")
                        .build();

                FilterExpression eventIdFilter = FilterExpression.newBuilder()
                        .setFilter(Filter.newBuilder()
                                .setFieldName("customEvent:event_id")
                                .setStringFilter(Filter.StringFilter.newBuilder()
                                        .setValue(eventId)
                                        .setMatchType(Filter.StringFilter.MatchType.EXACT)
                                        .build())
                                .build())
                        .build();

                List<RunReportRequest> requests = new ArrayList<>();

                // 0: Total Views
                requests.add(RunReportRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addDateRanges(dateRange)
                        .addDimensions(Dimension.newBuilder().setName("customEvent:event_id").build())
                        .addMetrics(Metric.newBuilder().setName("eventCount").build())
                        .setDimensionFilter(eventIdFilter)
                        .build());

                // 1: Views Time Series
                requests.add(RunReportRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addDateRanges(dateRange)
                        .addDimensions(Dimension.newBuilder().setName("date").build())
                        .addMetrics(Metric.newBuilder().setName("eventCount").build())
                        .addOrderBys(OrderBy.newBuilder()
                                .setDimension(OrderBy.DimensionOrderBy.newBuilder()
                                        .setDimensionName("date")
                                        .setOrderType(OrderBy.DimensionOrderBy.OrderType.ALPHANUMERIC)
                                        .build())
                                .build())
                        .setDimensionFilter(eventIdFilter)
                        .build());

                // 2: Traffic Sources
                requests.add(RunReportRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addDateRanges(dateRange)
                        .addDimensions(Dimension.newBuilder().setName("sessionSource").build())
                        .addDimensions(Dimension.newBuilder().setName("sessionMedium").build())
                        .addMetrics(Metric.newBuilder().setName("eventCount").build())
                        .addOrderBys(OrderBy.newBuilder()
                                .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                        .setMetricName("eventCount")
                                        .build())
                                .setDesc(true)
                                .build())
                        .setLimit(5)
                        .setDimensionFilter(eventIdFilter)
                        .build());

                // 3: Audience Geography
                requests.add(RunReportRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addDateRanges(dateRange)
                        .addDimensions(Dimension.newBuilder().setName("city").build())
                        .addMetrics(Metric.newBuilder().setName("eventCount").build())
                        .addOrderBys(OrderBy.newBuilder()
                                .setMetric(OrderBy.MetricOrderBy.newBuilder()
                                        .setMetricName("eventCount")
                                        .build())
                                .setDesc(true)
                                .build())
                        .setLimit(5)
                        .setDimensionFilter(eventIdFilter)
                        .build());

                // 4: Device Breakdown
                requests.add(RunReportRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addDateRanges(dateRange)
                        .addDimensions(Dimension.newBuilder().setName("deviceCategory").build())
                        .addMetrics(Metric.newBuilder().setName("eventCount").build())
                        .setDimensionFilter(eventIdFilter)
                        .build());

                BatchRunReportsRequest batchRequest = BatchRunReportsRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addAllRequests(requests)
                        .build();

                BatchRunReportsResponse batchResponse = analyticsClient.batchRunReports(batchRequest);
                EventViewsDTO result = new EventViewsDTO();

                if (batchResponse.getReportsCount() < 5) {
                    log.warn("Google Analytics batch response contains fewer reports than expected: {} vs 5",
                            batchResponse.getReportsCount());
                    return result;
                }

                // Parse Total Views
                RunReportResponse totalViewsResponse = batchResponse.getReports(0);
                result.setTotalViews(totalViewsResponse.getRowsCount() > 0 && totalViewsResponse.getRows(0).getMetricValuesCount() > 0
                        ? Integer.parseInt(totalViewsResponse.getRows(0).getMetricValues(0).getValue())
                        : 0);

                // Parse Time Series
                RunReportResponse timeSeriesResponse = batchResponse.getReports(1);
                List<EventViewsDTO.TimeSeriesData> timeSeries = new ArrayList<>();
                timeSeriesResponse.getRowsList().forEach(row -> {
                    String date = row.getDimensionValuesCount() > 0 ? row.getDimensionValues(0).getValue() : "Unknown";
                    int views = row.getMetricValuesCount() > 0 ? Integer.parseInt(row.getMetricValues(0).getValue()) : 0;
                    timeSeries.add(new EventViewsDTO.TimeSeriesData(date, views));
                });
                result.setViewsTimeSeries(timeSeries);

                // Parse Traffic Sources
                RunReportResponse trafficSourcesResponse = batchResponse.getReports(2);
                List<EventViewsDTO.TrafficSource> trafficSources = new ArrayList<>();
                trafficSourcesResponse.getRowsList().forEach(row -> {
                    String source = row.getDimensionValuesCount() > 0 ? row.getDimensionValues(0).getValue() : "Unknown";
                    String medium = row.getDimensionValuesCount() > 1 ? row.getDimensionValues(1).getValue() : "Unknown";
                    int views = row.getMetricValuesCount() > 0 ? Integer.parseInt(row.getMetricValues(0).getValue()) : 0;
                    trafficSources.add(new EventViewsDTO.TrafficSource(source, medium, views));
                });
                result.setTrafficSources(trafficSources);

                // Parse Audience Geo
                RunReportResponse audienceGeoResponse = batchResponse.getReports(3);
                List<EventViewsDTO.AudienceGeo> audienceGeo = new ArrayList<>();
                audienceGeoResponse.getRowsList().forEach(row -> {
                    String location = row.getDimensionValuesCount() > 0 ? row.getDimensionValues(0).getValue() : "Unknown";
                    int views = row.getMetricValuesCount() > 0 ? Integer.parseInt(row.getMetricValues(0).getValue()) : 0;
                    audienceGeo.add(new EventViewsDTO.AudienceGeo(location, views));
                });
                result.setAudienceGeography(audienceGeo);

                // Parse Device Breakdown
                RunReportResponse deviceBreakdownResponse = batchResponse.getReports(4);
                List<EventViewsDTO.DeviceBreakdown> deviceBreakdown = new ArrayList<>();
                deviceBreakdownResponse.getRowsList().forEach(row -> {
                    String device = row.getDimensionValuesCount() > 0 ? row.getDimensionValues(0).getValue() : "Unknown";
                    int views = row.getMetricValuesCount() > 0 ? Integer.parseInt(row.getMetricValues(0).getValue()) : 0;
                    deviceBreakdown.add(new EventViewsDTO.DeviceBreakdown(device, views));
                });
                result.setDeviceBreakdown(deviceBreakdown);

                return result;
            } catch (Exception e) {
                log.error("Error fetching batched GA data for eventId={}: {}", eventId, e.getMessage(), e);
                throw new RuntimeException("Failed to fetch batched GA data", e);
            }
        });
    }
}