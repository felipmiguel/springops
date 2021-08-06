package com.springops.springopsagent.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.springops.springopsagent.service.model.AppDeployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

@ActiveProfiles("test")
@SpringBootTest
public class ParsingTests {
    
    @Autowired
    ModelParser parser;

    static final String directoryPath = "/home/fmiguel/springops/pepe";

    @Test
    void parseDirectory(){
        

        List<AppDeployment> deployments = parser.parseDirectory(directoryPath, AppDeployment.class);
        Assert.notNull(deployments, "not possible");

    }


    @Test
    void generateDeploymentFile() throws JsonGenerationException, JsonMappingException, IOException{
        String appName = "sampleapp";
        URL releaseUrl = URI.create(
                "https://springopsartifacts.blob.core.windows.net/releases/simple-microservice-0.0.1-SNAPSHOT.jar")
                .toURL();
        AppDeployment deployment = AppDeployment.builder()
                .appName(appName)
                .version("1.0")
                .MemoryInGb(1)
                .artifactsSource(releaseUrl)
                .instanceCount(1)
                .JvmOptions("-Xms256m -Xmx2048m")
                .environmentVariables(Map.of("env1", "envvalue1", "env2", "envvalue2"))
                .build();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(new File(directoryPath, "sample.yaml"), deployment);
    }
    
}
