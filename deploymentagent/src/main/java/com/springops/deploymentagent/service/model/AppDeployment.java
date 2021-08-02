package com.springops.deploymentagent.service.model;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Application deployment representation */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppDeployment {

    /** Spring cloud application name */
    @NotNull(message = "the app name is mandatory")
    String appName;
    /**
     * Artifact version. It is used to know when the artifact binaries changed so it
     * needs to be deployed
     */
    @NotNull(message = "version is mandatory")
    String version;
    /** Url with jar file to be deployed */
    @NotNull(message = "it is necessary to provide an artifact to deploy")
    URL artifactsSource;
    /** Number of instances to deploy */

    Boolean blueGreen;

    @Min(value = 1, message = "It is necessary at least one instance")
    @Max(value = 500, message = "The max amount of instances supported is 500")
    Integer instanceCount;
    /** Memory in Gb per instance */
    @Min(value = 1, message = "The min amount of memory per instance is 1Gb")
    @Max(value = 8, message = "The max amount of memory per instance is 8Gb")
    Integer MemoryInGb;
    /** Number of CPU per instance */
    @Min(value = 1, message = "The min amount of vCPU is 1")
    @Max(value = 4, message = "The max amount of vCPU is 8")
    Integer CPU;

    /** Java Virtual Machine parameters */
    String JvmOptions;

    /** Environment variables */
    Map<String, String> environmentVariables;

    // Application SETTINGS

    Boolean identity;
    /** Runtime version: Java_8, Java_11, NetCore_31 */
    String runtime;

    /** The application exposes a public endpoint */
    Boolean isPublic;

    /** The application can be accessed using https only or not */
    Boolean httpsOnly;

    /** End to end TLS to secure traffic from ingress controller to apps */
    Boolean endToEndTls;

    /** Application custom domain */
    List<AppCustomDomain> customDomains;

    /** Application temporary disk */
    AppDisk temporaryDisk;
    /** Application persistent disk */
    AppDisk persistentDisk;
}
