package com.springops.deploymentagent.service;

import com.springops.deploymentagent.service.model.AppDeployment;

public interface ChangesChecker {

    AppDeployment checkApp(String appName);
    
}
