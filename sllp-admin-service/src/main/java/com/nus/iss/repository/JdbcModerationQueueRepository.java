package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nus.iss.dto.ModerationQueueItem;
import com.nus.iss.dto.ModerationQueueResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JdbcModerationQueueRepository implements ModerationQueueRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public ModerationQueueResponse find(String moderationStatus, Long userId, String compositionSource, String language,
            LocalDateTime from, LocalDateTime toExclusive, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size).addValue("offset", (long) page * size);
        List<String> predicates = new ArrayList<>();
        addFilter(predicates, params, "moderationStatus", moderationStatus, "c.moderation_status = :moderationStatus");
        addFilter(predicates, params, "userId", userId, "c.user_id = :userId");
        addFilter(predicates, params, "compositionSource", compositionSource, "c.composition_source = :compositionSource");
        addFilter(predicates, params, "language", language, "c.language = :language");
        addFilter(predicates, params, "from", from, "c.created_at >= :from");
        addFilter(predicates, params, "toExclusive", toExclusive, "c.created_at < :toExclusive");
        String filters = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM composition c" + filters, params, Long.class);
        List<ModerationQueueItem> items = jdbc.query("""
                SELECT c.id, c.user_id, u.username, c.title, c.composition_source, c.language,
                       c.composition_status, c.moderation_status, c.moderation_reason_category,
                       c.moderation_reason_note, c.created_at, c.moderated_at
                FROM composition c
                JOIN app_user u ON u.id = c.user_id
                """ + filters + " ORDER BY c.created_at DESC, c.id DESC LIMIT :limit OFFSET :offset", params,
                (rs, row) -> new ModerationQueueItem(rs.getLong("id"), rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("title"), rs.getString("composition_source"), rs.getString("language"),
                        rs.getString("composition_status"), rs.getString("moderation_status"),
                        rs.getString("moderation_reason_category"), rs.getString("moderation_reason_note"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("moderated_at") == null ? null : rs.getTimestamp("moderated_at").toLocalDateTime()));
        return new ModerationQueueResponse(items, total, page, size, ((long) page + 1) * size < total);
    }

    private static void addFilter(List<String> predicates, MapSqlParameterSource params, String parameter,
            Object value, String predicate) {
        if (value != null) {
            predicates.add(predicate);
            params.addValue(parameter, value);
        }
    }
}
