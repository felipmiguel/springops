package com.springops.deploymentagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.springops.deploymentagent.service.model.AppDeployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class ExempleAppDeploymentTest {

    @Autowired
    ApplicationDeployer deployer;

    @Test
    void deployApp() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sampleapp";
        AppDeployment deployment = AppDeployment.builder()
                .appName(appName)
                .version("1.0")
                .MemoryInGb(1)
                .artifactsSource(releaseUrl)
                .instanceCount(1)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2"))
                .build();
        deployAppAndAssert(deployment);
    }

    @Test
    void deployAppBlueGreen() throws IOException{
        // ARRANGE
        URL releaseUrl = URI.create(
            "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sampleapp";
        AppDeployment deployment = AppDeployment.builder()
                .blueGreen(true)
                .appName(appName)
                .version("1")
                .MemoryInGb(1)
                .artifactsSource(releaseUrl)
                .instanceCount(1)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2"))
                .build();
        deployAppAndAssert(deployment);
    }

    private void deployAppAndAssert(AppDeployment deployment) throws IOException {
        // ACT
        deployer.deployApp(deployment);
        // ASSERT
        AppDeployment deployedVersion = deployer.getCurrentStatus(deployment.getAppName());
        assertNotNull(deployedVersion);
        assertEquals(1, deployedVersion.getInstanceCount());
        assertFalse(deployedVersion.getHttpsOnly());
        assertFalse(deployedVersion.getIsPublic());
        assertEquals(2, deployedVersion.getEnvironmentVariables().size());
        assertNull(deployedVersion.getCustomDomains());
        assertNotNull(deployedVersion.getTemporaryDisk());
        assertEquals(5, deployedVersion.getTemporaryDisk().getSizeInGb());
        assertEquals("/tmp", deployedVersion.getTemporaryDisk().getMountPath());
        assertNull(deployedVersion.getPersistentDisk());
        // ARRANGE
        deployment.setCPU(2);
        deployment.setMemoryInGb(2);
        deployment.setEnvironmentVariables(Map.of("env1", "envvalue1"));
        // ACT
        deployer.deployApp(deployment);
        // ASSERT
        deployedVersion = deployer.getCurrentStatus(deployment.getAppName());
        assertNotNull(deployedVersion);
        assertEquals(2, deployedVersion.getCPU());
        assertEquals(2, deployedVersion.getMemoryInGb());
        assertEquals(1, deployedVersion.getEnvironmentVariables().size());
        assertEquals("envvalue1", deployedVersion.getEnvironmentVariables().get("env1"));
        assertFalse(deployedVersion.getEnvironmentVariables().containsKey("env2"));
        // CLEANUP
        deployer.removeApp(deployment);
        deployedVersion = deployer.getCurrentStatus(deployment.getAppName());
        assertNull(deployedVersion);
    }

    @Test
    void refreshAppStatus() {
        AppDeployment deployment = deployer.getCurrentStatus("sampleapp");
        assertNotNull(deployment);
    }

}
