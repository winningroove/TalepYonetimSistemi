package com.example.company;

import com.example.enums.MusteriDegeri;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CompanyRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Company> rowMapper = (rs, rowNum) -> {
        Company company = new Company();
        company.setCompanyId(rs.getLong("company_id"));
        company.setName(rs.getString("name"));
        company.setMusteriDegeri(rs.getString("musteri_degeri") != null
                ? MusteriDegeri.valueOf(rs.getString("musteri_degeri"))
                : null);
        company.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        company.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return company;
    };

    public List<Company> findAll() {
        String sql = "SELECT * FROM Eren_companies ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Company> findById(Long companyId) {
        String sql = "SELECT * FROM Eren_companies WHERE company_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, companyId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Company> findByName(String name) {
        String sql = "SELECT * FROM Eren_companies WHERE LOWER(name) = LOWER(?)";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(Company company) {
        String sql = """
            INSERT INTO Eren_companies (company_id, name, musteri_degeri, created_at, updated_at)
            VALUES (Eren_seq_companies.NEXTVAL, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
                company.getName(),
                company.getMusteriDegeri() != null ? company.getMusteriDegeri().name() : null);
    }

    public void delete(Long companyId) {
        jdbcTemplate.update("DELETE FROM Eren_companies WHERE company_id = ?", companyId);
    }

    public void update(Company company) {
        String sql = """
            UPDATE Eren_companies
            SET name = ?, musteri_degeri = ?, updated_at = SYSTIMESTAMP
            WHERE company_id = ?
            """;
        jdbcTemplate.update(sql,
                company.getName(),
                company.getMusteriDegeri() != null ? company.getMusteriDegeri().name() : null,
                company.getCompanyId());
    }
}
