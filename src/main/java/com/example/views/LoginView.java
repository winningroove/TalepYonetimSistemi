package com.example.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Giriş Yap")
@AnonymousAllowed
public class LoginView extends HorizontalLayout {

    public LoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildLeftPanel(), buildRightPanel());
    }

    private VerticalLayout buildLeftPanel() {
        VerticalLayout left = new VerticalLayout();
        left.setWidth("45%");
        left.setHeightFull();
        left.setPadding(true);
        left.setSpacing(false);
        left.setJustifyContentMode(JustifyContentMode.CENTER);
        left.getStyle()
            .set("background-color", "#1B2A3B")
            .set("color", "white")
            .set("padding", "48px");

        H1 baslik = new H1("Talep Yönetim Sistemi");
        baslik.getStyle()
            .set("color", "white")
            .set("font-size", "28px")
            .set("margin-bottom", "8px");

        Paragraph aciklama = new Paragraph(
            "Müşteri taleplerini yönetin, önceliklendirin ve iş akışına alın.");
        aciklama.getStyle()
            .set("color", "#aaaaaa")
            .set("font-size", "14px")
            .set("margin-bottom", "48px");

        H4 ozellikBaslik = new H4("Sistem Özellikleri");
        ozellikBaslik.getStyle().set("color", "#cccccc").set("margin-bottom", "16px");

        left.add(baslik, aciklama, ozellikBaslik);
        left.add(ozellikSatir("👤", "Müşteri", "Talep oluşturun ve takip edin"));
        left.add(ozellikSatir("📋", "Ürün Sorumlusu", "Talepleri önceliklendirin"));
        left.add(ozellikSatir("💻", "Geliştirici", "Görevleri üstlenin ve tamamlayın"));
        left.add(ozellikSatir("⚙️", "Admin", "Kullanıcıları yönetin"));

        return left;
    }

    private HorizontalLayout ozellikSatir(String icon, String baslik, String aciklama) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("margin-bottom", "16px");

        Span iconSpan = new Span(icon);
        iconSpan.getStyle().set("font-size", "24px");

        VerticalLayout text = new VerticalLayout();
        text.setPadding(false);
        text.setSpacing(false);

        Span baslikSpan = new Span(baslik);
        baslikSpan.getStyle().set("color", "white").set("font-weight", "bold").set("font-size", "14px");

        Span aciklamaSpan = new Span(aciklama);
        aciklamaSpan.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        text.add(baslikSpan, aciklamaSpan);
        row.add(iconSpan, text);
        return row;
    }

    private VerticalLayout buildRightPanel() {
        VerticalLayout right = new VerticalLayout();
        right.setWidth("55%");
        right.setHeightFull();
        right.setPadding(true);
        right.setSpacing(false);
        right.setAlignItems(Alignment.CENTER);
        right.setJustifyContentMode(JustifyContentMode.CENTER);
        right.getStyle().set("background-color", "#f5f7fa");

        H2 girisBaslik = new H2("Hoş Geldiniz");
        girisBaslik.getStyle()
            .set("color", "#1B2A3B")
            .set("margin-bottom", "4px");

        Paragraph altYazi = new Paragraph("Devam etmek için giriş yapın.");
        altYazi.getStyle()
            .set("color", "#888888")
            .set("margin-bottom", "24px");

        LoginForm loginForm = new LoginForm();
        loginForm.setAction("login");
        loginForm.getStyle().set("width", "360px");

        right.add(girisBaslik, altYazi, loginForm);
        return right;
    }
}