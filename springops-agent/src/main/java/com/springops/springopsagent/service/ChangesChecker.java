package com.springops.springopsagent.service;

import java.io.IOException;
import java.util.List;

import com.springops.springopsagent.service.model.AppDeployment;

public interface ChangesChecker {

    List<AppDeployment> checkApp(String appName) throws IOException;
    
}
