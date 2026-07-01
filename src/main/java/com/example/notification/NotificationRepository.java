package com.example.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserNotification> rowMapper = (rs, rowNum) -> {
        UserNotification n = new UserNotification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setMessage(rs.getString("message"));
        long reqId = rs.getLong("request_id");
        n.setRequestId(rs.wasNull() ? null : reqId);
        n.setRead(rs.getInt("is_read") == 1);
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return n;
    };

    public void save(UserNotification n) {
        String sql = """
            INSERT INTO Eren_notifications (notification_id, user_id, message, request_id, is_read, created_at)
            VALUES (Eren_seq_notifications.NEXTVAL, ?, ?, ?, 0, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql, n.getUserId(), n.getMessage(), n.getRequestId());
    }

    public int countUnread(Long userId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM Eren_notifications WHERE user_id = ? AND is_read = 0",
            Integer.class, userId);
        return c != null ? c : 0;
    }

    public List<UserNotification> findRecent(Long userId) {
        String sql = """
            SELECT notification_id, user_id, message, request_id, is_read, created_at
            FROM Eren_notifications
            WHERE user_id = ?
            ORDER BY created_at DESC
            FETCH FIRST 20 ROWS ONLY
            """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public void markAllRead(Long userId) {
        jdbcTemplate.update(
            "UPDATE Eren_notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }

    public void deleteAll(Long userId) {
        jdbcTemplate.update("DELETE FROM Eren_notifications WHERE user_id = ?", userId);
    }
}
