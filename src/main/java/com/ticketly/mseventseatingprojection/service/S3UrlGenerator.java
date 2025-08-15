package com.ticketly.mseventseatingprojection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3UrlGenerator {

    @Value("${aws.s3.public-base-url}")
    private String publicBaseUrl;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Generates a full, public, non-expiring URL for a given S3 object key.
     * In a 'dev' profile, it constructs the URL for LocalStack.
     * In production, it would use the configured public base URL (e.g., a CDN).
     *
     * @param key The S3 object key (e.g., "cover-photos/uuid.png")
     * @return The full public URL.
     */
    public String generatePublicUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        // For local development, we construct the path-style URL for LocalStack
        if ("dev".equals(activeProfile)) {
            return String.format("%s/%s/%s", publicBaseUrl, bucketName, key);
        }

        // For production, the base URL would be your CDN or public S3 endpoint
        return String.format("%s/%s", publicBaseUrl, key);
    }
}
