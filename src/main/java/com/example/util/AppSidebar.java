package com.example.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Tüm panellerin (PO / Müşteri / Geliştirici / SM / Admin) ortak sol menüsünü üretir.
 * Değişmeyen iskelet (logo, başlık, bildirim satırı, tema düğmesi, çıkış) buradadır;
 * view yalnızca alt başlığı, bildirim zilini, menü öğelerini ve profil satırını verir.
 */
public final class AppSidebar {

    private AppSidebar() {}

    /**
     * @param altBaslik    başlık altındaki panel adı (ör. "Ürün Sorumlusu Yönetim Paneli")
     * @param bildirimZili view'a özel NotificationBell bileşeni
     * @param menuButtons  {@link #menuButton(String, Runnable)} ile üretilmiş menü öğeleri
     * @param profilSatiri view'a özel profil satırı (ProfileDialog.sidebarProfileRow)
     */
    public static VerticalLayout build(String altBaslik, Component bildirimZili,
                                       List<Button> menuButtons, Component profilSatiri) {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("260px");
        sidebar.setHeightFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(false);
        sidebar.getStyle()
            .set("background-image",
                "linear-gradient(180deg, rgba(3,107,170,0.92) 0%, rgba(2,74,120,0.95) 100%)")
            .set("backdrop-filter", "blur(16px)")
            .set("-webkit-backdrop-filter", "blur(16px)")
            .set("color", "white")
            .set("flex-shrink", "0")
            .set("border-right", "1px solid rgba(255,255,255,0.08)")
            .set("box-shadow", "6px 0 30px rgba(15,23,35,0.28)");

        H3 baslik = new H3("Talep Yönetim Sistemi");
        baslik.getStyle().set("color", "white").set("margin-top", "0");

        Span alt = new Span(altBaslik);
        alt.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        HorizontalLayout bildirimSatir = new HorizontalLayout(new Span("Bildirimler"), bildirimZili);
        bildirimSatir.setAlignItems(Alignment.CENTER);
        bildirimSatir.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("margin-top", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle().set("color", "#aaaaaa").set("margin-bottom", "8px").set("margin-top", "24px");

        sidebar.add(Brand.sidebarLogo(), baslik, alt, bildirimSatir, menuBaslik);
        menuButtons.forEach(sidebar::add);
        sidebar.addAndExpand(new Div());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        sidebar.add(divider, profilSatiri, new ThemeToggle(), logoutButton());
        return sidebar;
    }

    /** Sol menüdeki tıklanabilir öğe (ortak stil + hover). */
    public static Button menuButton(String text, Runnable onClick) {
        Button btn = new Button(text, e -> onClick.run());
        btn.getStyle()
            .set("color", "white")
            .set("background", "rgba(255,255,255,0.07)")
            .set("border", "none")
            .set("border-left", "3px solid rgba(255,255,255,0.2)")
            .set("border-radius", "6px")
            .set("text-align", "left")
            .set("width", "100%")
            .set("cursor", "pointer")
            .set("padding", "10px 14px")
            .set("margin-bottom", "4px")
            .set("font-size", "13px")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.25)");
        btn.getElement().addEventListener("mouseover", e ->
            btn.getStyle().set("background", "rgba(255,255,255,0.15)").set("border-left", "3px solid rgba(255,255,255,0.9)"));
        btn.getElement().addEventListener("mouseout", e ->
            btn.getStyle().set("background", "rgba(255,255,255,0.07)").set("border-left", "3px solid rgba(255,255,255,0.2)"));
        return btn;
    }

    private static Button logoutButton() {
        Button logoutBtn = new Button("Çıkış Yap",
            e -> UI.getCurrent().getPage().setLocation("/logout"));
        logoutBtn.getStyle()
            .set("background-color", "#c0392b")
            .set("color", "white")
            .set("width", "100%")
            .set("margin-top", "12px")
            .set("cursor", "pointer");
        return logoutBtn;
    }
}
