package com.example.prioritization;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Eren_prioritization_config (anahtar-değer) tablosuna erişim.
 * Yalnızca admin tarafından değiştirilen (varsayılandan sapan) ayarlar saklanır.
 */
@Repository
public class PrioritizationConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrioritizationConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Tablodaki tüm override değerlerini döndürür. */
    public Map<String, Double> findAll() {
        Map<String, Double> map = new LinkedHashMap<>();
        jdbcTemplate.query(
            "SELECT config_key, config_value FROM Eren_prioritization_config",
            rs -> { map.put(rs.getString("config_key"), rs.getDouble("config_value")); });
        return map;
    }

    /** Anahtar varsa günceller, yoksa ekler. */
    public void upsert(String key, double value) {
        int updated = jdbcTemplate.update(
            "UPDATE Eren_prioritization_config SET config_value = ?, updated_at = SYSTIMESTAMP WHERE config_key = ?",
            value, key);
        if (updated == 0) {
            jdbcTemplate.update(
                "INSERT INTO Eren_prioritization_config (config_key, config_value) VALUES (?, ?)",
                key, value);
        }
    }

    /** Tüm override'ları siler (varsayılanlara dönüş). */
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM Eren_prioritization_config");
    }
}
