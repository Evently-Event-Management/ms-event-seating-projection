package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import com.ticketly.mseventseatingprojection.service.EventTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventTrackingControllerTest {

    @Mock
    private EventTrackingService eventTrackingService;

    @Mock
    private ServerHttpRequest request;

    @InjectMocks
    private EventTrackingController controller;

    @Test
    public void testTrackEventViewWithMobileUserAgent() {
        // Setup
        String eventId = "event123";
        String mobileUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
        EventTrackingDocument document = new EventTrackingDocument();
        
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.USER_AGENT)).thenReturn(mobileUserAgent);
        
        when(eventTrackingService.incrementViewCount(eq(eventId), eq("mobile")))
            .thenReturn(Mono.just(document));

        // Execute
        StepVerifier.create(controller.trackEventView(eventId, request))
            .expectNextMatches(response -> response.getBody() == document)
            .verifyComplete();

        // Verify
        verify(eventTrackingService).incrementViewCount(eq(eventId), eq("mobile"));
    }

    @Test
    public void testTrackEventViewWithDesktopUserAgent() {
        // Setup
        String eventId = "event123";
        String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        EventTrackingDocument document = new EventTrackingDocument();
        
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.USER_AGENT)).thenReturn(desktopUserAgent);
        
        when(eventTrackingService.incrementViewCount(eq(eventId), eq("desktop")))
            .thenReturn(Mono.just(document));

        // Execute
        StepVerifier.create(controller.trackEventView(eventId, request))
            .expectNextMatches(response -> response.getBody() == document)
            .verifyComplete();

        // Verify
        verify(eventTrackingService).incrementViewCount(eq(eventId), eq("desktop"));
    }

    @Test
    public void testTrackEventViewWithTabletUserAgent() {
        // Setup
        String eventId = "event123";
        String tabletUserAgent = "Mozilla/5.0 (iPad; CPU OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
        EventTrackingDocument document = new EventTrackingDocument();
        
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.USER_AGENT)).thenReturn(tabletUserAgent);
        
        when(eventTrackingService.incrementViewCount(eq(eventId), eq("tablet")))
            .thenReturn(Mono.just(document));

        // Execute
        StepVerifier.create(controller.trackEventView(eventId, request))
            .expectNextMatches(response -> response.getBody() == document)
            .verifyComplete();

        // Verify
        verify(eventTrackingService).incrementViewCount(eq(eventId), eq("tablet"));
    }

    @Test
    public void testTrackEventViewWithUnknownUserAgent() {
        // Setup
        String eventId = "event123";
        String unknownUserAgent = "Some unknown user agent string";
        EventTrackingDocument document = new EventTrackingDocument();
        
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.USER_AGENT)).thenReturn(unknownUserAgent);
        
        when(eventTrackingService.incrementViewCount(eq(eventId), eq("other")))
            .thenReturn(Mono.just(document));

        // Execute
        StepVerifier.create(controller.trackEventView(eventId, request))
            .expectNextMatches(response -> response.getBody() == document)
            .verifyComplete();

        // Verify
        verify(eventTrackingService).incrementViewCount(eq(eventId), eq("other"));
    }

    @Test
    public void testTrackEventViewWithNullUserAgent() {
        // Setup
        String eventId = "event123";
        EventTrackingDocument document = new EventTrackingDocument();
        
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.USER_AGENT)).thenReturn(null);
        
        when(eventTrackingService.incrementViewCount(eq(eventId), eq("other")))
            .thenReturn(Mono.just(document));

        // Execute
        StepVerifier.create(controller.trackEventView(eventId, request))
            .expectNextMatches(response -> response.getBody() == document)
            .verifyComplete();

        // Verify
        verify(eventTrackingService).incrementViewCount(eq(eventId), eq("other"));
    }
}