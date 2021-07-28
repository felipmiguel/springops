package com.springops.deploymentagent;

import com.springops.deploymentagent.service.events.GitChangesPublisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class DeploymentagentApplication implements CommandLineRunner {
	@Autowired
	private GitChangesPublisher gitChecker;
	@Value("${springops.git.frequency}")
	private Integer frequency;

	private static Logger LOG = LoggerFactory
      .getLogger(DeploymentagentApplication.class);

	public static void main(String[] args) {
		LOG.info("STARTING APP...");
		SpringApplication.run(DeploymentagentApplication.class, args);
		LOG.info("CLOSING APP...");
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("EXECUTING : command line runner");
		while(true){
			gitChecker.checkChanges();
			LOG.info("go to sleep {} milliseconds", frequency);
			Thread.sleep(frequency);
		}
	}

}
