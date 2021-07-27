package com.springops.deploymentagent.service.impl;

import com.springops.deploymentagent.service.ChangesChecker;
import com.springops.deploymentagent.service.model.AppDeployment;

import org.springframework.stereotype.Service;

@Service
public class GitChangesImpl implements ChangesChecker{

    @Override
    public AppDeployment checkApp(String appName) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
