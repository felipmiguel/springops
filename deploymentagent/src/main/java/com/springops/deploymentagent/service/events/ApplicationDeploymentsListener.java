package com.springops.deploymentagent.service.events;

import java.io.IOException;

import com.springops.deploymentagent.service.ApplicationDeployer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationDeploymentsListener {
    @Autowired
    private ApplicationDeployer deployer;
    private Logger logger = LoggerFactory.getLogger(ApplicationDeploymentsListener.class);
    @EventListener
    public void onAppDeployment(AppDeploymentEvent deploymentEvent){
        try {
            logger.info("Deploying app: " + deploymentEvent.getDeployment().getAppName());
            deployer.deployApp(deploymentEvent.getDeployment());
            logger.info("Deployed app: " + deploymentEvent.getDeployment().getAppName());
        } catch (IOException ex) {
            logger.error("An error happened during the deployment of App " + deploymentEvent.getDeployment().getAppName(), ex);
        } catch(Exception ex){
            logger.error("An unexpected error happened during the deployment of App " + deploymentEvent.getDeployment().getAppName(), ex);
        }
    }
    
}
