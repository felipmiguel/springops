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
import java.util.Objects;
import java.util.Optional;

import com.springops.deploymentagent.service.model.AppCustomDomain;
import com.springops.deploymentagent.service.model.AppDeployment;
import com.springops.deploymentagent.service.model.AppDisk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class AppDeploymentTest {

    @Autowired
    ApplicationDeployer deployer;

    @Test
    void deployApp() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "simple-app";
        AppDeployment deployment = AppDeployment.builder().appName(appName).version("1.0").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2")).build();
        deployAppAndAssert(deployment);
    }

    @Test
    void deployAppBlueGreen() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sample-bluegreen-app";
        AppDeployment deployment = AppDeployment.builder().blueGreen(true).appName(appName).version("1").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2")).build();
        deployAppAndAssert(deployment);
    }

    @Test
    void deployWithAppProperties() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sample-app-props";
        AppDeployment deployment = AppDeployment.builder().appName(appName).version("1").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1).endToEndTls(true)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2")).isPublic(true).httpsOnly(true)
                .temporaryDisk(AppDisk.builder().sizeInGb(3).mountPath("/tmp").build())
                // .persistentDisk(AppDisk.builder().sizeInGb(5).mountPath("/persistent").build())
                .build();
        deployAndAssert(deployment);
        deployment.setCPU(2);
        deployment.setEndToEndTls(false);
        deployment.setPersistentDisk(null);
        deployment.setIsPublic(false);
        deployment.setHttpsOnly(false);
        deployment.getTemporaryDisk().setSizeInGb(5);
        deployAndAssert(deployment);
        cleanUp(deployment);
    }

    @Test
    void deployAndChangeRuntime() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sample-runtime";
        AppDeployment deployment = AppDeployment.builder().appName(appName).version("1").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1).endToEndTls(true)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2")).isPublic(true).httpsOnly(true)
                .temporaryDisk(AppDisk.builder().sizeInGb(3).mountPath("/tmp").build())
                // .persistentDisk(AppDisk.builder().sizeInGb(5).mountPath("/persistent").build())
                .build();
        deployAndAssert(deployment);
        deployment.setRuntime("Java_11");
        deployAndAssert(deployment);
        cleanUp(deployment);
    }

    @Test
    void deployWithIdentity() throws IOException {
        // ARRANGE
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/spring-cloud-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        String appName = "sample-identity";
        AppDeployment deployment = AppDeployment.builder().appName(appName).version("1").MemoryInGb(1)
                .artifactsSource(releaseUrl).instanceCount(1).endToEndTls(true)
                .environmentVariables(Map.of("env1", "val1", "env2", "val2")).isPublic(true).httpsOnly(true)
                .temporaryDisk(AppDisk.builder().sizeInGb(3).mountPath("/tmp").build()).identity(true)
                // .persistentDisk(AppDisk.builder().sizeInGb(5).mountPath("/persistent").build())
                .build();
        deployAndAssert(deployment);
        deployment.setIdentity(false);
        deployAndAssert(deployment);
        cleanUp(deployment);

    }

    private void deployAppAndAssert(AppDeployment deployment) throws IOException {
        AppDeployment deployedVersion;
        deployAndAssert(deployment);
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
        cleanUp(deployment);
    }

    private void deployAndAssert(AppDeployment deployment) throws IOException {
        // ACT
        deployer.deployApp(deployment);
        // ASSERT
        AppDeployment deployedVersion = deployer.getCurrentStatus(deployment.getAppName());
        assertNotNull(deployedVersion);
        assertEquals(deployment.getInstanceCount() == null ? 1 : deployment.getInstanceCount(),
                deployedVersion.getInstanceCount());
        assertEquals(deployment.getRuntime() == null ? "Java_8" : deployment.getRuntime(),
                deployedVersion.getRuntime());

        assertEquals(deployment.getHttpsOnly() == null ? false : deployment.getHttpsOnly(),
                deployedVersion.getHttpsOnly());
        assertEquals(deployment.getIsPublic() == null ? false : deployment.getIsPublic(),
                deployedVersion.getIsPublic());
        assertEquals(deployment.getEnvironmentVariables().size(), deployedVersion.getEnvironmentVariables().size());
        assertEquals(deployment.getIdentity() == null ? false : deployment.getIdentity(),
                deployedVersion.getIdentity());
        for (String envVar : deployment.getEnvironmentVariables().keySet()) {
            assertTrue(deployedVersion.getEnvironmentVariables().containsKey(envVar));
            assertEquals(deployment.getEnvironmentVariables().get(envVar),
                    deployedVersion.getEnvironmentVariables().get(envVar));
        }

        if (deployment.getTemporaryDisk() == null) {
            assertEquals("/tmp", deployedVersion.getTemporaryDisk().getMountPath());
            assertEquals(5, deployedVersion.getTemporaryDisk().getSizeInGb());
        } else {
            assertTrue(Objects.deepEquals(deployment.getTemporaryDisk(), deployedVersion.getTemporaryDisk()));
        }

        if (deployment.getPersistentDisk() == null) {
            assertNull(deployedVersion.getPersistentDisk());
        } else {
            assertNotNull(deployedVersion.getPersistentDisk());
            assertTrue(Objects.deepEquals(deployment.getPersistentDisk(), deployedVersion.getPersistentDisk()));
        }

        // assertEquals(deployment.getIsPublic(), deployedVersion.getIsPublic());
        // assertEquals(deployment.getHttpsOnly(), deployedVersion.getHttpsOnly());
        assertEquals(deployment.getCustomDomains() == null, deployedVersion.getCustomDomains() == null);
        if (deployment.getCustomDomains() != null) {
            for (AppCustomDomain domain : deployment.getCustomDomains()) {

                Optional<AppCustomDomain> deployedDomain = deployedVersion.getCustomDomains().stream()
                        .filter(cd -> cd.getDomain().equals(domain.getDomain())).findFirst();
                assertTrue(deployedDomain.isPresent());
                assertEquals(domain.getCertThumbprint(), deployedDomain.get().getCertThumbprint());
                // assertEquals(domain.getCertName(), deployedDomain.get().getCertName());

            }
        }
        assertEquals(deployment.getEndToEndTls() == null ? false : deployment.getEndToEndTls(),
                deployedVersion.getEndToEndTls());

    }

    private void cleanUp(AppDeployment deployment) {
        AppDeployment deployedVersion;
        // CLEANUP
        deployer.removeApp(deployment);
        deployedVersion = deployer.getCurrentStatus(deployment.getAppName());
        assertNull(deployedVersion);
    }

    @Test
    void refreshAppStatus() {
        AppDeployment deployment = deployer.getCurrentStatus("sample-identity");
        assertNotNull(deployment);
    }

}
