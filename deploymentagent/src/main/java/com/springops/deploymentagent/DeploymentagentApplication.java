package com.springops.deploymentagent;

import javax.annotation.PostConstruct;

import com.springops.deploymentagent.service.events.GitChangesPublisher;

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
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DeploymentagentApplication  {
	// @Autowired
	// private GitChangesPublisher gitChecker;
	@Value("${springops.git.frequency}")
	private Integer frequency;

	@Autowired
    private JobScheduler jobScheduler;

	@Bean
    public StorageProvider storageProvider(JobMapper jobMapper) {
        InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

	private static Logger LOG = LoggerFactory
      .getLogger(DeploymentagentApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DeploymentagentApplication.class, args);
		
	}

	@PostConstruct
    public void scheduleRecurrently() {
		LOG.info("Scheduling recurrent task to check GIT every minute");
        jobScheduler.<GitChangesPublisher>scheduleRecurrently(Cron.minutely(), x -> x.checkChanges());
    }

}
