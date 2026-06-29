// repository/RequestRepository.java
package com.example.repository;

import com.example.enums.RequestStatus;
import com.example.enums.YoneticiTakdiri;
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
    request.setRequestId(rs.getLong(1));
    request.setCustomerId(rs.getLong(2));
    request.setTitle(rs.getString(3));
    request.setDescription(rs.getString(4));
    request.setStatus(RequestStatus.valueOf(rs.getString(5)));
    request.setRejectionReason(rs.getString(6));
    String takdiri = rs.getString(7);
    request.setYoneticiTakdiri(takdiri != null ? YoneticiTakdiri.valueOf(takdiri) : YoneticiTakdiri.YOK);
    request.setCreatedAt(rs.getTimestamp(8).toLocalDateTime());
    request.setUpdatedAt(rs.getTimestamp(9).toLocalDateTime());
    return request;
};

    public Optional<Request> findById(Long requestId) {
        String sql = "SELECT request_id, customer_id, title, description, status, rejection_reason, yonetici_takdiri, created_at, updated_at FROM Eren_requests WHERE request_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, requestId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Request> findByCustomerId(Long customerId) {
        String sql = """
            SELECT request_id, customer_id, title, description, status, rejection_reason, yonetici_takdiri, created_at, updated_at FROM Eren_requests
            WHERE customer_id = ?
            ORDER BY created_at DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, customerId);
    }

    public List<Request> findAllActive() {
        String sql = """
            SELECT request_id, customer_id, title, description, status, rejection_reason, yonetici_takdiri, created_at, updated_at FROM Eren_requests
            WHERE status != 'REJECTED'
            ORDER BY created_at ASC
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<Request> findByStatus(RequestStatus status) {
        String sql = "SELECT request_id, customer_id, title, description, status, rejection_reason, yonetici_takdiri, created_at, updated_at FROM Eren_requests WHERE status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    public Optional<Long> findLastRequestIdByCustomer(Long customerId) {
    String sql = """
        SELECT request_id FROM Eren_requests
        WHERE customer_id = ?
        ORDER BY created_at DESC
        FETCH FIRST 1 ROWS ONLY
        """;
    try {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(sql, Long.class, customerId));
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();
    }
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

    public void updateYoneticiTakdiri(Long requestId, YoneticiTakdiri takdir) {
        String sql = """
            UPDATE Eren_requests
            SET yonetici_takdiri = ?, updated_at = SYSTIMESTAMP
            WHERE request_id = ?
            """;
        jdbcTemplate.update(sql, takdir.name(), requestId);
    }

    
    public CredibilityStats getCredibilityStats(Long customerId) {
        String sql = """
            SELECT
                COUNT(*) AS total,
                COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected,
                COUNT(CASE WHEN status IN
                    ('PRIORITIZED', 'BACKLOG', 'IN_PROGRESS', 'TESTING', 'DONE')
                    THEN 1 END) AS approved
            FROM Eren_requests
            WHERE customer_id = ?
            """;
        return jdbcTemplate.queryForObject(sql,
            (rs, rowNum) -> new CredibilityStats(
                rs.getInt("total"),
                rs.getInt("rejected"),
                rs.getInt("approved")),
            customerId);
    }

    public record CredibilityStats(int total, int rejected, int approved) {}
}