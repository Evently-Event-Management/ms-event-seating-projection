package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.service.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1/categories") // Public-facing endpoint
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    /**
     * Public endpoint to fetch all categories for use in dropdowns, etc.
     *
     * @return A Flux of all category documents.
     */
    @GetMapping
    public ResponseEntity<Flux<CategoryDocument>> getAllCategories() {
        return ResponseEntity.ok(categoryQueryService.getAllCategories());
    }

    /**
     * Public endpoint to fetch only parent categories (where parentId is null).
     *
     * @return A Flux of parent category documents.
     */
    @GetMapping("/parents")
    public ResponseEntity<Flux<CategoryDocument>> getParentCategories() {
        return ResponseEntity.ok(categoryQueryService.getParentCategories());
    }
}
