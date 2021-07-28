package com.springops.deploymentagent.service;

import java.io.IOException;

import com.springops.deploymentagent.service.model.AppDeployment;

public interface ChangesChecker {

    AppDeployment checkApp(String appName) throws IOException;
    
}
