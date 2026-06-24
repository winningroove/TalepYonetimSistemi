// enums/MusteriDegeri.java
package com.example.enums;

public enum MusteriDegeri {
    VIP(5),
    BUYUK(4),
    ORTA(3),
    KUCUK(2),
    IC_KULLANICI(1);

    private final int puan;

    MusteriDegeri(int puan) {
        this.puan = puan;
    }

    public int getPuan() {
        return puan;
    }
}