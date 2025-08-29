package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CategoryReadRepository extends ReactiveMongoRepository<CategoryDocument, String> {

    /**
     * Finds all categories with the specified parent ID.
     *
     * @param parentId The ID of the parent category
     * @return A Flux of all subcategories belonging to the specified parent
     */
    Flux<CategoryDocument> findByParentId(String parentId);
}
