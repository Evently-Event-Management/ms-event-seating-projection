package com.ticketly.mseventseatingprojection.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "categories")
public class CategoryDocument {
    @Id
    private String id;
    private String name;
    private String parentId;
    private String parentName;
}