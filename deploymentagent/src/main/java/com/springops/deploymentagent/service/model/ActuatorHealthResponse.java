package com.springops.deploymentagent.service.model;

import lombok.Data;

@Data
public class ActuatorHealthResponse {
    String status;
    String[] groups;    
}
