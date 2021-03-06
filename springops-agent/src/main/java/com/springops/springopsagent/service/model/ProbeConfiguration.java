package com.springops.springopsagent.service.model;

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
    int delayBeforeProbes;
    int delayBetweenProbes;    
}
