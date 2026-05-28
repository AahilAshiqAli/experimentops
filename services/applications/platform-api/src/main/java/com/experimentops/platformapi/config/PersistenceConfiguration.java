package com.experimentops.platformapi.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement // enables transaction based database operations
@EntityScan(basePackages = "com.experimentops.platformapi.model.entity")
@EnableJpaRepositories(basePackages = "com.experimentops.platformapi.dal.repository")
public class PersistenceConfiguration {

    // Configure database properties from application.properties
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    // Configure database connection pooling using Hikari Library
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    // Configuring Factory bean that is given to TransactionManager
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            Environment environment
    ) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(environment.getProperty("spring.jpa.show-sql", Boolean.class, false));
        vendorAdapter.setDatabasePlatform(environment.getProperty("spring.jpa.properties.hibernate.dialect"));

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("com.experimentops.platformapi");
        factory.setJpaPropertyMap(jpaProperties(environment));
        return factory;
    }

    // Using the EntityManagerFactory Bean to create TransactionManager which manages database transactions and Java entities using Hibernate
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private Map<String, Object> jpaProperties(Environment environment) {
        Map<String, Object> properties = new LinkedHashMap<>();
        putIfPresent(properties, "hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        putIfPresent(properties, "hibernate.dialect", environment.getProperty("spring.jpa.properties.hibernate.dialect"));
        putIfPresent(
                properties,
                "hibernate.jdbc.lob.non_contextual_creation",
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation")
        );
        return properties;
    }

    private void putIfPresent(Map<String, Object> properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }
}
