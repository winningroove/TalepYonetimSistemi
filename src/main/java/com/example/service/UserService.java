// service/UserService.java
package com.example.service;

import com.example.enums.Role;
import com.example.model.User;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public List<User> findAllActive() {
        return userRepository.findAllActive();
    }

    public void createUser(User user, String rawPassword) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kullanımda.");
        }

        if (user.getRole() == Role.CUSTOMER && user.getMusteriDegeri() == null) {
            throw new IllegalArgumentException("Müşteri değeri zorunludur.");
        }

        if (user.getRole() != Role.CUSTOMER && user.getMusteriDegeri() != null) {
            throw new IllegalArgumentException("Müşteri değeri yalnızca CUSTOMER rolü için geçerlidir.");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);

        userRepository.save(user);
    }

    public void updateUser(User user) {
        userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        userRepository.update(user);
    }

    public void setActive(Long userId, boolean isActive) {
        userRepository.setActive(userId, isActive);
    }

    public int getMusteriDegeriPuan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (user.getMusteriDegeri() == null) {
            return 1;
        }

        return user.getMusteriDegeri().getPuan();
    }
}