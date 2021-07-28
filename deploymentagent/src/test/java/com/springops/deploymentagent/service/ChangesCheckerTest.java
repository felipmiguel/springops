package com.springops.deploymentagent.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import com.springops.deploymentagent.service.model.AppDeployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class ChangesCheckerTest {

    @Autowired
    private ChangesChecker checker;

    @Test
    void getChanges() throws IOException{
        List<AppDeployment> deployments = checker.checkApp("pepe");
        assertNotNull(deployments);
        assertFalse(deployments.isEmpty());
    }
    
}
