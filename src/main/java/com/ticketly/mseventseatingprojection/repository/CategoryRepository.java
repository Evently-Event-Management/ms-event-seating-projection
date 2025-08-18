package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CategoryRepository extends ReactiveMongoRepository<CategoryDocument, String> {
    /**
     * Finds all parent categories (where parentId is null)
     *
     * @return A Flux of all parent categories
     */
    Flux<CategoryDocument> findByParentIdIsNull();
}
