package com.springops.springopsagent.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.springops.springopsagent.service.GitRepoFactory;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitRepoFactoryImpl implements GitRepoFactory {

    @Value("${springops.git.uri}")
    private String gitUri;
    @Value("${springops.git.user}")
    private String gitUserName;
    @Value("${springops.git.password}")
    private String gitPassword;
    @Value("${springops.git.baseDirectory}")
    private String baseDirectory;

    private Map<String, Repository> repositories = new HashMap<String, Repository>();

    private String getAppPath(String appName) {
        return baseDirectory + "/" + appName;
    }

    private Repository buildRepository(String appName) throws IOException {
        String appPath = getAppPath(appName);
        File localPath = new File(appName);
        if (!localPath.exists()){
            localPath.mkdirs();
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(appPath)).readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        return repository;

    }

    @Override
    public Repository getRepository(String appName) throws IOException {
        if (repositories.containsKey(appName)) {
            return repositories.get(appName);
        } else {
            Repository repo = buildRepository(appName);
            repositories.put(appName, repo);
            return repo;
        }

    }

}
