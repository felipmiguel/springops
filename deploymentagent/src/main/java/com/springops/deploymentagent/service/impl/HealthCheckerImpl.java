package com.springops.deploymentagent.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springops.deploymentagent.service.HealthChecker;
import com.springops.deploymentagent.service.model.ActuatorHealthResponse;
import com.springops.deploymentagent.service.model.ProbeConfiguration;

import org.apache.commons.compress.utils.IOUtils;
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
        for (int i = 0; i < configuration.getTimesToCheck() && !failed(failures, failuresInARow, configuration); i++) {

            try {
                ResponseEntity<Object> response = webClient.get().accept(MediaType.APPLICATION_JSON)
                        .header("Authorization",
                                "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()))
                        .retrieve().toEntity(Object.class).block();
                if (response.getStatusCode().isError()) {
                    failures++;
                    failuresInARow++;
                } else {
                    // if (response.getBody().getStatus().equals("UP")) {
                    //     failuresInARow = 0;
                    // }
                }
                // Object healthInfo = webClient.get().retrieve().bodyToMono(Object.class)
                // .block();
                // if (healthInfo.getStatus().equals("UP")) {
                // failuresInARow = 0;
                // } else {
                // failures++;
                // failuresInARow++;
                // }
            } catch (Exception e) {
                logger.warn("An error happened during probe", e);
                failures++;
                failuresInARow++;
            }
            try {
                Thread.sleep(configuration.getDelayBetweenProbes());
            } catch (InterruptedException e) {
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

    // @Override
    // public boolean check(ProbeConfiguration configuration) {
    // int failures = 0;
    // int failuresInARow = 0;

    // for (int i = 0; i < configuration.getTimesToCheck() && !failed(failures,
    // failuresInARow, configuration); i++) {

    // try {

    // ActuatorHealthResponse response =
    // retrieveObject(configuration.getTestEndpoint(),
    // ActuatorHealthResponse.class);

    // if (response == null) {
    // failures++;
    // failuresInARow++;
    // }
    // if (response.getStatus().equals("UP")) {
    // failuresInARow = 0;
    // }

    // } catch (Exception e) {
    // logger.warn("An error happened during probe", e);
    // failures++;
    // failuresInARow++;
    // }
    // try {
    // Thread.sleep(configuration.getDelayBetweenProbes());
    // } catch (InterruptedException e) {
    // return false;
    // }

    // }

    // return !failed(failures, failuresInARow, configuration);

    // }

    private <T> T retrieveObject(String endpoint, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            URL url = URI.create(endpoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.connect();
            T result;
            try (InputStream inputStream = connection.getInputStream()) {

                result = mapper.readValue(inputStream, valueType);
            }
            connection.disconnect();
            return result;
        } catch (MalformedURLException e) {
            logger.error("Invalid address", e);
            return null;
        } catch (IOException e) {
            logger.error("Cannot retrieve object", e);
            return null;
        }
    }

}
