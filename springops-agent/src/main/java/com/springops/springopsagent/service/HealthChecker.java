package com.springops.springopsagent.service;

import com.springops.springopsagent.service.model.ProbeConfiguration;

public interface HealthChecker {
    boolean check(ProbeConfiguration configuration);    
}
