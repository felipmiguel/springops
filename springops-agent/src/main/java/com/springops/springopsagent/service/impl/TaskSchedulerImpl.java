package com.springops.springopsagent.service.impl;

import com.springops.springopsagent.service.TaskScheduler;
import com.springops.springopsagent.service.events.GitChangesPublisher;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class TaskSchedulerImpl implements TaskScheduler {
    @Value("${springops.git.schedule:* * * * *}")
	private String frequency;

	@Autowired
    private JobScheduler jobScheduler;

    private static Logger logger = LoggerFactory
      .getLogger(TaskSchedulerImpl.class);

	

    @Override
    public void scheduleGitCheck(){
        // String frequency = Cron.minutely();
        logger.info("Scheduling recurrent task to check GIT using {} frequency", frequency);
        jobScheduler.<GitChangesPublisher>scheduleRecurrently(frequency, x -> x.checkChanges());
    }
    
    
}
