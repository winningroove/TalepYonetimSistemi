package com.example.user;

import com.example.company.CompanyService;
import com.example.enums.Role;
import com.example.request.RequestRepository;
import com.example.workflow.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyService companyService;
    private final RequestRepository requestRepository;
    private final WorkflowRepository workflowRepository;

    /** Ad/şirket çözümlemesi grid'lerde çok sık çağrıldığı için id->User önbelleği.
     *  Tüm yazma işlemleri önbelleği temizler (uygulama içi tek yazma yolu servislerdir). */
    private final Map<Long, User> idCache = new ConcurrentHashMap<>();

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long userId) {
        if (userId == null) return Optional.empty();
        User cached = idCache.get(userId);
        if (cached != null) return Optional.of(cached);
        Optional<User> result = userRepository.findById(userId);
        result.ifPresent(u -> idCache.put(userId, u));
        return result;
    }

    public List<User> findAllActive() {
        return userRepository.findAllActive();
    }

    public void createUser(User user, String rawPassword) {
        if (user.getNameSurname() == null || user.getNameSurname().isBlank()) {
            throw new IllegalArgumentException("Ad soyad alanı boş bırakılamaz.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-posta alanı boş bırakılamaz.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Şifre alanı boş bırakılamaz.");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Rol seçimi zorunludur.");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kullanımda.");
        }

        if (user.getRole() == Role.CUSTOMER && user.getCompanyId() == null) {
            throw new IllegalArgumentException("Müşteri için şirket seçimi zorunludur.");
        }

        if (user.getRole() != Role.CUSTOMER && user.getCompanyId() != null) {
            throw new IllegalArgumentException("Şirket yalnızca CUSTOMER rolü için geçerlidir.");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);

        userRepository.save(user);
        idCache.clear();
    }

    public void updateUser(User user) {
        if (user.getNameSurname() == null || user.getNameSurname().isBlank()) {
            throw new IllegalArgumentException("Ad soyad alanı boş bırakılamaz.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-posta alanı boş bırakılamaz.");
        }
        userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        userRepository.update(user);
        idCache.clear();
    }

    public void setActive(Long userId, boolean isActive) {
        userRepository.setActive(userId, isActive);
        idCache.clear();
    }

    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (requestRepository.countByCustomerId(userId) > 0) {
            throw new IllegalStateException(
                "Bu kullanıcının talepleri olduğu için silinemez. Bunun yerine pasife alın.");
        }
        if (workflowRepository.countByDeveloperId(userId) > 0) {
            throw new IllegalStateException(
                "Bu kullanıcıya atanmış görevler olduğu için silinemez. Bunun yerine pasife alın.");
        }

        userRepository.delete(userId);
        idCache.clear();
    }

    /** Müşterinin değer puanı artık bağlı olduğu ŞİRKETTEN miras alınır. */
    public int getMusteriDegeriPuan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        return companyService.getMusteriDegeriPuan(user.getCompanyId());
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
