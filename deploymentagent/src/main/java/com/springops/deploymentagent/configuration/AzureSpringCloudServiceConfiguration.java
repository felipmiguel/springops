package com.springops.deploymentagent.configuration;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appplatform.models.SpringService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureSpringCloudServiceConfiguration {
    @Autowired
    private AzureResourceManager azureResourceManager;

    @Value("${springops.azure-spring-cloud.resource-group}")
    private String resourceGroup; 
    @Value("${springops.azure-spring-cloud.service-name}")
    private String serviceName; 

    @Bean
    SpringService getCurrentSpringService(){
        SpringService service = azureResourceManager.springServices().getByResourceGroup(resourceGroup, serviceName);
        return service;
    }
    
}
