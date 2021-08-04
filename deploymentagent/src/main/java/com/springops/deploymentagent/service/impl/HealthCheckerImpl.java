package com.springops.deploymentagent.service.impl;

import java.net.URI;
import java.util.Base64;

import com.springops.deploymentagent.service.HealthChecker;
import com.springops.deploymentagent.service.model.ProbeConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class HealthCheckerImpl implements HealthChecker {

    private Logger logger = LoggerFactory.getLogger(HealthCheckerImpl.class);

    @Override
    public boolean check(ProbeConfiguration configuration) {
        URI uri = URI.create(configuration.getTestEndpoint());
        String normalizedUri = String.format("%s://%s%s%s%s", uri.getScheme(), uri.getHost(), getPort(uri),
                getString(uri.getPath()), getString(uri.getQuery()));
        WebClient webClient = WebClient.create(normalizedUri);

        int failures = 0;
        int failuresInARow = 0;
        try {
            logger.info("Waiting {} ms before starting probes");
            Thread.sleep(configuration.getDelayBeforeProbes());

        } catch (InterruptedException e) {
            logger.info("Interrupted. bye, bye", e);
            return false;
        }
        logger.info("Starting probes to endpoint {}", normalizedUri);
        for (int i = 0; i < configuration.getTimesToCheck() && !failed(failures, failuresInARow, configuration); i++) {

            try {
                logger.info("Probe {}", i);
                ResponseEntity<Object> response = webClient.get().accept(MediaType.APPLICATION_JSON)
                        .header("Authorization",
                                "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()))
                        .retrieve().toEntity(Object.class).block();
                if (response.getStatusCode().isError()) {
                    failures++;
                    failuresInARow++;
                } else {
                    // if (response.getBody().getStatus().equals("UP")) {
                    // failuresInARow = 0;
                    // }
                }
            } catch (Exception e) {
                logger.warn("An error happened during probe", e);
                failures++;
                failuresInARow++;
            }
            try {
                Thread.sleep(configuration.getDelayBetweenProbes());
            } catch (InterruptedException e) {
                logger.info("Interrupted. bye, bye", e);
                return false;
            }

        }

        return !failed(failures, failuresInARow, configuration);
    }

    private String getPort(URI uri) {
        if (uri.getPort() > 0) {
            return ":" + uri.getPort();
        } else {
            return "";
        }
    }

    private String getString(String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    private boolean failed(int failures, int failuresInARow, ProbeConfiguration configuration) {
        return failures >= configuration.getMaxAllowedFailures() || failuresInARow >= configuration.getMaxRowFailures();
    }
}
