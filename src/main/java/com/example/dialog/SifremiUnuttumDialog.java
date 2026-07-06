package com.example.dialog;

import com.example.enums.Role;
import com.example.notification.NotificationService;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;

/** E-posta soran pencere; gönderilince tüm aktif Admin'lere bildirim gider. */
public final class SifremiUnuttumDialog {

    private SifremiUnuttumDialog() {}

    public static void open(UserService userService, NotificationService notificationService) {
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
}
