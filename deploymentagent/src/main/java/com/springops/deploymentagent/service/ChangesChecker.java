package com.springops.deploymentagent.service;

import java.io.IOException;
import java.util.List;

import com.springops.deploymentagent.service.model.AppDeployment;

public interface ChangesChecker {

    List<AppDeployment> checkApp(String appName) throws IOException;
    
}
