package com.ticketly.mseventseatingprojection.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "organizations")
public class OrganizationDocument {
    @Id
    private String id; // The organization UUID from PostgreSQL
    private String name;
    private String logoUrl;
    private String website;
    private String userId;
}
