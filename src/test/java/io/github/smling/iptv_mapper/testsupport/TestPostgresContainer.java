package io.github.smling.iptv_mapper.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;

public class TestPostgresContainer extends PostgreSQLContainer<TestPostgresContainer> {
    private static final String IMAGE = "postgres:16";
    private static final TestPostgresContainer INSTANCE = new TestPostgresContainer()
            .withDatabaseName("iptv-mapper-test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    private TestPostgresContainer() {
        super(IMAGE);
    }

    static {
        // Start exactly once per JVM before any tests access it
        INSTANCE.start();
    }

    public static TestPostgresContainer getInstance() {
        return INSTANCE;
    }
}
