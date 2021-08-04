package com.springops.springopsagent.service.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.springops.springopsagent.service.ModelParser;

import org.springframework.stereotype.Service;

@Service
public class ModelParserImpl implements ModelParser {

    private static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public <T> T parseFile(File file, Class<T> valueType) {
        try {
            if (isYaml(file)) {
                return yamlMapper.readValue(file, valueType);
            } else {
                return jsonMapper.readValue(file, valueType);
            }
        } catch (JsonParseException jpex) {
            // TODO: Log parsing error as info
            jpex.printStackTrace();
            return null;
        } catch(JsonMappingException jmex){
            // TODO: Log error as info
            jmex.printStackTrace();
            return null;
        } catch(IOException ioex){
            // TODO: Log io error as warning
            return null;
        }

    }

    @Override
    public <T> T parseFile(String filePath, Class<T> valueType) {
        return parseFile(new File(filePath), valueType);
    }

    private boolean isYaml(File file){
        return isYaml(file.getName());
    }

    private boolean isYaml(String filePath) {
        return (filePath.endsWith(".yml") || filePath.endsWith(".yaml"));
    }

    @Override
    public <T> List<T> parseDirectory(String directoryPath, Class<T> valueType) {
        File localPath = new File(directoryPath);
        File[] files = localPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
            }
        });
        return Arrays.asList(files).stream().map(filePath -> parseFile(filePath, valueType))
                .filter(appDeployement -> appDeployement != null).collect(Collectors.toList());
    }

}
