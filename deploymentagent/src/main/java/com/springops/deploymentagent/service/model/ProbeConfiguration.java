package com.springops.deploymentagent.service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProbeConfiguration {
    String appName;
    String testEndpoint;
    int timesToCheck;
    int maxAllowedFailures;
    int maxRowFailures;
    int delayBetweenProbes;    
}
