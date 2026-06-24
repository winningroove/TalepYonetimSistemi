// enums/IsTipi.java
package com.example.enums;

public enum IsTipi {
    GUVENLIK_ACIGI(5),
    KRITIK_BUG(5),
    BUG(4),
    PERFORMANS(3),
    ENTEGRASYON(3),
    FEATURE_REQUEST(2),
    ENHANCEMENT(2),
    DOKUMANTASYON(1);

    private final int puan;

    IsTipi(int puan) {
        this.puan = puan;
    }

    public int getPuan() {
        return puan;
    }
}