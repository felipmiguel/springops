package com.springops.deploymentagent;

import javax.annotation.PostConstruct;

import com.springops.deploymentagent.service.TaskScheduler;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DeploymentagentApplication  {
	

	@Autowired
	TaskScheduler taskScheduler;

	

	public static void main(String[] args) {
		SpringApplication.run(DeploymentagentApplication.class, args);
		
	}

	@PostConstruct
    public void scheduleRecurrently() {
		taskScheduler.scheduleGitCheck();		
    }

}
