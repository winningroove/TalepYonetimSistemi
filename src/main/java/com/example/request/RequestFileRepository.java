package com.example.request;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RequestFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequestFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RequestFile> rowMapper = (rs, rowNum) -> {
        RequestFile file = new RequestFile();
        file.setFileId(rs.getLong("file_id"));
        file.setRequestId(rs.getLong("request_id"));
        file.setFileName(rs.getString("file_name"));
        file.setFileData(rs.getBytes("file_data"));
        file.setFileSize(rs.getLong("file_size"));
        file.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return file;
    };

    public void save(RequestFile file) {
        String sql = """
            INSERT INTO Eren_request_files
                (file_id, request_id, file_name, file_data, file_size, created_at)
            VALUES
                (Eren_seq_req_files.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
            file.getRequestId(),
            file.getFileName(),
            file.getFileData(),
            file.getFileSize()
        );
    }

    public List<RequestFile> findByRequestId(Long requestId) {
        String sql = "SELECT * FROM Eren_request_files WHERE request_id = ?";
        return jdbcTemplate.query(sql, rowMapper, requestId);
    }

    public Optional<RequestFile> findById(Long fileId) {
        String sql = "SELECT * FROM Eren_request_files WHERE file_id = ?";
        try {
            return Optional.ofNullable(
                jdbcTemplate.queryForObject(sql, rowMapper, fileId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
