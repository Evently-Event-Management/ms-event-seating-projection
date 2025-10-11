package com.ticketly.mseventseatingprojection;

import com.ticketly.mseventseatingprojection.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestConfig.class)
class MsEventSeatingProjectionApplicationTests {

    @Test
    void contextLoads() {
    }

}
