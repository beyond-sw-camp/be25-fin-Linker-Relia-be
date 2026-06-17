package com.linker.relia.infra.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "clova.stt")
public class ClovaSttProperties {
    @NotBlank
    private String apiKey;

    @NotBlank
    private String invokeUrl;

    @NotBlank
    private String grpcUrl;
}
