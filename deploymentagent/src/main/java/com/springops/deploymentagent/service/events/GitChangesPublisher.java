package com.springops.deploymentagent.service.events;

import java.io.IOException;
import java.util.List;

import com.springops.deploymentagent.service.ChangesChecker;
import com.springops.deploymentagent.service.model.AppDeployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class GitChangesPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private ChangesChecker checker;

    private Logger logger = LoggerFactory.getLogger(GitChangesPublisher.class);

    public void checkChanges() {
        logger.info("Checking GIT for changes");
        List<AppDeployment> deployments = null;
        try {
            deployments = checker.checkApp("pepe");
        } catch (IOException e) {
            logger.error("There is an error checking GIT for changes", e);
        }
        if (deployments == null){
            logger.info("No changes");

        } else {
            for (AppDeployment appDeployment : deployments) {
                logger.info("publishing appDeployment event for app " + appDeployment.getAppName());
                applicationEventPublisher.publishEvent(new AppDeploymentEvent(this, appDeployment));
            }
        }

    }

}
