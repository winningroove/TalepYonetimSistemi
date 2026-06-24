// model/User.java
package com.example.model;

import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;
    private String nameSurname;
    private String email;
    private String password;
    private Role role;
    private MusteriDegeri musteriDegeri;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}