package com.example.company;

import com.example.enums.MusteriDegeri;
import com.example.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Optional<Company> findById(Long companyId) {
        return companyRepository.findById(companyId);
    }

    /** Şirket adını çözer; bulunamazsa "-" döner. Grid/etiket gösterimleri için. */
    public String getName(Long companyId) {
        if (companyId == null) return "-";
        return companyRepository.findById(companyId)
                .map(Company::getName)
                .orElse("-");
    }

    /** Şirketin değer puanı; şirket veya değeri yoksa 1 (en düşük). Önceliklendirme için. */
    public int getMusteriDegeriPuan(Long companyId) {
        if (companyId == null) return 1;
        return companyRepository.findById(companyId)
                .map(Company::getMusteriDegeri)
                .map(MusteriDegeri::getPuan)
                .orElse(1);
    }

    public Company createCompany(String name, MusteriDegeri musteriDegeri) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Şirket adı zorunludur.");
        }
        if (musteriDegeri == null) {
            throw new IllegalArgumentException("Şirket değeri zorunludur.");
        }
        String trimmed = name.trim();
        if (companyRepository.findByName(trimmed).isPresent()) {
            throw new IllegalArgumentException("Bu şirket zaten kayıtlı.");
        }
        Company company = new Company();
        company.setName(trimmed);
        company.setMusteriDegeri(musteriDegeri);
        companyRepository.save(company);
        return companyRepository.findByName(trimmed)
                .orElseThrow(() -> new IllegalStateException("Şirket kaydedilemedi."));
    }

    public void updateCompany(Long companyId, String name, MusteriDegeri musteriDegeri) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Şirket adı zorunludur.");
        }
        if (musteriDegeri == null) {
            throw new IllegalArgumentException("Şirket değeri zorunludur.");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Şirket bulunamadı."));

        String trimmed = name.trim();
        // Aynı ada sahip BAŞKA bir şirket varsa engelle
        companyRepository.findByName(trimmed)
                .filter(c -> !c.getCompanyId().equals(companyId))
                .ifPresent(c -> { throw new IllegalArgumentException("Bu şirket adı zaten kullanımda."); });

        company.setName(trimmed);
        company.setMusteriDegeri(musteriDegeri);
        companyRepository.update(company);
    }

    public void deleteCompany(Long companyId) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Şirket bulunamadı."));

        if (userRepository.countByCompanyId(companyId) > 0) {
            throw new IllegalStateException(
                "Bu şirkete bağlı müşteriler olduğu için silinemez. Önce müşterileri başka şirkete taşıyın veya silin.");
        }

        companyRepository.delete(companyId);
    }
}
