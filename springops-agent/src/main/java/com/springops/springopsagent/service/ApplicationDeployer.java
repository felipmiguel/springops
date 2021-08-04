package com.springops.springopsagent.service;

import java.io.IOException;

import com.springops.springopsagent.service.model.AppDeployment;

public interface ApplicationDeployer {

    void deployApp(AppDeployment deployment) throws IOException;

    AppDeployment getCurrentStatus(String appName);

    void removeApp(AppDeployment deployment);
    
}
