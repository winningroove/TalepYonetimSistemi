// repository/RequestRepository.java
package com.example.repository;

import com.example.enums.RequestStatus;
import com.example.model.Request;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Request> rowMapper = (rs, rowNum) -> {
        Request request = new Request();
        request.setRequestId(rs.getLong("request_id"));
        request.setCustomerId(rs.getLong("customer_id"));
        request.setTitle(rs.getString("title"));
        request.setDescription(rs.getString("description"));
        request.setStatus(RequestStatus.valueOf(rs.getString("status")));
        request.setRejectionReason(rs.getString("rejection_reason"));
        request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        request.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return request;
    };

    public Optional<Request> findById(Long requestId) {
        String sql = "SELECT * FROM Eren_requests WHERE request_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, requestId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Request> findByCustomerId(Long customerId) {
        String sql = """
            SELECT * FROM Eren_requests
            WHERE customer_id = ?
            ORDER BY created_at DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, customerId);
    }

    public List<Request> findAllActive() {
        String sql = """
            SELECT * FROM Eren_requests
            WHERE status != 'REJECTED'
            ORDER BY created_at ASC
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<Request> findByStatus(RequestStatus status) {
        String sql = "SELECT * FROM Eren_requests WHERE status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    public void save(Request request) {
        String sql = """
            INSERT INTO Eren_requests
                (request_id, customer_id, title, description, status, created_at, updated_at)
            VALUES
                (Eren_seq_requests.NEXTVAL, ?, ?, ?, 'NEW', SYSTIMESTAMP, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
                request.getCustomerId(),
                request.getTitle(),
                request.getDescription()
        );
    }

    public void updateStatus(Long requestId, RequestStatus status) {
        String sql = """
            UPDATE Eren_requests
            SET status = ?, updated_at = SYSTIMESTAMP
            WHERE request_id = ?
            """;
        jdbcTemplate.update(sql, status.name(), requestId);
    }

    public void reject(Long requestId, String rejectionReason) {
        String sql = """
            UPDATE Eren_requests
            SET status = 'REJECTED', rejection_reason = ?, updated_at = SYSTIMESTAMP
            WHERE request_id = ?
            """;
        jdbcTemplate.update(sql, rejectionReason, requestId);
    }
}