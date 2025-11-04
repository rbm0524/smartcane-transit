// com/smartcane/transit/SkTransitProperties.java
package com.smartcane.transit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "sk.transit")
public class SkTransitProperties {
    private String baseUrl;
    private String appKey;
}
