package com.experimentops.platformapi;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseMysqlTest {

    // Not using a wrapper. Keeping it simple
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.2.0")
            .withDatabaseName("experimentops_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startContainer() {
        mysql.start();
    }

    // This is creating DataSourceConfig for us so we don't have to do it manually. We have to make DataSourceConfig when we have multiple databases, and we need multiple custom URLs to maybe simultaneously run.
    @DynamicPropertySource
    static void registerMysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
}
