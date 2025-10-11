package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ticketly.mseventseatingprojection.dto.CategoryChangePayload;
import com.ticketly.mseventseatingprojection.dto.EventChangePayload;
import com.ticketly.mseventseatingprojection.dto.OrganizationChangePayload;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustom;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import model.EventStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebeziumEventConsumerTest {

    @Mock
    private ProjectorService projectorService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventReadRepositoryCustom eventReadRepositoryCustom;

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
}