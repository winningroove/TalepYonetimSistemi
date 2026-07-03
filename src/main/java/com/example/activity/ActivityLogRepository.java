package com.example.activity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ActivityLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ActivityLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ActivityLog> rowMapper = (rs, rowNum) -> {
        ActivityLog a = new ActivityLog();
        a.setActivityId(rs.getLong("activity_id"));
        a.setRequestId(rs.getLong("request_id"));
        long uid = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : uid);
        a.setAction(rs.getString("action"));
        a.setDetail(rs.getString("detail"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    };

    public List<ActivityLog> findByRequestId(Long requestId) {
        String sql = """
            SELECT activity_id, request_id, user_id, action, detail, created_at
            FROM Eren_activity_log
            WHERE request_id = ?
            ORDER BY created_at ASC, activity_id ASC
            """;
        return jdbcTemplate.query(sql, rowMapper, requestId);
    }

    public List<ActivityLog> findRecentByUserId(Long userId) {
        String sql = """
            SELECT activity_id, request_id, user_id, action, detail, created_at
            FROM Eren_activity_log
            WHERE user_id = ?
            ORDER BY created_at DESC, activity_id DESC
            FETCH FIRST 20 ROWS ONLY
            """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public void save(ActivityLog a) {
        String sql = """
            INSERT INTO Eren_activity_log (activity_id, request_id, user_id, action, detail, created_at)
            VALUES (Eren_seq_activity_log.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql, a.getRequestId(), a.getUserId(), a.getAction(), a.getDetail());
    }
}
