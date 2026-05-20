package com.mycompany.filenet.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads FileNet connection settings from application.properties.
 * No more hardcoded credentials in Java files.
 */
@Component
public class FileNetConfig {

    @Value("${filenet.uri}")
    public String ceUri;

    @Value("${filenet.username}")
    public String username;

    @Value("${filenet.password}")
    public String password;

    @Value("${filenet.objectstore}")
    public String objectStore;
}