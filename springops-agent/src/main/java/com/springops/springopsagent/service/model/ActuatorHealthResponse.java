package com.springops.springopsagent.service.model;

import lombok.Data;

@Data
public class ActuatorHealthResponse {
    String status;
    String[] groups;    
}
