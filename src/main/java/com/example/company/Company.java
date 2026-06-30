package com.example.company;

import com.example.enums.MusteriDegeri;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Company {
    private Long companyId;
    private String name;
    private MusteriDegeri musteriDegeri;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
