package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends ReactiveMongoRepository<CategoryDocument, String> {
}
