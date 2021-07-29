package com.springops.deploymentagent.controller;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobStatusController {

    private JobScheduler jobScheduler;

    private JobStatusController(JobScheduler jobSheduler){
        this.jobScheduler= jobSheduler;
    }

    @GetMapping(value = "/hi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hi() {
        return new ResponseEntity<>("Hi!", HttpStatus.OK);
    }
}
