package com.ticketly.mseventseatingprojection.service.impl;

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.analytics.data.v1beta.BatchRunReportsRequest;
import com.google.analytics.data.v1beta.BatchRunReportsResponse;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Filter;
import com.google.analytics.data.v1beta.FilterExpression;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.OrderBy;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
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

    private BetaAnalyticsDataClient getAnalyticsClient() throws IOException {
        // Replace escaped newlines with actual newlines
        String formattedPrivateKey = privateKey.replace("\\n", "\n");

        // Create credentials from the environment variables
        GoogleCredentials credentials = ServiceAccountCredentials.fromPkcs8(
                clientEmail,
                formattedPrivateKey,
                null,
                null,
                null);

        // Build the analytics client
        return BetaAnalyticsDataClient.create(
                BetaAnalyticsDataSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build()
        );
    }

    @Override
    public Mono<Integer> getEventTotalViews(String eventId) {
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

                if (response.getRowsCount() > 0 && !response.getRows(0).getMetricValues(0).getValue().isEmpty()) {
                    return Integer.parseInt(response.getRows(0).getMetricValues(0).getValue());
                }
                return 0;

            } catch (Exception e) {
                log.error("Error fetching GA total views for eventId={}: {}", eventId, e.getMessage(), e);
                return 0;
            }
        });
    }

    @Override
    public Mono<EventViewsDTO> getEventViewsAnalytics(String eventId) {
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

                // Make the batch request
                BatchRunReportsRequest batchRequest = BatchRunReportsRequest.newBuilder()
                        .setProperty(formattedPropertyId)
                        .addAllRequests(requests)
                        .build();

                BatchRunReportsResponse batchResponse = analyticsClient.batchRunReports(batchRequest);

                // Parse the response
                EventViewsDTO result = new EventViewsDTO();

                // 0: Total Views
                RunReportResponse totalViewsResponse = batchResponse.getReports(0);
                result.setTotalViews(totalViewsResponse.getRowsCount() > 0
                        ? Integer.parseInt(totalViewsResponse.getRows(0).getMetricValues(0).getValue())
                        : 0);

                // 1: Views Time Series
                RunReportResponse timeSeriesResponse = batchResponse.getReports(1);
                List<EventViewsDTO.TimeSeriesData> timeSeries = new ArrayList<>();
                for (int i = 0; i < timeSeriesResponse.getRowsCount(); i++) {
                    String date = timeSeriesResponse.getRows(i).getDimensionValues(0).getValue();
                    int views = Integer.parseInt(timeSeriesResponse.getRows(i).getMetricValues(0).getValue());
                    timeSeries.add(new EventViewsDTO.TimeSeriesData(date, views));
                }
                result.setViewsTimeSeries(timeSeries);

                // 2: Traffic Sources
                RunReportResponse trafficSourcesResponse = batchResponse.getReports(2);
                List<EventViewsDTO.TrafficSource> trafficSources = new ArrayList<>();
                for (int i = 0; i < trafficSourcesResponse.getRowsCount(); i++) {
                    String source = trafficSourcesResponse.getRows(i).getDimensionValues(0).getValue();
                    String medium = trafficSourcesResponse.getRows(i).getDimensionValues(1).getValue();
                    int views = Integer.parseInt(trafficSourcesResponse.getRows(i).getMetricValues(0).getValue());
                    trafficSources.add(new EventViewsDTO.TrafficSource(source, medium, views));
                }
                result.setTrafficSources(trafficSources);

                // 3: Audience Geography
                RunReportResponse audienceGeoResponse = batchResponse.getReports(3);
                List<EventViewsDTO.AudienceGeo> audienceGeo = new ArrayList<>();
                for (int i = 0; i < audienceGeoResponse.getRowsCount(); i++) {
                    String location = audienceGeoResponse.getRows(i).getDimensionValues(0).getValue();
                    int views = Integer.parseInt(audienceGeoResponse.getRows(i).getMetricValues(0).getValue());
                    audienceGeo.add(new EventViewsDTO.AudienceGeo(location, views));
                }
                result.setAudienceGeography(audienceGeo);

                // 4: Device Breakdown
                RunReportResponse deviceBreakdownResponse = batchResponse.getReports(4);
                List<EventViewsDTO.DeviceBreakdown> deviceBreakdown = new ArrayList<>();
                for (int i = 0; i < deviceBreakdownResponse.getRowsCount(); i++) {
                    String device = deviceBreakdownResponse.getRows(i).getDimensionValues(0).getValue();
                    int views = Integer.parseInt(deviceBreakdownResponse.getRows(i).getMetricValues(0).getValue());
                    deviceBreakdown.add(new EventViewsDTO.DeviceBreakdown(device, views));
                }
                result.setDeviceBreakdown(deviceBreakdown);

                return result;
            } catch (Exception e) {
                log.error("Error fetching batched GA data for eventId={}: {}", eventId, e.getMessage(), e);
                // Return empty results when an error occurs
                return new EventViewsDTO();
            }
        });
    }
}