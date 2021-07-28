package com.springops.deploymentagent.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArmConfiguration {

    @Value("${springops.azure-spring-cloud.subscription-id}")
    private String subscriptionId;

    
    private Logger logger = LoggerFactory.getLogger(AzureSpringCloudServiceConfiguration.class);

    @Bean
    AzureResourceManager getAzureResourceManager() {
        logger.info("Creating Azure Resource Manager using AzureProfile " + AzureEnvironment.AZURE);
        final AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        logger.info("Getting TokenCredential");
        final TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()).build();

        logger.info("configuring Azure Resource Manager");
        AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BASIC).authenticate(credential, profile)
                .withSubscription(subscriptionId);
        return azureResourceManager;
    }
}
