// model/Prioritization.java
package com.example.model;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.IsTipi;
import com.example.enums.YoneticiMudahalesi;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Prioritization {
    private Long priorityId;
    private Long requestId;
    private int isEtkisi;
    private int aciliyet;
    private int musteriDegeriPuan;
    private IsTipi isTipi;
    private int isTimiPuan;
    private int beklemeSuresiPuan;
    private YoneticiMudahalesi yoneticiMudahalesi;
    private GelistiriciMudahalesi gelistiriciMudahalesi;
    private double bazSkor;
    private int priorityScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}