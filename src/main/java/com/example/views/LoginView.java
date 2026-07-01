package com.example.views;

import com.example.enums.Role;
import com.example.notification.NotificationService;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
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
    private final UserService userService;
    private final NotificationService notificationService;

    public LoginView(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        getStyle()
            .set("background-color", "#1B2A3B")
            .set("background-image",
                "radial-gradient(circle at 22% 18%, rgba(3,107,170,0.38) 0%, transparent 46%), "
                + "radial-gradient(circle at 82% 85%, rgba(3,107,170,0.20) 0%, transparent 42%), "
                + "linear-gradient(135deg, #1B2A3B 0%, #16222f 100%)")
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
            .set("background-color", "rgba(255,255,255,0.92)")
            .set("backdrop-filter", "blur(16px) saturate(1.2)")
            .set("-webkit-backdrop-filter", "blur(16px) saturate(1.2)")
            .set("border", "1px solid rgba(255,255,255,0.6)")
            .set("border-radius", "20px")
            .set("padding", "40px 48px")
            .set("box-shadow", "0 24px 70px rgba(0,0,0,0.45)")
            .set("width", "420px")
            .set("max-width", "92vw")
            .set("animation", "uiFadeIn .45s ease");

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

        Button sifremiUnuttumBtn = new Button("Şifremi Unuttum", e -> sifremiUnuttumDialogAc());
        sifremiUnuttumBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        sifremiUnuttumBtn.getStyle().set("margin-top", "8px");

        kart.add(girisBaslik, girisAlt, loginForm, sifremiUnuttumBtn);

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

    /** E-postasını soran dialog; gönderilince tüm aktif Admin'lere bildirim gider. */
    private void sifremiUnuttumDialogAc() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Şifremi Unuttum");
        dialog.setWidth("400px");

        Paragraph aciklama = new Paragraph(
            "E-posta adresinizi girin; sistem yöneticisine bildirim gönderilecek ve "
            + "sizinle iletişime geçilecektir.");
        aciklama.getStyle().set("font-size", "13px").set("color", "#555");

        EmailField emailField = new EmailField("E-posta");
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);

        Button gonderBtn = new Button("Gönder", e -> {
            String email = emailField.getValue();
            if (email == null || email.isBlank()) {
                Notification.show("E-posta adresi zorunludur.", 3000, Notification.Position.MIDDLE);
                return;
            }
            String kullaniciAdi = userService.findByEmail(email.trim())
                .map(User::getNameSurname)
                .orElse(null);
            String mesaj = kullaniciAdi != null
                ? kullaniciAdi + " (" + email.trim() + ") şifresini unuttu."
                : "Bilinmeyen e-posta (" + email.trim() + ") için şifremi unuttum bildirimi.";

            for (User admin : userService.findActiveByRole(Role.ADMIN)) {
                notificationService.notify(admin.getUserId(), mesaj, null);
            }

            Notification.show("İsteğiniz sistem yöneticisine iletildi.",
                3000, Notification.Position.TOP_CENTER);
            dialog.close();
        });
        gonderBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        dialog.add(new VerticalLayout(aciklama, emailField));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), gonderBtn);
        dialog.open();
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
