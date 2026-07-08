package com.example.util;

import com.vaadin.flow.component.html.Image;


public final class Brand {

    public static final String LOGO_PATH = "/images/logo.png";

    private Brand() {}

    /** Sidebar'ın sol üstü için küçük logo. */
    public static Image sidebarLogo() {
        Image img = new Image(LOGO_PATH, "Şirket Logosu");
        img.setHeight("42px");
        img.getStyle()
            .set("object-fit", "contain")
            .set("max-width", "100%")
            .set("margin-bottom", "10px");
        return img;
    }

    /** Login ekranı için büyük, ortalanmış logo. */
    public static Image loginLogo() {
        Image img = new Image(LOGO_PATH, "Şirket Logosu");
        img.setHeight("72px");
        img.getStyle()
            .set("object-fit", "contain")
            .set("max-width", "70%")
            .set("display", "block")
            .set("margin", "0 auto 18px");
        return img;
    }
}
