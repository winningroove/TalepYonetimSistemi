// security/DataInitializer.java
package com.example.security;

import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import com.example.model.User;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM Eren_users", Integer.class);

        if (count != null && count > 0) {
            log.info("DataInitializer: Kullanıcılar zaten mevcut, atlanıyor.");
            return;
        }

        log.info("DataInitializer: Başlangıç kullanıcıları oluşturuluyor...");

        User admin = new User();
        admin.setNameSurname("Sistem Admin");
        admin.setEmail("admin@firma.com");
        admin.setRole(Role.ADMIN);
        userService.createUser(admin, "Admin1234!");
        
        User sm = new User();
        sm.setNameSurname("Ali Demir");
        sm.setEmail("ali.demir@firma.com");
        sm.setRole(Role.SCRUM_MASTER);
        userService.createUser(sm, "Sm1234!");

        User po = new User();
        po.setNameSurname("Ayşe Kaya");
        po.setEmail("ayse.kaya@firma.com");
        po.setRole(Role.PRODUCT_OWNER);
        userService.createUser(po, "Po1234!");

        User dev1 = new User();
        dev1.setNameSurname("Mehmet Çelik");
        dev1.setEmail("mehmet.celik@firma.com");
        dev1.setRole(Role.DEVELOPER);
        userService.createUser(dev1, "Dev1234!");

        User dev2 = new User();
        dev2.setNameSurname("Zeynep Arslan");
        dev2.setEmail("zeynep.arslan@firma.com");
        dev2.setRole(Role.DEVELOPER);
        userService.createUser(dev2, "Dev1234!");

        User c1 = new User();
        c1.setNameSurname("TeknoCorp Yetkili");
        c1.setEmail("yetkili@teknocorp.com");
        c1.setRole(Role.CUSTOMER);
        c1.setMusteriDegeri(MusteriDegeri.VIP);
        userService.createUser(c1, "Cust1234!");

        User c2 = new User();
        c2.setNameSurname("Global A.Ş. Temsilci");
        c2.setEmail("temsilci@globalas.com");
        c2.setRole(Role.CUSTOMER);
        c2.setMusteriDegeri(MusteriDegeri.BUYUK);
        userService.createUser(c2, "Cust1234!");

        User c3 = new User();
        c3.setNameSurname("Mehmet Öz");
        c3.setEmail("mehmet.oz@musteri.com");
        c3.setRole(Role.CUSTOMER);
        c3.setMusteriDegeri(MusteriDegeri.ORTA);
        userService.createUser(c3, "Cust1234!");

        User c4 = new User();
        c4.setNameSurname("Medya TR");
        c4.setEmail("iletisim@medyatr.com");
        c4.setRole(Role.CUSTOMER);
        c4.setMusteriDegeri(MusteriDegeri.KUCUK);
        userService.createUser(c4, "Cust1234!");

        log.info("DataInitializer: Tüm kullanıcılar oluşturuldu.");
    }
}