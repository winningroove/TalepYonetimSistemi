// views/admin/AdminView.java
package com.example.views;

import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import com.example.model.User;
import com.example.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route("admin")
@PageTitle("Kullanıcı Yönetimi")
@RolesAllowed("ROLE_ADMIN")
public class AdminView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public AdminView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        H2 baslik = new H2("Kullanıcı Yönetimi");
        Button yeniKullaniciBtn = new Button("+ Yeni Kullanıcı", e -> yeniKullaniciDialogAc());

        HorizontalLayout ustBar = new HorizontalLayout(baslik, yeniKullaniciBtn);
        ustBar.setWidthFull();
        ustBar.setAlignItems(Alignment.CENTER);

        grid.addColumn(User::getNameSurname).setHeader("Ad Soyad").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("E-posta").setAutoWidth(true);
        grid.addColumn(u -> u.getRole().name()).setHeader("Rol");
        grid.addColumn(u -> u.getMusteriDegeri() != null
                ? u.getMusteriDegeri().name() : "-").setHeader("Müşteri Değeri");
        grid.addComponentColumn(u -> {
            Span durum = new Span(u.isActive() ? "Aktif" : "Pasif");
            durum.getStyle().set("color", u.isActive() ? "green" : "red");
            return durum;
        }).setHeader("Durum");
        grid.addComponentColumn(u -> {
            Button toggleBtn = new Button(
                u.isActive() ? "Pasife Al" : "Aktife Al", e -> {
                    userService.setActive(u.getUserId(), !u.isActive());
                    listeYenile();
                }
            );
            return toggleBtn;
        }).setHeader("İşlem");

        grid.setWidthFull();

        add(ustBar, grid);
        listeYenile();
    }

    private void listeYenile() {
        grid.setItems(userService.findAllActive());
    }

    private void yeniKullaniciDialogAc() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yeni Kullanıcı Ekle");
        dialog.setWidth("400px");

        TextField adSoyadField = new TextField("Ad Soyad");
        adSoyadField.setWidthFull();

        EmailField emailField = new EmailField("E-posta");
        emailField.setWidthFull();

        PasswordField sifreField = new PasswordField("Şifre");
        sifreField.setWidthFull();

        ComboBox<Role> rolBox = new ComboBox<>("Rol");
        rolBox.setItems(Role.values());
        rolBox.setWidthFull();

        ComboBox<MusteriDegeri> musteriDegeriBox = new ComboBox<>("Müşteri Değeri");
        musteriDegeriBox.setItems(MusteriDegeri.values());
        musteriDegeriBox.setWidthFull();
        musteriDegeriBox.setVisible(false);

        // Rol CUSTOMER seçilince müşteri değeri görünür
        rolBox.addValueChangeListener(e -> {
            musteriDegeriBox.setVisible(e.getValue() == Role.CUSTOMER);
        });

        Button kaydetButton = new Button("Kaydet", e -> {
            try {
                User user = new User();
                user.setNameSurname(adSoyadField.getValue());
                user.setEmail(emailField.getValue());
                user.setRole(rolBox.getValue());
                user.setMusteriDegeri(musteriDegeriBox.isVisible()
                    ? musteriDegeriBox.getValue() : null);

                userService.createUser(user, sifreField.getValue());
                Notification.show("Kullanıcı oluşturuldu.",
                    3000, Notification.Position.TOP_CENTER);
                dialog.close();
                listeYenile();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button iptalButton = new Button("İptal", e -> dialog.close());

        dialog.add(new VerticalLayout(
            adSoyadField, emailField, sifreField, rolBox, musteriDegeriBox));
        dialog.getFooter().add(iptalButton, kaydetButton);
        dialog.open();
    }
}