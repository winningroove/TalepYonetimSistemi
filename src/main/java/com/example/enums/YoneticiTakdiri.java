// enums/YoneticiTakdiri.java
package com.example.enums;


public enum YoneticiTakdiri {
    YOK(0),         // Normal / Takdir yok
    ONEMLI(5),      // Önemli / Birim hedefi
    STRATEJIK(10),  // Stratejik / Şirket hedefi
    KRITIK(15);     // Kritik / Acil müdahale

    private final int puan;

    YoneticiTakdiri(int puan) {
        this.puan = puan;
    }

    public int getPuan() {
        return puan;
    }
}
