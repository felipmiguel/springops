package com.springops.deploymentagent.service.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppDisk {
    @Min(1)
    int sizeInGb;
    @NotNull
    String mountPath;
}
