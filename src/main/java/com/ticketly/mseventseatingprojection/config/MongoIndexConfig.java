//package com.ticketly.mseventseatingprojection.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
//import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
//import org.springframework.data.mongodb.core.index.GeospatialIndex;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.Mono;
//
//@Configuration
//@Slf4j
//public class MongoIndexConfig {
//
//    private final ReactiveMongoTemplate reactiveMongoTemplate;
//
//    @Autowired
//    public MongoIndexConfig(ReactiveMongoTemplate reactiveMongoTemplate) {
//        this.reactiveMongoTemplate = reactiveMongoTemplate;
//    }
//
//    @PostConstruct
//    public void initIndexes() {
//        log.info("Initializing MongoDB indexes...");
//
//        // Create a 2dsphere index on sessions.venueDetails.location
//        GeospatialIndex geospatialIndex = new GeospatialIndex("sessions.venueDetails.location");
//        geospatialIndex.typed(GeoSpatialIndexType.GEO_2DSPHERE);
//
//        reactiveMongoTemplate.indexOps("events").createIndex(geospatialIndex)
//                .doOnSuccess(result -> log.info("Successfully created geospatial index on sessions.venueDetails.location"))
//                .doOnError(error -> log.error("Error creating geospatial index: {}", error.getMessage()))
//                .onErrorResume(e -> {
//                    log.error("Failed to create index", e);
//                    return Mono.empty();
//                })
//                .subscribe();
//    }
//}
