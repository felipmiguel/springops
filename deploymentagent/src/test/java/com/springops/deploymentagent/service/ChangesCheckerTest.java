package com.springops.deploymentagent.service;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
// @ComponentScan(basePackages = { "com.springops.deploymentagent" })
@SpringBootTest
public class ChangesCheckerTest {

    @Autowired
    private ChangesChecker checker;

    @Test
    void getChanges() throws IOException{
        checker.checkApp("pepe");
    }
    
}
