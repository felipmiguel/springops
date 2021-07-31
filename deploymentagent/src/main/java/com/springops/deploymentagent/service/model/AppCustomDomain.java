package com.springops.deploymentagent.service.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppCustomDomain {
    @NotNull
    String domain;

    String certName;
    String certThumbprint;
}
