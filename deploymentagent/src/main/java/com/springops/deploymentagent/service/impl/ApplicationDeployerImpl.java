package com.springops.deploymentagent.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment.Update;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.springops.deploymentagent.service.ApplicationDeployer;
import com.springops.deploymentagent.service.model.AppDeployment;
import com.springops.deploymentagent.service.model.AppDeployment.AppDeploymentBuilder;

import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationDeployerImpl implements ApplicationDeployer {

    @Autowired
    private SpringService springService;
    static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    

    private void validateDeploymentModel(AppDeployment expectedDeployment) {
        Set<ConstraintViolation<AppDeployment>> violations = validator.validate(expectedDeployment);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
    }

    private void deployNewApp(AppDeployment deployment) throws IOException {
        File jar = getJarFile(deployment);
        springService.apps().define(deployment.getAppName()).defineActiveDeployment("default").withJarFile(jar)
                .withCpu(deployment.getCPU() == null ? 1 : deployment.getCPU())
                .withMemory(deployment.getMemoryInGb() == null ? 1 : deployment.getMemoryInGb())
                .withJvmOptions(deployment.getJvmOptions()).withVersionName(deployment.getVersion())
                .withInstance(deployment.getInstanceCount() == null ? 1 : deployment.getInstanceCount()).attach()
                .create();
    }

    private void updateApp(SpringApp app, AppDeployment deployment) throws IOException {
        Update update = app.getActiveDeployment().update();
        boolean needsUpdate = false;
        if (deployment.getVersion() != null) {
            needsUpdate = true;
            File jar = getJarFile(deployment);
            update = update.withJarFile(jar).withVersionName(deployment.getVersion());
        }
        if (deployment.getCPU() != null) {
            needsUpdate = true;
            update = update.withCpu(deployment.getCPU());
        }
        if (deployment.getInstanceCount() != null) {
            needsUpdate = true;
            update = update.withInstance(deployment.getInstanceCount());
        }
        if (deployment.getJvmOptions() != null) {
            needsUpdate = true;
            update = update.withJvmOptions(deployment.getJvmOptions());
        }
        if (deployment.getMemoryInGb() != null) {
            needsUpdate = true;
            update = update.withMemory(deployment.getMemoryInGb());
        }
        if (deployment.getEnvironmentVariables() != null) {
            needsUpdate = true;
            for (String name : deployment.getEnvironmentVariables().keySet()) {
                update = update.withEnvironment(name, deployment.getEnvironmentVariables().get(name));
            }
        }

        if (needsUpdate) {
            update.apply();
        }
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
        return builder.build();
    }

    

    private AppDeployment getCurrentStatus(SpringApp app) {
        SpringAppDeployment deployment = app.getActiveDeployment();
        String version = deployment.innerModel().properties().source().version();
        return AppDeployment.builder().appName(app.name())
                .environmentVariables(deployment.innerModel().properties().deploymentSettings().environmentVariables())
                .version(version).CPU(deployment.innerModel().properties().deploymentSettings().cpu())
                .JvmOptions(deployment.innerModel().properties().deploymentSettings().jvmOptions())
                .MemoryInGb(deployment.innerModel().properties().deploymentSettings().memoryInGB())
                .instanceCount(app.getActiveDeployment().instances().size()).build();
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

    @Override
    public void deployApp(AppDeployment expectedDeployment) throws IOException {

        validateDeploymentModel(expectedDeployment);

        SpringApp app = getSpringApp(expectedDeployment.getAppName());

        if (app == null) {
            deployNewApp(expectedDeployment);
        } else {
            AppDeployment actualDeployment = getCurrentStatus(app);
            AppDeployment toDeploy = getDiffDeployment(actualDeployment, expectedDeployment);
            updateApp(app, toDeploy);
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
