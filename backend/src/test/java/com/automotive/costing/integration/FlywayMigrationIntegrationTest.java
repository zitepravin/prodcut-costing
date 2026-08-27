package com.automotive.costing.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Flyway applies through the latest migration on a real MySQL/MariaDB.
 * <p>
 * Runs when either:
 * <ul>
 *   <li>{@code TEST_DB_URL} / {@code DB_URL} points at a host database, or</li>
 *   <li>Docker is available for Testcontainers</li>
 * </ul>
 * Skipped when neither host DB nor Docker is available.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("com.automotive.costing.integration.AbstractMySqlIntegrationTest#databaseAvailable")
class FlywayMigrationIntegrationTest extends AbstractMySqlIntegrationTest {

    /** Latest migration folder version (keep in sync with db/migration). */
    private static final int EXPECTED_MIN_VERSION = 164;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesThroughLatestMigration() {
        Integer maxVersion = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1",
                Integer.class);
        assertTrue(maxVersion != null && maxVersion >= EXPECTED_MIN_VERSION,
                "Expected Flyway version >= " + EXPECTED_MIN_VERSION + ", got " + maxVersion);

        Integer erpTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name IN "
                        + "('erp_inbound_batch','erp_part_staging','erp_bom_staging','erp_material_price_staging')",
                Integer.class);
        assertTrue(erpTables != null && erpTables == 4,
                "Expected 4 ERP staging tables, found " + erpTables);

        Integer camTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name IN "
                        + "('platform_customer','platform_module','customer_module_entitlement','customer_user_membership')",
                Integer.class);
        assertTrue(camTables != null && camTables == 4,
                "Expected 4 Central Access tables, found " + camTables);
    }
}
