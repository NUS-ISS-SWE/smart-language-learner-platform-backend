package com.nus.iss.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class JdbcModerationQueueRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;
    private JdbcModerationQueueRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcModerationQueueRepository(new NamedParameterJdbcTemplate(dataSource));
        jdbc.execute("DROP TABLE IF EXISTS composition, app_user");
        jdbc.execute("CREATE TABLE app_user (id BIGINT PRIMARY KEY, username VARCHAR(255) NOT NULL)");
        jdbc.execute("""
                CREATE TABLE composition (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    title VARCHAR(255),
                    composition_source VARCHAR(32),
                    language VARCHAR(32),
                    composition_status VARCHAR(32),
                    moderation_status VARCHAR(32),
                    moderation_reason_category VARCHAR(32),
                    moderation_reason_note VARCHAR(500),
                    created_at TIMESTAMP NOT NULL,
                    moderated_at TIMESTAMP
                )
                """);
        jdbc.update("INSERT INTO app_user (id, username) VALUES (?, ?)", 1L, "moderator-test-user");
        jdbc.update("""
                INSERT INTO composition (id, user_id, title, composition_source, language, composition_status,
                    moderation_status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, 1L, 1L, "A composition", "RAW_TEXT", "ENGLISH", "DRAFT", "ACTIVE");
    }

    @Test
    void findsQueueWhenAllOptionalFiltersAreAbsent() {
        var response = repository.find(null, null, null, null, null, null, 0, 20);

        assertEquals(1, response.total());
        assertEquals(1, response.items().size());
        assertFalse(response.hasMore());
    }
}
