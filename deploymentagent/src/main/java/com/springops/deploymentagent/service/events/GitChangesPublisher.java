package com.springops.deploymentagent.service.events;

import java.io.IOException;
import java.util.List;

import com.springops.deploymentagent.service.ApplicationDeployer;
import com.springops.deploymentagent.service.ChangesChecker;
import com.springops.deploymentagent.service.model.AppDeployment;

import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class GitChangesPublisher {

    // @Autowired
    // private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private ChangesChecker checker;
    @Autowired
    private ApplicationDeployer deployer;

    @Autowired
    private JobScheduler jobScheduler;

    private Logger logger = LoggerFactory.getLogger(GitChangesPublisher.class);

    @Job(name = "Check for GIT changes", retries = 2)
    public void checkChanges() {
        
        logger.info("Checking GIT for changes");
        List<AppDeployment> deployments = null;
        try {
            deployments = checker.checkApp("pepe");
        } catch (IOException e) {
            logger.error("There is an error checking GIT for changes", e);
        }
        if (deployments == null) {
            logger.info("No changes");

        } else {
            for (AppDeployment appDeployment : deployments) {
                logger.info("enqueueing appDeployment for app " + appDeployment.getAppName());
                jobScheduler.enqueue( () -> deployer.deployApp(appDeployment));
            }
        }

    }

}
