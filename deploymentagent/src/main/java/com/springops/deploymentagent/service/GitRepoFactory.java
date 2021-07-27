package com.springops.deploymentagent.service;

import java.io.IOException;

import org.eclipse.jgit.lib.Repository;

public interface GitRepoFactory {
    Repository getRepository(String appName) throws IOException;
}
