package com.example.dialog;

import com.example.company.Company;
import com.example.company.CompanyService;
import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Function;

/** Kullanıcı ekleme / düzenleme penceresi (Admin paneli). */
public final class KullaniciDialog {

    private KullaniciDialog() {}

    public static void yeni(CompanyService companyService, UserService userService, Runnable onSuccess,
                            Function<Role, String> rolLabel, Function<MusteriDegeri, String> degerLabel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yeni Kullanıcı Ekle");
        dialog.setWidth("420px");

        TextField adSoyadField = new TextField("Ad Soyad");
        adSoyadField.setWidthFull();

        EmailField emailField = new EmailField("E-posta");
        emailField.setWidthFull();

        PasswordField sifreField = new PasswordField("Şifre");
        sifreField.setWidthFull();

        ComboBox<Role> rolBox = new ComboBox<>("Rol");
        rolBox.setItems(Role.values());
        rolBox.setItemLabelGenerator(rolLabel::apply);
        rolBox.setWidthFull();

        ComboBox<Company> sirketBox = sirketBox(companyService, degerLabel);
        sirketBox.setVisible(false);

        rolBox.addValueChangeListener(e -> {
            boolean isCustomer = e.getValue() != null && e.getValue() == Role.CUSTOMER;
            sirketBox.setVisible(isCustomer);
            if (!isCustomer) {
                sirketBox.clear();
            }
        });

        Button kaydetBtn = kaydetButonu(() -> {
            User user = new User();
            user.setNameSurname(adSoyadField.getValue());
            user.setEmail(emailField.getValue());
            user.setRole(rolBox.getValue());
            user.setCompanyId(sirketBox.isVisible() && sirketBox.getValue() != null
                ? sirketBox.getValue().getCompanyId() : null);

            userService.createUser(user, sifreField.getValue());
            Notification.show("Kullanıcı oluşturuldu.", 3000, Notification.Position.TOP_CENTER);
        }, dialog, onSuccess);

        dialog.add(new VerticalLayout(adSoyadField, emailField, sifreField, rolBox, sirketBox));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), kaydetBtn);
        dialog.open();
    }

    public static void duzenle(User user, CompanyService companyService, UserService userService,
                               Runnable onSuccess, Function<Role, String> rolLabel,
                               Function<MusteriDegeri, String> degerLabel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kullanıcı Düzenle — " + user.getNameSurname());
        dialog.setWidth("420px");

        TextField adSoyadField = new TextField("Ad Soyad");
        adSoyadField.setValue(user.getNameSurname());
        adSoyadField.setWidthFull();

        EmailField emailField = new EmailField("E-posta");
        emailField.setValue(user.getEmail());
        emailField.setWidthFull();

        Span rolBilgi = new Span("Rol: " + rolLabel.apply(user.getRole()));
        rolBilgi.getStyle().set("color", "#666").set("font-size", "13px");

        ComboBox<Company> sirketBox = sirketBox(companyService, degerLabel);
        if (user.getRole() == Role.CUSTOMER) {
            sirketBox.setVisible(true);
            if (user.getCompanyId() != null) {
                companyService.findById(user.getCompanyId()).ifPresent(sirketBox::setValue);
            }
        } else {
            sirketBox.setVisible(false);
        }

        Button kaydetBtn = kaydetButonu(() -> {
            user.setNameSurname(adSoyadField.getValue());
            user.setEmail(emailField.getValue());
            if (user.getRole() == Role.CUSTOMER) {
                user.setCompanyId(sirketBox.getValue() != null
                    ? sirketBox.getValue().getCompanyId() : null);
            }
            userService.updateUser(user);
            Notification.show("Kullanıcı güncellendi.", 3000, Notification.Position.TOP_CENTER);
        }, dialog, onSuccess);

        dialog.add(new VerticalLayout(rolBilgi, adSoyadField, emailField, sirketBox));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), kaydetBtn);
        dialog.open();
    }

    private static ComboBox<Company> sirketBox(CompanyService companyService,
                                               Function<MusteriDegeri, String> degerLabel) {
        ComboBox<Company> sirketBox = new ComboBox<>("Şirket");
        sirketBox.setItems(companyService.findAll());
        sirketBox.setItemLabelGenerator(c -> c.getName()
            + (c.getMusteriDegeri() != null ? " (" + degerLabel.apply(c.getMusteriDegeri()) + ")" : ""));
        sirketBox.setWidthFull();
        return sirketBox;
    }

    private static Button kaydetButonu(Runnable islem, Dialog dialog, Runnable onSuccess) {
        Button btn = new Button("Kaydet", e -> {
            try {
                islem.run();
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        btn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
        return btn;
    }
}
