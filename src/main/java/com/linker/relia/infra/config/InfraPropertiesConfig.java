package com.linker.relia.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ClovaSttProperties.class,
        NcpObjectStorageProperties.class,
        SolapiSmsProperties.class
})
public class InfraPropertiesConfig {
}
