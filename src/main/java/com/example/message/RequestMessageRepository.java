package com.example.message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequestMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RequestMessage> rowMapper = (rs, rowNum) -> {
        RequestMessage m = new RequestMessage();
        m.setMessageId(rs.getLong("message_id"));
        m.setRequestId(rs.getLong("request_id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setBody(rs.getString("body"));
        m.setInternal(rs.getInt("internal") == 1);
        m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return m;
    };

    /** internal=false -> müşteri kanalı, internal=true -> ekip kanalı. */
    public List<RequestMessage> findByRequestId(Long requestId, boolean internal) {
        String sql = """
            SELECT message_id, request_id, sender_id, body, internal, created_at
            FROM Eren_request_messages
            WHERE request_id = ? AND internal = ?
            ORDER BY created_at ASC
            """;
        return jdbcTemplate.query(sql, rowMapper, requestId, internal ? 1 : 0);
    }

    public void save(RequestMessage message) {
        String sql = """
            INSERT INTO Eren_request_messages (message_id, request_id, sender_id, body, internal, created_at)
            VALUES (Eren_seq_request_messages.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
                message.getRequestId(),
                message.getSenderId(),
                message.getBody(),
                message.isInternal() ? 1 : 0);
    }
}
