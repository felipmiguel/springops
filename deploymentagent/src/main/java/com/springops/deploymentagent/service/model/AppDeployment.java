package com.springops.deploymentagent.service.model;

import java.net.URL;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;

/** Application deployment representation */
@Data
@Builder
public class AppDeployment {

    /** Spring cloud application name */
    @NotNull (message = "the app name is mandatory")
    String appName;
    /** Artifact version. It is used to know when the artifact binaries changed so it needs to be deployed */
    @NotNull(message = "version is mandatory")
    String version;
    /** Url with jar file to be deployed */   
    @NotNull(message = "it is necessary to provide an artifact to deploy")
    URL artifactsSource;
    /** Number of instances to deploy */
    
    @Min(value=1, message = "It is necessary at least one instance")
    @Max(value=500, message = "The max amount of instances supported is 500")
    Integer instanceCount;
    /** Memory in Gb per instance */
    @Min(value=1, message = "The min amount of memory per instance is 1Gb")
    @Max(value=8, message = "The max amount of memory per instance is 8Gb")
    Integer MemoryInGb;
    /** Number of CPU per instance */
    @Min(value=1, message = "The min amount of vCPU is 1")
    @Max(value=4, message = "The max amount of vCPU is 8")
    Integer CPU;
    /** Java Virtual Machine parameters */
    String JvmOptions;
    /** Environment variables */
    Map<String, String> environmentVariables;
}
