package com.ticketly.mseventseatingprojection.config; // Or your appropriate config package

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
@Slf4j
public class GoogleAnalyticsConfig {

    // Move all your @Value properties here from the service
    @Value("${google.analytics.property-id}")
    private String propertyId;

    @Value("${google.analytics.client-email}")
    private String clientEmail;

    @Value("${google.analytics.private-key}")
    private String privateKey;

    @Value("${google.analytics.client-id}")
    private String clientId;

    @Value("${google.analytics.private-key-id}")
    private String privateKeyId;

    /**
     * Creates a singleton bean for the Google Analytics client.
     * The 'destroyMethod = "close"' tells Spring to call client.close()
     * automatically when the application shuts down, preventing resource leaks.
     */
    @Bean(destroyMethod = "close")
    public BetaAnalyticsDataClient googleAnalyticsClient() throws IOException {
        log.info("Initializing Google Analytics Client...");

        if (clientId == null || clientEmail == null || privateKeyId == null || privateKey == null || propertyId == null) {
            log.error("Missing required Google Analytics configuration. Check application properties.");
            throw new IllegalStateException("Missing required Google Analytics configuration.");
        }

        String formattedPrivateKey = privateKey.replace("\\n", "\n");

        try {
            List<String> scopes = List.of("https://www.googleapis.com/auth/analytics.readonly");

            GoogleCredentials credentials = ServiceAccountCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientEmail(clientEmail)
                    .setPrivateKeyId(privateKeyId)
                    .setPrivateKeyString(formattedPrivateKey)
                    .setScopes(scopes)
                    .build();

            BetaAnalyticsDataClient client = BetaAnalyticsDataClient.create(
                    BetaAnalyticsDataSettings.newBuilder()
                            .setCredentialsProvider(() -> credentials)
                            .build()
            );
            log.info("Google Analytics Client initialized successfully.");
            return client;

        } catch (Exception e) {
            log.error("Failed to initialize Google Analytics client: {}", e.getMessage(), e);
            throw new IOException("Failed to initialize Google Analytics client", e);
        }
    }

    /**
     * Expose the property ID as a bean as well, just for cleanliness.
     * This is optional, but keeps all GA config in one place.
     */
    @Bean
    public String googleAnalyticsPropertyId() {
        return "properties/" + propertyId;
    }
}