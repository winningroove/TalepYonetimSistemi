// enums/GelistiriciMudahalesi.java
package com.example.enums;

public enum GelistiriciMudahalesi {
    QUICK_WIN(10),
    DUSUK(5),
    ORTA(0),
    YUKSEK(-5),
    COK_YUKSEK(-10);

    private final int duzeltici;

    GelistiriciMudahalesi(int duzeltici) {
        this.duzeltici = duzeltici;
    }

    public int getDuzeltici() {
        return duzeltici;
    }
}