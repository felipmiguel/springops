package com.springops.deploymentagent.service;

import java.io.File;
import java.util.List;

public interface ModelParser {
    <T> T parseFile(String filePath, Class<T> valueType);

    <T> T parseFile(File file, Class<T> valueType);

    <T> List<T> parseDirectory(String directoryPath, Class<T> valueType);

}
