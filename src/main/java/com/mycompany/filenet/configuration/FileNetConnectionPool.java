package com.mycompany.filenet.configuration;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;

@Component
public class FileNetConnectionPool {

    @Autowired
    private FileNetConfig config;

    private Connection connection;
    private ObjectStore objectStore;

    // =========================================================
    // INITIALIZE FILENET CONNECTION
    // =========================================================
    @PostConstruct
    public void init() {

        try {

            System.out.println("Connecting to FileNet: "
                    + config.ceUri);

            // Create connection
            connection = Factory.Connection.getConnection(
                    config.ceUri
            );

            // Create security subject
            Subject subject = UserContext.createSubject(
                    connection,
                    config.username,
                    config.password,
                    "FileNetP8WSI"
            );

            // Push security context
            UserContext.get().pushSubject(subject);

            // Fetch domain
            Domain domain = Factory.Domain.fetchInstance(
                    connection,
                    null,
                    null
            );

            // Fetch object store
            objectStore = Factory.ObjectStore.fetchInstance(
                    domain,
                    config.objectStore,
                    null
            );

            System.out.println(
                    "FileNet connected. ObjectStore: "
                            + objectStore.get_Name()
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to connect to FileNet",
                    e
            );
        }
    }

    // =========================================================
    // RETURN OBJECT STORE
    // =========================================================
    public ObjectStore getObjectStore() {

        Subject subject = UserContext.createSubject(
                connection,
                config.username,
                config.password,
                "FileNetP8WSI"
        );

        UserContext.get().pushSubject(subject);

        return objectStore;
    }

    // =========================================================
    // RETURN CONNECTION
    // =========================================================
    public Connection getConnection() {
        return connection;
    }
}