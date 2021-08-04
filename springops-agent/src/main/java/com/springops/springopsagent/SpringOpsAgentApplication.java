package com.springops.springopsagent;

import javax.annotation.PostConstruct;

import com.springops.springopsagent.service.TaskScheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringOpsAgentApplication  {
	

	@Autowired
	TaskScheduler taskScheduler;

	

	public static void main(String[] args) {
		SpringApplication.run(SpringOpsAgentApplication.class, args);
		
	}

	@PostConstruct
    public void scheduleRecurrently() {
		taskScheduler.scheduleGitCheck();		
    }

}
