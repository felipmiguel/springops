package com.springops.deploymentagent.controller;

import javax.validation.Valid;

import com.springops.deploymentagent.service.model.AppDeployment;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobStatusController {

    @GetMapping(value = "/hi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hi() {
        return new ResponseEntity<>("Hi!", HttpStatus.OK);
    }

    @GetMapping(value="/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validate(@Valid AppDeployment deployment){
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
