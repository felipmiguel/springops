package com.springops.deploymentagent.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.models.DeploymentResourceStatus;
import com.azure.resourcemanager.appplatform.models.ManagedIdentityProperties;
import com.azure.resourcemanager.appplatform.models.ManagedIdentityType;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment.Update;
import com.azure.resourcemanager.appplatform.models.SpringAppDomain;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.springops.deploymentagent.service.ApplicationDeployer;
import com.springops.deploymentagent.service.HealthChecker;
import com.springops.deploymentagent.service.model.AppCustomDomain;
import com.springops.deploymentagent.service.model.AppDeployment;
import com.springops.deploymentagent.service.model.AppDeployment.AppDeploymentBuilder;
import com.springops.deploymentagent.service.model.AppDisk;
import com.springops.deploymentagent.service.model.ProbeConfiguration;

import org.apache.commons.compress.utils.IOUtils;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationDeployerImpl implements ApplicationDeployer {

    @Autowired
    private SpringService springService;
    static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static final Collection<RuntimeVersion> allowedRuntimes = RuntimeVersion.values();

    @Autowired
    private HealthChecker healthChecker;

    private Logger logger = LoggerFactory.getLogger(ApplicationDeployerImpl.class);

    private void validateDeploymentModel(AppDeployment expectedDeployment) {
        Set<ConstraintViolation<AppDeployment>> violations = validator.validate(expectedDeployment);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
        if (expectedDeployment.getRuntime() != null) {
            RuntimeVersion v = RuntimeVersion.fromString(expectedDeployment.getRuntime());
            if (!allowedRuntimes.contains(v)) {
                String values = allowedRuntimes.stream().map(rv -> rv.toString()).collect(Collectors.joining(","));
                throw new ValidationException(String.format("The runtime version %s is not valid. Allowed values %s",
                        expectedDeployment.getRuntime(), values));
            }
        }
    }

    private SpringApp deployNewApp(AppDeployment deployment) throws IOException {
        File jar = getJarFile(deployment);

        var create = springService.apps().define(deployment.getAppName()).defineActiveDeployment("default")
                .withJarFile(jar).withMemory(deployment.getMemoryInGb() == null ? 1 : deployment.getMemoryInGb())
                .withRuntime(deployment.getRuntime() == null ? RuntimeVersion.JAVA_8
                        : RuntimeVersion.fromString(deployment.getRuntime()))
                .withCpu(deployment.getCPU() == null ? 1 : deployment.getCPU())
                .withJvmOptions(deployment.getJvmOptions()).withVersionName(deployment.getVersion())
                .withInstance(deployment.getInstanceCount() == null ? 1 : deployment.getInstanceCount());
        // .withEnvironment("dd", "value");
        if (deployment.getEnvironmentVariables() != null && !deployment.getEnvironmentVariables().isEmpty()) {
            for (Entry<String, String> envVar : deployment.getEnvironmentVariables().entrySet()) {
                create.withEnvironment(envVar.getKey(), envVar.getValue());
            }
        }

        SpringApp app = create.attach().create();

        return app;
    }

    private void updateAppBlueGreen(SpringApp app, AppDeployment deployment) throws IOException {
        File jar = getJarFile(deployment);
        String stagingDeploymentName = getStagingDeploymentName(app.activeDeploymentName());
        var create = app.deployments().define(stagingDeploymentName).withJarFile(jar)
                .withCpu(deployment.getCPU() == null ? 1 : deployment.getCPU())
                .withMemory(deployment.getMemoryInGb() == null ? 1 : deployment.getMemoryInGb())
                .withJvmOptions(deployment.getJvmOptions()).withVersionName(deployment.getVersion())
                .withInstance(deployment.getInstanceCount() == null ? 1 : deployment.getInstanceCount());

        if (deployment.getEnvironmentVariables() != null && !deployment.getEnvironmentVariables().isEmpty()) {
            for (Entry<String, String> envVar : deployment.getEnvironmentVariables().entrySet()) {
                create.withEnvironment(envVar.getKey(), envVar.getValue());
            }
        }

        SpringAppDeployment springdeployment = create.create();
        // check until it is up and running
        boolean doCheck = true;
        while (doCheck && springdeployment.status() != DeploymentResourceStatus.RUNNING
                && springdeployment.status() != DeploymentResourceStatus.FAILED
                && springdeployment.status() != DeploymentResourceStatus.STOPPED) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                doCheck = false;
            }
            springdeployment.refresh();
        }
        if (doCheck) {
            if (springdeployment.status() == DeploymentResourceStatus.RUNNING) {
                // check health
                ProbeConfiguration probe = getProbeConfiguration(app, deployment, stagingDeploymentName);
                if (healthChecker.check(probe)) {
                    // activate
                    app.update().withActiveDeployment(springdeployment.name()).apply();
                    // remove other deployments
                    List<SpringAppDeployment> otherDeployments = app.deployments().list().stream()
                            .filter(d -> !d.name().equals(springdeployment.name())).collect(Collectors.toList());
                    for (SpringAppDeployment otherDeployment : otherDeployments) {
                        logger.info("Deleting deployment {}", otherDeployment.name());
                        app.deployments().deleteById(otherDeployment.id());
                    }
                } else {
                    logger.warn("Health check for new deployment failed");
                }

            } else {
                app.deployments().deleteByName(stagingDeploymentName);
            }
        }
    }

    private String getStagingDeploymentName(String activeDeploymentName) {
        switch (activeDeploymentName) {
            case "blue":
            case "default":
                return "green";
            case "green":
                return "blue";
            case "pink":
                return "blue";
            default:
                return "pink";
        }
    }

    private ProbeConfiguration getProbeConfiguration(SpringApp app, AppDeployment deployment,
            String stagingDeploymentName) throws MalformedURLException {
        String baseUrl = getDeploymentBaseUrl(app, stagingDeploymentName);
        String healthEndpointUrl = String.format("%s/actuator", baseUrl);
        return ProbeConfiguration.builder().appName(app.name()).testEndpoint(healthEndpointUrl).delayBetweenProbes(5000)
                .maxAllowedFailures(3).maxRowFailures(3).timesToCheck(5).build();
    }

    private String getDeploymentBaseUrl(SpringApp app, String deploymentName) {
        return String.format("%s/%s/%s", app.parent().listTestKeys().primaryTestEndpoint(), app.name(), deploymentName);
    }

    private void updateApp(SpringApp app, AppDeployment deployment) throws IOException {

        if (deploymentNeedsUpdate(deployment)) {
            Update update = app.getActiveDeployment().update();
            if (deployment.getVersion() != null) {
                File jar = getJarFile(deployment);
                update = update.withJarFile(jar).withVersionName(deployment.getVersion());
            }
            if (deployment.getRuntime() != null) {
                update = update.withRuntime(RuntimeVersion.fromString(deployment.getRuntime()));
            }
            if (deployment.getCPU() != null) {
                update = update.withCpu(deployment.getCPU());
            }
            if (deployment.getInstanceCount() != null) {
                update = update.withInstance(deployment.getInstanceCount());
            }
            if (deployment.getJvmOptions() != null) {
                update = update.withJvmOptions(deployment.getJvmOptions());
            }
            if (deployment.getMemoryInGb() != null) {
                update = update.withMemory(deployment.getMemoryInGb());
            }
            if (deployment.getEnvironmentVariables() != null) {
                for (String name : deployment.getEnvironmentVariables().keySet()) {
                    update = update.withEnvironment(name, deployment.getEnvironmentVariables().get(name));
                }

                // remove unused environment variables
                Map<String, String> currentEnv = app.getActiveDeployment().innerModel().properties()
                        .deploymentSettings().environmentVariables();
                if (currentEnv != null) {
                    for (String name : currentEnv.keySet()) {
                        if (deployment.getEnvironmentVariables() == null
                                || !deployment.getEnvironmentVariables().containsKey(name)) {
                            update = update.withoutEnvironment(name);
                        }
                    }
                }
            }

            update.apply();
        }

    }

    private void updateAppSettings(SpringApp app, AppDeployment deployment) {
        // after creation properties
        boolean requiresUpdate = false;

        if (deployment.getEndToEndTls() != null) {
            requiresUpdate = true;
            app.innerModel().properties().withEnableEndToEndTls(deployment.getEndToEndTls());
        }
        var update = app.update();

        if (deployment.getIdentity() != null) {
            requiresUpdate = true;
            if (deployment.getIdentity()) {
                app.innerModel()
                        .withIdentity(new ManagedIdentityProperties().withType(ManagedIdentityType.SYSTEM_ASSIGNED));
            } else {
                app.innerModel().withIdentity(new ManagedIdentityProperties().withType(ManagedIdentityType.NONE));
            }
        }
        if (deployment.getIsPublic() != null) {
            requiresUpdate = true;
            if (deployment.getIsPublic()) {
                update = update.withDefaultPublicEndpoint();
            } else {
                update = update.withoutDefaultPublicEndpoint();
            }
        }
        if (deployment.getHttpsOnly() != null) {
            requiresUpdate = true;
            if (deployment.getHttpsOnly()) {
                update = update.withHttpsOnly();
            } else {
                update = update.withoutHttpsOnly();
            }
        }
        if (deployment.getCustomDomains() != null) {
            Map<String, SpringAppDomain> currentDomains = app.customDomains().list().stream()
                    .collect(Collectors.toMap(cd -> cd.name(), cd -> cd));
            List<String> domainNamesToBe = deployment.getCustomDomains().stream().map(d -> d.getDomain())
                    .collect(Collectors.toList());
            List<String> domainsToDelete = currentDomains.keySet().stream().filter(cd -> domainNamesToBe.contains(cd))
                    .collect(Collectors.toList());
            List<AppCustomDomain> domainsToBe = new ArrayList<AppCustomDomain>();

            for (AppCustomDomain d : deployment.getCustomDomains()) {
                if (!currentDomains.containsKey(d.getDomain())) {
                    domainsToBe.add(d);
                } else {
                    SpringAppDomain currentDomain = currentDomains.get(d.getDomain());
                    if (!currentDomain.properties().certName().equals(d.getCertName())
                            || !currentDomain.properties().thumbprint().equals(d.getCertThumbprint())) {
                        domainsToBe.add(d);
                    }
                }
            }
            for (AppCustomDomain d : domainsToBe) {
                requiresUpdate = true;
                if (d.getCertThumbprint() != null) {
                    update = update.withCustomDomain(d.getDomain(), d.getCertThumbprint());
                } else {
                    update = update.withCustomDomain(d.getDomain());
                }
            }
            for (String domainToDelete : domainsToDelete) {
                requiresUpdate = true;
                update = update.withoutCustomDomain(domainToDelete);
            }
        }
        if (deployment.getTemporaryDisk() != null) {
            requiresUpdate = true;
            update = update.withTemporaryDisk(deployment.getTemporaryDisk().getSizeInGb(),
                    deployment.getTemporaryDisk().getMountPath());
        }
        if (deployment.getPersistentDisk() != null) {
            requiresUpdate = true;
            if (deployment.getPersistentDisk().getMountPath() == ""
                    && deployment.getPersistentDisk().getSizeInGb() == 0) {
                update = update.withPersistentDisk(0, "/persistent");
                // app.innerModel().properties().withPersistentDisk(new PersistentDisk());
            } else {
                update = update.withPersistentDisk(deployment.getPersistentDisk().getSizeInGb(),
                        deployment.getPersistentDisk().getMountPath());
            }
        }

        if (requiresUpdate) {
            logger.info("An update after creation required");
            update.apply();
        } else {
            logger.info("No application properties update required");
        }

    }

    private boolean deploymentNeedsUpdate(AppDeployment deployment) {
        boolean needsUpdate;
        needsUpdate = deployment.getVersion() != null || deployment.getCPU() != null
                || deployment.getInstanceCount() != null || deployment.getJvmOptions() != null
                || deployment.getMemoryInGb() != null || deployment.getEnvironmentVariables() != null
                || deployment.getRuntime() != null;
        return needsUpdate;
    }

    private boolean applicationNeedsUpdate(AppDeployment deployment) {
        boolean needsUpdate;
        needsUpdate = deployment.getIsPublic() != null || deployment.getHttpsOnly() != null
                || deployment.getEndToEndTls() != null || deployment.getCustomDomains() != null
                || deployment.getTemporaryDisk() != null || deployment.getPersistentDisk() != null
                || deployment.getRuntime() != null || deployment.getIdentity() != null;
        return needsUpdate;

    }

    private File getJarFile(AppDeployment deployment) throws IOException {
        File jar = new File(deployment.getAppName() + ".jar");
        HttpURLConnection connection = (HttpURLConnection) deployment.getArtifactsSource().openConnection();
        connection.connect();
        try (InputStream inputStream = connection.getInputStream();
                OutputStream outputStream = new FileOutputStream(jar)) {
            IOUtils.copy(inputStream, outputStream);
        }
        connection.disconnect();
        return jar;
    }

    private AppDeployment getDiffDeployment(AppDeployment actual, AppDeployment expected) {
        AppDeploymentBuilder builder = AppDeployment.builder();
        builder = builder.appName(expected.getAppName());
        if (!expected.getVersion().equals(actual.getVersion())) {
            builder = builder.version(expected.getVersion()).artifactsSource(expected.getArtifactsSource());
        }
        if (!Objects.equals(expected.getCPU(), actual.getCPU())) {
            builder = builder.CPU(expected.getCPU());
        }
        if (!Objects.equals(expected.getInstanceCount(), actual.getInstanceCount())) {
            builder = builder.instanceCount(expected.getInstanceCount());
        }
        if (!Objects.equals(expected.getJvmOptions(), actual.getJvmOptions())) {
            builder = builder.JvmOptions(expected.getJvmOptions());
        }
        if (!Objects.equals(expected.getMemoryInGb(), actual.getMemoryInGb())) {
            builder = builder.MemoryInGb(expected.getMemoryInGb());
        }
        if (!Objects.equals(expected.getEnvironmentVariables(), actual.getEnvironmentVariables())) {
            builder = builder.environmentVariables(expected.getEnvironmentVariables());
        }

        // check additional settings
        if (!Objects.equals(expected.getIdentity(), actual.getIdentity())) {
            builder.identity(expected.getIdentity());
        }
        if (!Objects.equals(expected.getRuntime(), actual.getRuntime())) {
            builder.runtime(expected.getRuntime());
        }
        if (!Objects.equals(expected.getIsPublic(), actual.getIsPublic())) {
            builder.isPublic(expected.getIsPublic());
        }
        if (!Objects.equals(expected.getHttpsOnly(), actual.getHttpsOnly())) {
            builder.httpsOnly(expected.getHttpsOnly());
        }
        if (!Objects.deepEquals(expected.getCustomDomains(), actual.getCustomDomains())) {
            builder.customDomains(expected.getCustomDomains());
        }
        if (!Objects.deepEquals(expected.getTemporaryDisk(), actual.getTemporaryDisk())) {
            if (expected.getTemporaryDisk() == null) {
                builder.temporaryDisk(new AppDisk(0, ""));
            } else {
                builder.temporaryDisk(expected.getTemporaryDisk());
            }
        }
        if (!Objects.deepEquals(expected.getPersistentDisk(), actual.getPersistentDisk())) {
            if (expected.getPersistentDisk() == null) {
                builder.persistentDisk(new AppDisk(0, ""));
            } else {
                builder.persistentDisk(expected.getPersistentDisk());
            }
        }
        if (!Objects.equals(expected.getEndToEndTls(), actual.getEndToEndTls())) {
            builder.endToEndTls(expected.getEndToEndTls());
        }

        return builder.build();
    }

    private AppDeployment getCurrentStatus(SpringApp app) {

        SpringAppDeployment deployment = app.getActiveDeployment();
        AppDeploymentBuilder builder = AppDeployment.builder();
        if (deployment != null) {
            String version = deployment.innerModel().properties().source().version();
            builder = builder.appName(app.name())
                    .environmentVariables(
                            deployment.innerModel().properties().deploymentSettings().environmentVariables())
                    .version(version).CPU(deployment.innerModel().properties().deploymentSettings().cpu())
                    .JvmOptions(deployment.innerModel().properties().deploymentSettings().jvmOptions())
                    .MemoryInGb(deployment.innerModel().properties().deploymentSettings().memoryInGB())
                    .runtime(deployment.innerModel().properties().deploymentSettings().runtimeVersion().toString())
                    .instanceCount(app.getActiveDeployment().instances().size());
        } else {
            builder = AppDeployment.builder().appName(app.name());
        }
        builder = builder.isPublic(app.isPublic()).httpsOnly(app.isHttpsOnly())
                .endToEndTls(app.innerModel().properties().enableEndToEndTls());
        if (app.identity() == null) {
            builder = builder.identity(false);
        } else {
            builder = builder.identity(app.identity().type().equals(ManagedIdentityType.SYSTEM_ASSIGNED));
        }
        List<SpringAppDomain> appDomains = app.customDomains().list().stream().collect(Collectors.toList());
        if (!appDomains.isEmpty()) {
            builder.customDomains(appDomains.stream().map(ad -> {
                if (ad.properties().certName() != null && !ad.properties().certName().isEmpty()) {
                    return new AppCustomDomain(ad.properties().appName(), ad.properties().certName(),
                            ad.properties().thumbprint());
                } else {
                    return new AppCustomDomain(ad.properties().appName(), null, null);
                }
            }).collect(Collectors.toList()));
        }
        if (app.temporaryDisk() != null && app.temporaryDisk().sizeInGB() > 0) {
            builder.temporaryDisk(new AppDisk(app.temporaryDisk().sizeInGB(), app.temporaryDisk().mountPath()));
        }
        if (app.persistentDisk() != null && app.persistentDisk().sizeInGB() > 0) {
            builder.persistentDisk(new AppDisk(app.persistentDisk().sizeInGB(), app.persistentDisk().mountPath()));
        }
        return builder.build();
    }

    private SpringApp getSpringApp(String appName) {
        SpringApp app;
        try {
            app = springService.apps().getByName(appName);
        } catch (ManagementException mex) {
            if (mex.getResponse().getStatusCode() == 404) {
                app = null;
            } else {
                throw mex;
            }
        }
        return app;
    }

    @Job(name = "Deploy Azure Spring Cloud application")
    @Override
    public void deployApp(@Valid AppDeployment expectedDeployment) throws IOException {

        logger.info("Validate " + expectedDeployment.getAppName());
        validateDeploymentModel(expectedDeployment);

        logger.info("Retrieving Spring Cloud App");
        SpringApp app = getSpringApp(expectedDeployment.getAppName());

        if (app == null) {
            logger.info("App doesn't exist. Deploy new.");
            app = deployNewApp(expectedDeployment);
            updateAppSettings(app, expectedDeployment);
        } else {
            logger.info("App already exists. Deploy update.");
            AppDeployment actualDeployment = getCurrentStatus(app);

            AppDeployment toDeploy = getDiffDeployment(actualDeployment, expectedDeployment);
            if (this.deploymentNeedsUpdate(toDeploy)) {
                if (Objects.equals(true, expectedDeployment.getBlueGreen())) {
                    updateAppBlueGreen(app, expectedDeployment);
                } else {
                    updateApp(app, toDeploy);
                }

            }
            if (this.applicationNeedsUpdate(toDeploy)) {
                updateAppSettings(app, toDeploy);
            }
        }
    }

    @Override
    public AppDeployment getCurrentStatus(String appName) {
        SpringApp app = getSpringApp(appName);
        if (app == null) {
            return null;
        } else {
            return getCurrentStatus(app);
        }
    }

    @Override
    public void removeApp(AppDeployment deployment) {
        springService.apps().deleteByName(deployment.getAppName());
    }
}
