package com.springops.springopsagent.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springops.springopsagent.service.model.ProbeConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class HealthCheckTest {
    @Autowired
    private HealthChecker healthChecker;

    @Test
    void testHealthEndpoint(){
        String testEndpoint = "https://primary:UD0jVzU4SaYqb42NEzAM9qyLjSmGfvfWarLJ16Hpd3oipUMHoMxSVDxhTZUnBF3c@fmiguelasclab.test.azuremicroservices.io/sampleapp/sostage/actuator/";
        // String testEndpoint= "http://localhost:8080/actuator/health";
        ProbeConfiguration probe = ProbeConfiguration.builder()
            .appName("demo")
            .testEndpoint(testEndpoint)
            .delayBetweenProbes(1000)
            .maxAllowedFailures(1)
            .maxRowFailures(1)
            .timesToCheck(5)
            .build();
        
        boolean healty = healthChecker.check(probe);
        assertTrue(healty);
    }
    
}
