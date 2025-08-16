package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.OrganizationDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends ReactiveMongoRepository<OrganizationDocument, String> {
}
