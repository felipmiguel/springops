package com.springops.springopsagent.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.springops.springopsagent.service.ChangesChecker;
import com.springops.springopsagent.service.ModelParser;
import com.springops.springopsagent.service.model.AppDeployment;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.merge.MergeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service
public class GitChangesImpl implements ChangesChecker {

    @Autowired
    private ModelParser parser;

    @Value("${springops.git.uri}")
    private String gitUri;
    @Value("${springops.git.user}")
    private String gitUserName;
    @Value("${springops.git.password}")
    private String gitPassword;
    @Value("${springops.git.baseDirectory}")
    private String baseDirectory;

    private Logger logger = LoggerFactory.getLogger(GitChangesImpl.class);

    private String getAppPath(String appName) {
        return baseDirectory + "/" + appName;
    }

    @Override
    public List<AppDeployment> checkApp(String appName) throws IOException {
        if (checkChanges(appName)) {
            return parser.parseDirectory(getAppPath(appName), AppDeployment.class);
        }
        return null;
    }

    private boolean checkChanges(String appName) {
        logger.info("Check GIT changes for app " + appName);
        String appPath = getAppPath(appName);
        logger.info("Local path: " + appPath);
        File localPath = new File(appPath);
        if (!localPath.exists()) {
            localPath.mkdirs();
            logger.info("Local path doesn't exist. Clone repo");
            cloneRepo(gitUri, localPath);
            return true;
        } else {
            return getPull(localPath);
        }

    }

    private void cloneRepo(String remotePath, File localPath) {

        CloneCommand cloneCommand = new CloneCommand();
        cloneCommand.setURI(remotePath);
        cloneCommand.setProgressMonitor(new TextProgressMonitor());
        cloneCommand.setDirectory(localPath);
        cloneCommand.setBranch("master");
        try {
            cloneCommand.call();
        } catch (InvalidRemoteException e) {
            logger.error("Error clonning repo", e);
        } catch (TransportException e) {
            logger.error("Error clonning repo", e);
        } catch (GitAPIException e) {
            logger.error("Error clonning repo", e);
        }
    }

    /** pull last changes, returns true if there are changes */
    private boolean getPull(File localRepoPath) {
        logger.info("pulling on " + localRepoPath);
        try (Git git = Git.open(localRepoPath)) {

            PullCommand pull = git.pull();
            pull.setStrategy(MergeStrategy.THEIRS);
            pull.setRebase(true);
            pull.setProgressMonitor(new TextProgressMonitor());
            PullResult result = pull.call();

            return result.isSuccessful() && result.getFetchResult().getTrackingRefUpdates().isEmpty() == false;

        } catch (Exception e) {
            logger.error("Error pulling changes", e);
            return false;
        }
    }

}
