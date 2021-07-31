package com.springops.deploymentagent.service;

import com.springops.deploymentagent.service.model.ProbeConfiguration;

public interface HealthChecker {
    boolean check(ProbeConfiguration configuration);    
}
