package com.springops.deploymentagent.service.events;

import com.springops.deploymentagent.service.model.AppDeployment;

import org.springframework.context.ApplicationEvent;

public class AppDeploymentEvent extends ApplicationEvent {

    private AppDeployment deployment;

    public AppDeploymentEvent(Object source, AppDeployment deployment) {
        super(source);
        this.deployment = deployment;
    }

    public AppDeployment getDeployment() {
        return this.deployment;
    }

}
