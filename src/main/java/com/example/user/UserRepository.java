package com.example.user;

import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setNameSurname(rs.getString("name_surname"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setMusteriDegeri(rs.getString("musteri_degeri") != null
                ? MusteriDegeri.valueOf(rs.getString("musteri_degeri"))
                : null);
        user.setActive(rs.getInt("is_active") == 1);
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return user;
    };

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM Eren_users WHERE email = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM Eren_users WHERE user_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM Eren_users ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<User> findAllActive() {
        String sql = "SELECT * FROM Eren_users WHERE is_active = 1 ORDER BY name_surname";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void save(User user) {
        String sql = """
            INSERT INTO Eren_users
                (user_id, name_surname, email, password, role, musteri_degeri, is_active, created_at, updated_at)
            VALUES
                (Eren_seq_users.NEXTVAL, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
                user.getNameSurname(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.getMusteriDegeri() != null ? user.getMusteriDegeri().name() : null,
                user.isActive() ? 1 : 0
        );
    }

    public void update(User user) {
        String sql = """
            UPDATE Eren_users
            SET name_surname = ?, email = ?, musteri_degeri = ?,
                is_active = ?, updated_at = SYSTIMESTAMP
            WHERE user_id = ?
            """;
        jdbcTemplate.update(sql,
                user.getNameSurname(),
                user.getEmail(),
                user.getMusteriDegeri() != null ? user.getMusteriDegeri().name() : null,
                user.isActive() ? 1 : 0,
                user.getUserId()
        );
    }

    public void updatePassword(Long userId, String hashedPassword) {
        String sql = "UPDATE Eren_users SET password = ?, updated_at = SYSTIMESTAMP WHERE user_id = ?";
        jdbcTemplate.update(sql, hashedPassword, userId);
    }

    public void setActive(Long userId, boolean isActive) {
        String sql = "UPDATE Eren_users SET is_active = ?, updated_at = SYSTIMESTAMP WHERE user_id = ?";
        jdbcTemplate.update(sql, isActive ? 1 : 0, userId);
    }
}
