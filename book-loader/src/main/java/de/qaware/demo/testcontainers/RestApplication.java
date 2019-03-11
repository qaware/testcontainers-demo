package de.qaware.demo.testcontainers;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Marks the module as JAX-RS Application and registers resources. No implementation required.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
}
