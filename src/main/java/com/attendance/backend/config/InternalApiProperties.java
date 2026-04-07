package com.attendance.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal.api")
public class InternalApiProperties {

    private String key = "local-admin-web-key";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
