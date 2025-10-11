package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ticketly.mseventseatingprojection.dto.CategoryChangePayload;
import com.ticketly.mseventseatingprojection.dto.CoverPhotoChangePayload;
import com.ticketly.mseventseatingprojection.dto.EventChangePayload;
import com.ticketly.mseventseatingprojection.dto.OrganizationChangePayload;
import com.ticketly.mseventseatingprojection.dto.SeatingMapChangePayload;
import com.ticketly.mseventseatingprojection.dto.SessionChangePayload;
import com.ticketly.mseventseatingprojection.dto.read.SessionStatusInfo;
import model.SessionStatus;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustom;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebeziumEventConsumerTest {

    @Mock
    private ProjectorService projectorService;

    @Mock
    private EventRepository eventReadRepository;

    @Mock
    private EventReadRepositoryCustom eventReadRepositoryCustom;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DebeziumEventConsumer debeziumEventConsumer;

    private ObjectNode createPayloadNode(String operation) {
        ObjectMapper realObjectMapper = new ObjectMapper();
        ObjectNode payloadNode = realObjectMapper.createObjectNode();
        payloadNode.put("op", operation);
        
        ObjectNode afterNode = realObjectMapper.createObjectNode();
        afterNode.put("id", UUID.randomUUID().toString());
        payloadNode.set("after", afterNode);
        
        ObjectNode beforeNode = realObjectMapper.createObjectNode();
        beforeNode.put("id", UUID.randomUUID().toString());
        payloadNode.set("before", beforeNode);
        
        return payloadNode;
    }

    @Test
    void onDebeziumEvent_withTombstoneRecord_shouldAcknowledgeImmediately() {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.events", 0, 0, "key", null);

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void processEventChange_withInsertOperation_shouldProjectEvent() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.events", 0, 0, "key", "{\"payload\":{\"op\":\"c\",\"after\":{\"id\":\"123\",\"status\":\"APPROVED\"}}}");

        ObjectNode payloadNode = createPayloadNode("c");
        ObjectNode afterNode = (ObjectNode) payloadNode.get("after");
        afterNode.put("status", "APPROVED");
        JsonNode jsonNode = payloadNode;

        EventChangePayload eventChange = new EventChangePayload();
        eventChange.setId(UUID.randomUUID());
        eventChange.setStatus("APPROVED");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(EventChangePayload.class))).thenReturn(eventChange);
        when(projectorService.projectFullEvent(any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(EventChangePayload.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processOrganizationChange_shouldProjectOrganization() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.organizations", 0, 0, "key", 
            "{\"payload\":{\"op\":\"c\",\"after\":{\"id\":\"123\",\"name\":\"Test Org\"}}}");

        ObjectNode payloadNode = createPayloadNode("c");
        ObjectNode orgAfterNode = (ObjectNode) payloadNode.get("after");
        orgAfterNode.put("name", "Test Org");
        JsonNode jsonNode = payloadNode;

        OrganizationChangePayload orgChange = new OrganizationChangePayload();
        orgChange.setId(UUID.randomUUID());
        orgChange.setName("Test Org");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(OrganizationChangePayload.class))).thenReturn(orgChange);
        when(projectorService.projectOrganizationChange(any(OrganizationChangePayload.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(OrganizationChangePayload.class));
        verify(projectorService).projectOrganizationChange(any(OrganizationChangePayload.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processCategoryChange_shouldProjectCategory() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.categories", 0, 0, "key", 
            "{\"payload\":{\"op\":\"c\",\"after\":{\"id\":\"123\",\"name\":\"Test Category\"}}}");

        ObjectNode payloadNode = createPayloadNode("c");
        ObjectNode catAfterNode = (ObjectNode) payloadNode.get("after");
        catAfterNode.put("name", "Test Category");
        JsonNode jsonNode = payloadNode;

        CategoryChangePayload categoryChange = new CategoryChangePayload();
        categoryChange.setId(UUID.randomUUID());
        categoryChange.setName("Test Category");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(CategoryChangePayload.class))).thenReturn(categoryChange);
        when(projectorService.projectCategoryChange(any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(CategoryChangePayload.class));
        verify(projectorService).projectCategoryChange(any(UUID.class));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processCategoryChange_withDelete_shouldDeleteCategory() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.categories", 0, 0, "key", 
            "{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"00000000-0000-0000-0000-000000000123\",\"name\":\"Test Category\"}}}");

        JsonNode jsonNode = new ObjectMapper().readTree(
            "{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"00000000-0000-0000-0000-000000000123\",\"name\":\"Test Category\"}}}");
        
        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(projectorService.deleteCategory(eq("00000000-0000-0000-0000-000000000123"))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(projectorService).deleteCategory(eq("00000000-0000-0000-0000-000000000123"));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processEventChange_withDelete_shouldDeleteEvent() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.events", 0, 0, "key", 
            "{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"00000000-0000-0000-0000-000000000123\"}}}");

        JsonNode jsonNode = new ObjectMapper().readTree(
            "{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"00000000-0000-0000-0000-000000000123\"}}}");
        
        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(projectorService.deleteEvent(eq(UUID.fromString("00000000-0000-0000-0000-000000000123")))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(projectorService).deleteEvent(any(UUID.class));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processEventChange_withUnapprovedStatus_shouldDeleteEvent() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.events", 0, 0, "key", 
            "{\"payload\":{\"op\":\"u\",\"after\":{\"id\":\"123\",\"status\":\"DRAFT\"}}}");

        ObjectNode payloadNode = createPayloadNode("u");
        ObjectNode eventAfterNode = (ObjectNode) payloadNode.get("after");
        eventAfterNode.put("id", "123");
        eventAfterNode.put("status", "DRAFT");
        JsonNode jsonNode = payloadNode;

        EventChangePayload eventChange = new EventChangePayload();
        eventChange.setId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        eventChange.setStatus("DRAFT");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(EventChangePayload.class))).thenReturn(eventChange);
        when(projectorService.deleteEvent(any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(EventChangePayload.class));
        verify(projectorService).deleteEvent(eq(eventChange.getId()));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processSessionChange_withInsert_shouldCreateSession() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.event_sessions", 0, 0, "key", 
            "{\"payload\":{\"op\":\"c\",\"after\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        JsonNode jsonNode = new ObjectMapper().readTree("{\"payload\":{\"op\":\"c\",\"after\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        SessionChangePayload sessionChange = new SessionChangePayload();
        sessionChange.setId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        sessionChange.setEventId(UUID.fromString("00000000-0000-0000-0000-000000000456"));

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(SessionChangePayload.class))).thenReturn(sessionChange);
        when(eventReadRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(projectorService.createSession(any(UUID.class), any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(SessionChangePayload.class));
        verify(eventReadRepository).existsById(eq("00000000-0000-0000-0000-000000000456"));
        verify(projectorService).createSession(eq(sessionChange.getEventId()), eq(sessionChange.getId()));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processSessionChange_withDelete_shouldDeleteSession() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.event_sessions", 0, 0, "key", 
            "{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        JsonNode jsonNode = new ObjectMapper().readTree("{\"payload\":{\"op\":\"d\",\"before\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        SessionChangePayload sessionChange = new SessionChangePayload();
        sessionChange.setId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        sessionChange.setEventId(UUID.fromString("00000000-0000-0000-0000-000000000456"));

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(SessionChangePayload.class))).thenReturn(sessionChange);
        when(eventReadRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(projectorService.deleteSession(any(UUID.class), any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(SessionChangePayload.class));
        verify(eventReadRepository).existsById(eq("00000000-0000-0000-0000-000000000456"));
        verify(projectorService).deleteSession(eq(sessionChange.getEventId()), eq(sessionChange.getId()));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processSessionChange_withUpdate_shouldUpdateSession() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.event_sessions", 0, 0, "key", 
            "{\"payload\":{\"op\":\"u\",\"after\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        JsonNode jsonNode = new ObjectMapper().readTree("{\"payload\":{\"op\":\"u\",\"after\":{\"id\":\"123\",\"event_id\":\"456\"}}}");

        SessionChangePayload sessionChange = new SessionChangePayload();
        sessionChange.setId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        sessionChange.setEventId(UUID.fromString("00000000-0000-0000-0000-000000000456"));

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), eq(SessionChangePayload.class))).thenReturn(sessionChange);
        when(eventReadRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(projectorService.projectSessionUpdate(any(UUID.class), any(UUID.class))).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(SessionChangePayload.class));
        verify(eventReadRepository).existsById(eq("00000000-0000-0000-0000-000000000456"));
        verify(projectorService).projectSessionUpdate(eq(sessionChange.getEventId()), eq(sessionChange.getId()));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processCoverPhotoChange_withCreate_shouldAddPhoto() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.event_cover_photos", 0, 0, "key", 
            "{\"payload\":{\"op\":\"c\",\"after\":{\"event_id\":\"123\",\"photo_url\":\"test.jpg\"}}}");

        JsonNode rootNode = new ObjectMapper().readTree("{\"payload\":{\"op\":\"c\",\"after\":{\"event_id\":\"123\",\"photo_url\":\"test.jpg\"}}}");
        JsonNode payloadNode = rootNode.path("payload");

        CoverPhotoChangePayload photoChange = new CoverPhotoChangePayload();
        photoChange.setEventId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        photoChange.setPhotoUrl("test.jpg");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);
        when(objectMapper.treeToValue(eq(payloadNode.path("after")), eq(CoverPhotoChangePayload.class))).thenReturn(photoChange);
        when(eventReadRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(projectorService.projectCoverPhotoAdded(any(UUID.class), anyString())).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(CoverPhotoChangePayload.class));
        verify(eventReadRepository).existsById(eq("00000000-0000-0000-0000-000000000123"));
        verify(projectorService).projectCoverPhotoAdded(eq(photoChange.getEventId()), eq("test.jpg"));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processCoverPhotoChange_withDelete_shouldRemovePhoto() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.event_cover_photos", 0, 0, "key", 
            "{\"payload\":{\"op\":\"d\",\"before\":{\"event_id\":\"123\",\"photo_url\":\"test.jpg\"}}}");

        JsonNode rootNode = new ObjectMapper().readTree("{\"payload\":{\"op\":\"d\",\"before\":{\"event_id\":\"123\",\"photo_url\":\"test.jpg\"}}}");
        JsonNode payloadNode = rootNode.path("payload");

        CoverPhotoChangePayload photoChange = new CoverPhotoChangePayload();
        photoChange.setEventId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        photoChange.setPhotoUrl("test.jpg");

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);
        when(objectMapper.treeToValue(eq(payloadNode.path("before")), eq(CoverPhotoChangePayload.class))).thenReturn(photoChange);
        when(eventReadRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(projectorService.projectCoverPhotoRemoved(any(UUID.class), anyString())).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(CoverPhotoChangePayload.class));
        verify(eventReadRepository).existsById(eq("00000000-0000-0000-0000-000000000123"));
        verify(projectorService).projectCoverPhotoRemoved(eq(photoChange.getEventId()), eq("test.jpg"));
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void processSeatingMapChange_withUpdate_shouldUpdateSeatingMap() throws JsonProcessingException {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "dbz.ticketly.public.session_seating_maps", 0, 0, "key", 
            "{\"payload\":{\"op\":\"u\",\"after\":{\"id\":\"789\",\"event_session_id\":\"456\",\"layout_data\":\"{\\\"rows\\\":10,\\\"cols\\\":10}\"}}}");

        JsonNode rootNode = new ObjectMapper().readTree(
            "{\"payload\":{\"op\":\"u\",\"after\":{\"id\":\"789\",\"event_session_id\":\"456\",\"layout_data\":\"{\\\"rows\\\":10,\\\"cols\\\":10}\"}}}");
        JsonNode payloadNode = rootNode.path("payload");

        SeatingMapChangePayload mapChange = new SeatingMapChangePayload();
        mapChange.setId(UUID.fromString("00000000-0000-0000-0000-000000000789"));
        mapChange.setSessionId(UUID.fromString("00000000-0000-0000-0000-000000000456"));
        mapChange.setLayoutData("{\"rows\":10,\"cols\":10}");
        
        // Create a session status response for the repository custom method
        SessionStatusInfo sessionStatusInfo = SessionStatusInfo.builder()
            .id("00000000-0000-0000-0000-000000000123")
            .sessionStatus(SessionStatus.ON_SALE) // Using the value from the code
            .build();

        // Mock the dependencies
        when(objectMapper.readTree(anyString())).thenReturn(rootNode);
        when(objectMapper.treeToValue(eq(payloadNode.path("after")), eq(SeatingMapChangePayload.class))).thenReturn(mapChange);
        when(eventReadRepositoryCustom.findSessionStatusById(anyString())).thenReturn(Mono.just(sessionStatusInfo));
        when(projectorService.projectSeatingMapPatch(any(UUID.class), any(UUID.class), anyString())).thenReturn(Mono.empty());

        // Act
        debeziumEventConsumer.onDebeziumEvent(record, acknowledgment);

        // Assert
        verify(objectMapper).readTree(anyString());
        verify(objectMapper).treeToValue(any(JsonNode.class), eq(SeatingMapChangePayload.class));
        verify(eventReadRepositoryCustom).findSessionStatusById(eq("00000000-0000-0000-0000-000000000456"));
        verify(projectorService).projectSeatingMapPatch(
            eq(UUID.fromString("00000000-0000-0000-0000-000000000123")), 
            eq(mapChange.getSessionId()), 
            eq(mapChange.getLayoutData())
        );
        verify(acknowledgment).acknowledge();
    }
    

}