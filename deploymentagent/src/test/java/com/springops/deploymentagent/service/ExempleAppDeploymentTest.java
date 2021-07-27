package com.springops.deploymentagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.springops.deploymentagent.service.model.AppDeployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ComponentScan(basePackages = { "com.springops.deploymentagent" })
@SpringBootTest
public class ExempleAppDeploymentTest {

    @Autowired
    ApplicationDeployer deployer;

    @Test
    void deployApp() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/simple-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sampleapp";
        AppDeployment deployment = AppDeployment.builder().appName(appName).version("1.0").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1).build();
        // ACT
        deployer.deployApp(deployment);
        // ASSERT
        AppDeployment deployedVersion = deployer.getCurrentStatus(appName);
        assertNotNull(deployedVersion);
        assertEquals(1, deployedVersion.getInstanceCount());
        // ARRANGE
        deployment.setCPU(2);
        deployment.setMemoryInGb(2);
        deployment.setEnvironmentVariables(Map.of("env1", "envvalue1"));
        // ACT
        deployer.deployApp(deployment);
        // ASSERT
        deployedVersion = deployer.getCurrentStatus(appName);
        assertNotNull(deployedVersion);
        assertEquals(2, deployedVersion.getCPU());
        assertEquals(2, deployedVersion.getMemoryInGb());
        // CLEANUP
        deployer.removeApp(deployment);
        deployedVersion = deployer.getCurrentStatus(appName);
        assertNull(deployedVersion);
    }

    @Test
    void refreshAppStatus() {
        AppDeployment deployment = deployer.getCurrentStatus("sampleapp");
        assertNotNull(deployment);
    }

}
