package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    /**
     * Fetches all category documents from the read model.
     *
     * @return A Flux emitting all category documents.
     */
    public Flux<CategoryDocument> getAllCategories() {
        return categoryRepository.findAll();
    }
}