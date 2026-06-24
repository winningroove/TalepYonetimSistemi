package com.example.enums;

public enum YoneticiMudahalesi {
    IPTAL(0.0),
    NOTR(1.0),
    YONETICI_ONAYLI(1.2),
    SOZLESME_ZORUNLU(1.5);

    private final double carpan;

    YoneticiMudahalesi(double carpan) {
        this.carpan = carpan;
    }

    public double getCarpan() {
        return carpan;
    }
}