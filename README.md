# Ticketly Event Seating Projection Service

## Swagger UI

The Swagger UI for this service is available at:

- [Swagger UI](/docs/swagger-ui.html)

If running locally, you can access it at:

- [http://localhost:8082/api/event-query/docs/swagger-ui/index.html](http://localhost:8082/api/event-query/docs/swagger-ui/index.html)

## OAuth2 Authentication

Swagger UI is configured to use OAuth2 with Keycloak. You may need to authenticate using the provided client credentials and scopes as configured in `application.yml`.

---

For more information, see the `application.yml` and `OpenApiConfig.java` files.

## Event Tracking and Analytics

The service includes features for tracking and analyzing event views and orders. It automatically detects device types (mobile, desktop, tablet, or other) using the User-Agent header.

### View Tracking Endpoints

- `POST /v1/events/{eventId}/views/increment?deviceType={deviceType}` - Increment view count with manual device type specification
- `POST /v1/events/{eventId}/views/track` - Automatically detect device type from User-Agent header
- `POST /v1/events/{eventId}/orders/increment` - Increment order count for an event
- `GET /v1/events/{eventId}/views/stats?fromDate={fromDate}&toDate={toDate}` - Get view statistics for date range

