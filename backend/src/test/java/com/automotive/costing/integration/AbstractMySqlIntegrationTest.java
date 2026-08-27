package com.automotive.costing.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base for MySQL integration tests.
 * Host {@code TEST_DB_URL}/{@code DB_URL} preferred; else Testcontainers when Docker works.
 * Static init never throws — tests use {@link #databaseAvailable()}.
 */
public abstract class AbstractMySqlIntegrationTest {

    private static final String HOST_URL = firstEnv("TEST_DB_URL", "DB_URL");
    private static final boolean USE_HOST = HOST_URL != null && !HOST_URL.isBlank();
    private static final MySQLContainer<?> MYSQL = startContainerQuietly();

    private static MySQLContainer<?> startContainerQuietly() {
        if (USE_HOST) {
            return null;
        }
        try {
            if (!isDockerAvailable()) {
                return null;
            }
            @SuppressWarnings("resource")
            MySQLContainer<?> c = new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("product_costing")
                    .withUsername("costing_app")
                    .withPassword("changeme");
            c.start();
            return c;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String firstEnv(String a, String b) {
        try {
            String v = System.getenv(a);
            if (v == null || v.isBlank()) {
                v = System.getenv(b);
            }
            return v;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String hostUser() {
        String u = firstEnv("TEST_DB_USERNAME", "DB_USERNAME");
        return u != null && !u.isBlank() ? u : "costing_app";
    }

    private static String hostPassword() {
        String p = firstEnv("TEST_DB_PASSWORD", "DB_PASSWORD");
        return p != null && !p.isBlank() ? p : "changeme";
    }

    public static boolean isHostDatabase() {
        return USE_HOST;
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean databaseAvailable() {
        return USE_HOST || MYSQL != null;
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (USE_HOST) {
            registry.add("spring.datasource.url", () -> HOST_URL);
            registry.add("spring.datasource.username", AbstractMySqlIntegrationTest::hostUser);
            registry.add("spring.datasource.password", AbstractMySqlIntegrationTest::hostPassword);
        } else if (MYSQL != null) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:1/none");
            registry.add("spring.datasource.username", () -> "none");
            registry.add("spring.datasource.password", () -> "none");
        }
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.costing.base-currency", () -> "USD");
        registry.add("app.jwt.secret", () -> "TEST_SECRET_KEY_AT_LEAST_256_BITS_LONG_FOR_HS256_ALGORITHM_OK");
        registry.add("app.security.require-strong-jwt-secret", () -> "false");
    }
}
