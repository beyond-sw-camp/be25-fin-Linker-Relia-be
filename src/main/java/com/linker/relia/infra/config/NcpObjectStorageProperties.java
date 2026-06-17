package com.linker.relia.infra.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage.ncp")
public class NcpObjectStorageProperties {
    @NotBlank
    private String endpoint;

    @NotBlank
    private String region;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String bucket;

    @Min(1)
    private int presignedUrlExpirationMinutes;

    @NotBlank
    private String consultationAudioPrefix;
}
