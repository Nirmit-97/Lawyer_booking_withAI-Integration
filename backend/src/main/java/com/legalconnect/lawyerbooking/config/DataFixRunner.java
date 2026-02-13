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
        
        // 2. Fix Case Status Casing (ensure UPPERCASE for Enum mapping)
        fixCaseStatus();

        // 3. Normalize Case Types and Specializations
        normalizeCaseTypes();

        // 4. Fix new Case feature flags (NULL -> 1/true)
        fixCaseFlags();

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

    private void normalizeCaseTypes() {
        try {
            logger.info("Normalizing case types and specializations...");
            
            // 1. Update cases table
            String caseTypeSql = "UPDATE cases SET case_type = 'OTHER' " +
                                "WHERE case_type IS NOT NULL AND UPPER(TRIM(case_type)) NOT IN " +
                                "('CRIMINAL', 'CIVIL', 'FAMILY', 'CORPORATE', 'INTELLECTUAL_PROPERTY', 'REAL_ESTATE', 'PROPERTY', 'LABOR', 'LABOUR', 'TAX', 'CYBER', 'CYBER_CRIME', 'OTHER')";
            int caseRows = jdbcTemplate.update(caseTypeSql);
            
            // 2. Update lawyer_specializations table
            String specSql = "UPDATE lawyer_specializations SET specialization = 'OTHER' " +
                             "WHERE specialization IS NOT NULL AND UPPER(TRIM(specialization)) NOT IN " +
                             "('CRIMINAL', 'CIVIL', 'FAMILY', 'CORPORATE', 'INTELLECTUAL_PROPERTY', 'REAL_ESTATE', 'PROPERTY', 'LABOR', 'LABOUR', 'TAX', 'CYBER', 'CYBER_CRIME', 'OTHER')";
            int specRows = jdbcTemplate.update(specSql);
            
            // 3. Clean orphaned audio records
            String audioSql = "UPDATE client_audio SET case_id = NULL " +
                              "WHERE case_id IS NOT NULL AND case_id NOT IN (SELECT id FROM cases)";
            int audioRows = jdbcTemplate.update(audioSql);
            
            logger.info("Normalization complete: {} cases, {} specializations, {} audio records updated.", caseRows, specRows, audioRows);
        } catch (Exception e) {
            logger.error("Failed to normalize data: {}", e.getMessage());
        }
    }

    private void fixCaseFlags() {
        try {
            logger.info("Initializing NULL case feature flags...");
            String sql = "UPDATE cases SET " +
                         "share_masked_only = COALESCE(share_masked_only, 1), " +
                         "share_docs_after_accept = COALESCE(share_docs_after_accept, 1), " +
                         "enable_chat_after_accept = COALESCE(enable_chat_after_accept, 1) " +
                         "WHERE share_masked_only IS NULL OR share_docs_after_accept IS NULL OR enable_chat_after_accept IS NULL";
            int rows = jdbcTemplate.update(sql);
            if (rows > 0) {
                logger.info("Initialized flags for {} cases.", rows);
            }
        } catch (Exception e) {
            logger.error("Failed to fix case flags: {}", e.getMessage());
        }
    }
}
