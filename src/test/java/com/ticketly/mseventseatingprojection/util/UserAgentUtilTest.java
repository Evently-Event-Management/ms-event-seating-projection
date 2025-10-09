package com.ticketly.mseventseatingprojection.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserAgentUtilTest {

    @Test
    public void testMobileUserAgent() {
        String mobileUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
        assertEquals("mobile", UserAgentUtil.getDeviceType(mobileUserAgent));
    }

    @Test
    public void testDesktopUserAgent() {
        String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        assertEquals("desktop", UserAgentUtil.getDeviceType(desktopUserAgent));
    }

    @Test
    public void testTabletUserAgent() {
        String tabletUserAgent = "Mozilla/5.0 (iPad; CPU OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
        assertEquals("tablet", UserAgentUtil.getDeviceType(tabletUserAgent));
    }

    @Test
    public void testNullUserAgent() {
        assertEquals("other", UserAgentUtil.getDeviceType((String)null));
    }

    @Test
    public void testEmptyUserAgent() {
        assertEquals("other", UserAgentUtil.getDeviceType(""));
    }

    @Test
    public void testGameConsoleUserAgent() {
        String gameConsoleUserAgent = "Mozilla/5.0 (PlayStation 4 3.11) AppleWebKit/537.73 (KHTML, like Gecko)";
        assertEquals("other", UserAgentUtil.getDeviceType(gameConsoleUserAgent));
    }
}