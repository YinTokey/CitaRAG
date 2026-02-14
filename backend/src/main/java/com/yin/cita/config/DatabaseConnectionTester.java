package com.yin.cita.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseConnectionTester implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseConnectionTester(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==========================================");
        System.out.println("DATASOURCE VERIFICATION");
        System.out.println("==========================================");
        System.out.println("DataSource Class: " + dataSource.getClass().getName());

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            System.out.println("Pool Name: " + hikari.getPoolName());
            System.out.println("Minimum Idle: " + hikari.getMinimumIdle());
            System.out.println("Maximum Pool Size: " + hikari.getMaximumPoolSize());
            System.out.println("Idle Timeout: " + hikari.getIdleTimeout());
            System.out.println("Auto Commit: " + hikari.isAutoCommit());
            System.out.println("Validation Timeout: " + hikari.getValidationTimeout());
            System.out.println("==========================================");
        } else {
            System.out.println("WARNING: Not using HikariCP!");
        }
    }
}
