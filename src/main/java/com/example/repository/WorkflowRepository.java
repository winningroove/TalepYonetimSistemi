// repository/WorkflowRepository.java
package com.example.repository;

import com.example.enums.WorkflowStatus;
import com.example.model.Workflow;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WorkflowRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Workflow> rowMapper = (rs, rowNum) -> {
        Workflow workflow = new Workflow();
        workflow.setTaskId(rs.getLong("task_id"));
        workflow.setRequestId(rs.getLong("request_id"));
        workflow.setDeveloperId(rs.getObject("developer_id") != null
                ? rs.getLong("developer_id")
                : null);
        workflow.setWorkflowStatus(WorkflowStatus.valueOf(rs.getString("workflow_status")));
        workflow.setVersion(rs.getInt("version"));
        workflow.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        workflow.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return workflow;
    };

    public Optional<Workflow> findByRequestId(Long requestId) {
        String sql = "SELECT * FROM Eren_workflows WHERE request_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, requestId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Workflow> findByDeveloperId(Long developerId) {
        String sql = """
            SELECT * FROM Eren_workflows
            WHERE developer_id = ? AND workflow_status != 'DONE'
            ORDER BY created_at ASC
            """;
        return jdbcTemplate.query(sql, rowMapper, developerId);
    }

    public Optional<Workflow> findByTaskId(Long taskId) {
    String sql = "SELECT * FROM Eren_workflows WHERE task_id = ?";
    try {
        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, taskId));
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();
    }
}

    public List<Workflow> findUnassigned() {
        String sql = """
            SELECT * FROM Eren_workflows
            WHERE developer_id IS NULL AND workflow_status != 'DONE'
            ORDER BY created_at ASC
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void save(Workflow workflow) {
        String sql = """
            INSERT INTO Eren_workflows
                (task_id, request_id, developer_id, workflow_status, version, created_at, updated_at)
            VALUES
                (Eren_seq_workflows.NEXTVAL, ?, NULL, 'BACKLOG', 0, SYSTIMESTAMP, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql, workflow.getRequestId());
    }

    public int updateStatus(Long taskId, WorkflowStatus newStatus, int currentVersion) {
        String sql = """
            UPDATE Eren_workflows
            SET workflow_status = ?, version = version + 1, updated_at = SYSTIMESTAMP
            WHERE task_id = ? AND version = ?
            """;
        return jdbcTemplate.update(sql, newStatus.name(), taskId, currentVersion);
    }

    public int assignDeveloper(Long taskId, Long developerId, int currentVersion) {
        String sql = """
            UPDATE Eren_workflows
            SET developer_id = ?, version = version + 1, updated_at = SYSTIMESTAMP
            WHERE task_id = ? AND version = ? AND developer_id IS NULL
            """;
        return jdbcTemplate.update(sql, developerId, taskId, currentVersion);
    }

    public List<Workflow> findDoneByDeveloperId(Long developerId) {
    String sql = """
        SELECT * FROM Eren_workflows
        WHERE developer_id = ? AND workflow_status = 'DONE'
        ORDER BY updated_at DESC
        """;
    return jdbcTemplate.query(sql, rowMapper, developerId);
}
}