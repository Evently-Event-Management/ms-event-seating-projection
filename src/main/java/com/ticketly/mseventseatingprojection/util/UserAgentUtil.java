package com.ticketly.mseventseatingprojection.util;

import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Utility class for parsing User Agent string and determining device type
 */
@Slf4j
public class UserAgentUtil {

    /**
     * Determines device type from request headers
     *
     * @param request The server HTTP request
     * @return String representing the device type: "mobile", "tablet", "desktop", or "other"
     */
    public static String getDeviceType(ServerHttpRequest request) {
        String userAgentHeader = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return getDeviceType(userAgentHeader);
    }

    /**
     * Determines device type from User-Agent header string
     *
     * @param userAgentHeader The User-Agent header string
     * @return String representing the device type: "mobile", "tablet", "desktop", or "other"
     */
    public static String getDeviceType(String userAgentHeader) {
        if (userAgentHeader == null || userAgentHeader.isEmpty()) {
            log.debug("User-Agent header is empty, returning 'other'");
            return "other";
        }

        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentHeader);
            DeviceType deviceType = userAgent.getOperatingSystem().getDeviceType();

            log.debug("Detected device type: {}", deviceType);

            return switch (deviceType) {
                case MOBILE -> "mobile";
                case TABLET -> "tablet";
                case COMPUTER -> "desktop";
                default -> "other";
            };
        } catch (Exception e) {
            log.error("Error parsing User-Agent: {}", userAgentHeader, e);
            return "other";
        }
    }
}