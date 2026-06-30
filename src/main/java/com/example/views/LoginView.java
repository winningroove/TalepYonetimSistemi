package com.example.views;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Giriş Yap")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        getStyle()
            .set("background-color", "#1B2A3B")
            .set("background-image",
                "radial-gradient(ellipse at 60% 40%, #22364a 0%, #1B2A3B 70%)")
            .set("min-height", "100vh");

        // Üst marka alanı
        H1 marka = new H1("Talep Yönetim Sistemi");
        marka.getStyle()
            .set("color", "white")
            .set("font-size", "26px")
            .set("font-weight", "700")
            .set("margin-bottom", "4px")
            .set("text-align", "center");

        Paragraph altYazi = new Paragraph("Müşteri taleplerini yönetin, önceliklendirin ve iş akışına alın.");
        altYazi.getStyle()
            .set("color", "#9aabbf")
            .set("font-size", "13px")
            .set("text-align", "center")
            .set("margin-top", "0")
            .set("margin-bottom", "32px");

        // Giriş kartı
        VerticalLayout kart = new VerticalLayout();
        kart.setPadding(false);
        kart.setSpacing(false);
        kart.setAlignItems(FlexComponent.Alignment.CENTER);
        kart.getStyle()
            .set("background-color", "#ffffff")
            .set("border-radius", "16px")
            .set("padding", "40px 48px")
            .set("box-shadow", "0 8px 40px rgba(0,0,0,0.35)")
            .set("width", "420px")
            .set("max-width", "92vw");

        H2 girisBaslik = new H2("Hoş Geldiniz");
        girisBaslik.getStyle()
            .set("color", "#1B2A3B")
            .set("font-size", "22px")
            .set("margin-top", "0")
            .set("margin-bottom", "4px");

        Paragraph girisAlt = new Paragraph("Devam etmek için giriş yapın.");
        girisAlt.getStyle()
            .set("color", "#888888")
            .set("font-size", "13px")
            .set("margin-top", "0")
            .set("margin-bottom", "20px");

        loginForm.setAction("login");
        loginForm.getStyle().set("width", "100%");
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setI18n(turkceI18n());

        kart.add(girisBaslik, girisAlt, loginForm);

        // Alt bilgi notu
        Span dipNot = new Span("© 2025 Talep Yönetim Sistemi");
        dipNot.getStyle()
            .set("color", "#556677")
            .set("font-size", "11px")
            .set("margin-top", "32px");

        add(marka, altYazi, kart, dipNot);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Spring Security giriş başarısız olunca login?error'a yönlendirir.
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }

    private LoginI18n turkceI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Giriş");
        form.setUsername("E-posta");
        form.setPassword("Şifre");
        form.setSubmit("Giriş Yap");
        i18n.setForm(form);

        LoginI18n.ErrorMessage hata = new LoginI18n.ErrorMessage();
        hata.setTitle("Giriş başarısız");
        hata.setMessage("E-posta veya şifre hatalı. Lütfen tekrar deneyin.");
        i18n.setErrorMessage(hata);

        return i18n;
    }
}
