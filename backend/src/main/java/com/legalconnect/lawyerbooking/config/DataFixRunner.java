package com.legalconnect.lawyerbooking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataFixRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataFixRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Running DataFixRunner to ensure data integrity...");

        // 1. Fix NULL deleted flags (default to false/0) for Cases
        fixDeletedFlag("cases");
        
        // Users, Lawyers, Admins do not currently have a 'deleted' column, so skipping them.
        // fixDeletedFlag("users");
        // fixDeletedFlag("lawyers");
        // fixDeletedFlag("admins");

        // 2. Fix Case Status Casing (ensure UPPERCASE for Enum mapping)
        fixCaseStatus();

        logger.info("DataFixRunner completed.");
    }

    private void fixDeletedFlag(String tableName) {
        try {
            String sql = "UPDATE " + tableName + " SET deleted = 0 WHERE deleted IS NULL";
            int rows = jdbcTemplate.update(sql);
            if (rows > 0) {
                logger.info("Updated {} rows in '{}' table: set deleted = 0 (false)", rows, tableName);
            }
        } catch (Exception e) {
            logger.error("Failed to update deleted flag for table '{}': {}", tableName, e.getMessage());
        }
    }

    private void fixCaseStatus() {
        try {
            logger.info("Normalizing all case statuses to UPPERCASE and replacing hyphens with underscores...");
            
            // 1. Convert to UPPERCASE first
            String updateUpperSql = "UPDATE cases SET case_status = UPPER(case_status) WHERE case_status IS NOT NULL";
            jdbcTemplate.update(updateUpperSql);
            
            // 2. Replace hyphens with underscores (e.g., IN-PROGRESS -> IN_PROGRESS)
            String updateHyphenSql = "UPDATE cases SET case_status = REPLACE(case_status, '-', '_') WHERE case_status LIKE '%-%'";
            int rows = jdbcTemplate.update(updateHyphenSql);
            
            logger.info("Updated {} rows: normalized case_status (UPPERCASE + underscore).", rows);
        } catch (Exception e) {
            logger.error("Failed to fix case status casing: {}", e.getMessage());
        }
    }
}
