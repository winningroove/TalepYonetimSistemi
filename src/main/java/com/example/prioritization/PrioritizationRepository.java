package com.example.prioritization;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.IsTipi;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PrioritizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrioritizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Prioritization> rowMapper = (rs, rowNum) -> {
        Prioritization p = new Prioritization();
        p.setPriorityId(rs.getLong("priority_id"));
        p.setRequestId(rs.getLong("request_id"));
        p.setIsEtkisi(rs.getInt("is_etkisi"));
        p.setAciliyet(rs.getInt("aciliyet"));
        p.setMusteriDegeriPuan(rs.getInt("musteri_degeri_puan"));
        p.setIsTipi(IsTipi.valueOf(rs.getString("is_tipi")));
        p.setIsTipiPuan(rs.getInt("is_tipi_puan"));
        p.setBeklemeSuresiPuan(rs.getInt("bekleme_suresi_puan"));
        p.setGelistiriciMudahalesi(rs.getString("gelistirici_mudahalesi") != null
                ? GelistiriciMudahalesi.valueOf(rs.getString("gelistirici_mudahalesi"))
                : null);
        p.setBazSkor(rs.getDouble("baz_skor"));
        p.setPriorityScore(rs.getInt("priority_score"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return p;
    };

    public Optional<Prioritization> findByRequestId(Long requestId) {
        String sql = "SELECT * FROM Eren_prioritizations WHERE request_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, requestId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Prioritization> findAllOrderByScore() {
        String sql = "SELECT * FROM Eren_prioritizations ORDER BY priority_score DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void save(Prioritization p) {
        String sql = """
            INSERT INTO Eren_prioritizations
                (priority_id, request_id, is_etkisi, aciliyet, musteri_degeri_puan,
                 is_tipi, is_tipi_puan, bekleme_suresi_puan,
                 gelistirici_mudahalesi, baz_skor, priority_score, created_at, updated_at)
            VALUES
                (Eren_seq_prioritizations.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
            """;
        jdbcTemplate.update(sql,
                p.getRequestId(),
                p.getIsEtkisi(),
                p.getAciliyet(),
                p.getMusteriDegeriPuan(),
                p.getIsTipi().name(),
                p.getIsTipiPuan(),
                p.getBeklemeSuresiPuan(),
                p.getGelistiriciMudahalesi() != null ? p.getGelistiriciMudahalesi().name() : null,
                p.getBazSkor(),
                p.getPriorityScore()
        );
    }

    public void update(Prioritization p) {
        String sql = """
            UPDATE Eren_prioritizations
            SET is_etkisi = ?, aciliyet = ?, musteri_degeri_puan = ?,
                is_tipi = ?, is_tipi_puan = ?, bekleme_suresi_puan = ?,
                gelistirici_mudahalesi = ?,
                baz_skor = ?, priority_score = ?, updated_at = SYSTIMESTAMP
            WHERE request_id = ?
            """;
        jdbcTemplate.update(sql,
                p.getIsEtkisi(),
                p.getAciliyet(),
                p.getMusteriDegeriPuan(),
                p.getIsTipi().name(),
                p.getIsTipiPuan(),
                p.getBeklemeSuresiPuan(),
                p.getGelistiriciMudahalesi() != null ? p.getGelistiriciMudahalesi().name() : null,
                p.getBazSkor(),
                p.getPriorityScore(),
                p.getRequestId()
        );
    }
}
