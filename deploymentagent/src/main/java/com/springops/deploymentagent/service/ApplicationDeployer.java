package com.springops.deploymentagent.service;

import java.io.IOException;

import com.springops.deploymentagent.service.model.AppDeployment;

public interface ApplicationDeployer {

    void deployApp(AppDeployment deployment) throws IOException;

    AppDeployment getCurrentStatus(String appName);

    void removeApp(AppDeployment deployment);
    
}
