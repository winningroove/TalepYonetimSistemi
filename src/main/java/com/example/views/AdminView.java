package com.example.views;

import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import com.example.model.User;
import com.example.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("admin")
@PageTitle("Yönetim Paneli")
@RolesAllowed("ADMIN")
public class AdminView extends HorizontalLayout {

    private final UserService userService;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<User> grid = new Grid<>(User.class, false);

    public AdminView(UserService userService) {
        this.userService = userService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> currentUserName = u.getNameSurname());

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showKullanicilar();
    }

    private VerticalLayout buildSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("260px");
        sidebar.setHeightFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(false);
        sidebar.getStyle()
            .set("background-color", "#1B2A3B")
            .set("color", "white")
            .set("flex-shrink", "0");

        H3 baslik = new H3("Talep Yönetim Sistemi");
        baslik.getStyle().set("color", "white").set("margin-top", "0");

        Span altBaslik = new Span("Yönetim Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle()
            .set("color", "#aaaaaa")
            .set("margin-bottom", "8px")
            .set("margin-top", "24px");

        Button kullanicilarBtn = menuButton("Kullanıcı Yönetimi");
        kullanicilarBtn.addClickListener(e -> showKullanicilar());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Admin)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, menuBaslik, kullanicilarBtn);
        sidebar.addAndExpand(new Div());
        sidebar.add(divider, girisYapan, kullaniciAdi, buildLogoutButton());

        return sidebar;
    }

    private Button menuButton(String text) {
        Button btn = new Button(text);
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
            btn.getStyle().set("background", "rgba(255,255,255,0.15)").set("border-left", "3px solid #4A9EDF"));
        btn.getElement().addEventListener("mouseout", e ->
            btn.getStyle().set("background", "rgba(255,255,255,0.07)").set("border-left", "3px solid rgba(255,255,255,0.2)"));
        return btn;
    }

    private Button buildLogoutButton() {
        Button logoutBtn = new Button("Çıkış Yap",
            e -> com.vaadin.flow.component.UI.getCurrent().getPage().setLocation("/logout"));
        logoutBtn.getStyle()
            .set("background-color", "#c0392b")
            .set("color", "white")
            .set("width", "100%")
            .set("margin-top", "12px")
            .set("cursor", "pointer");
        return logoutBtn;
    }

    private VerticalLayout buildMainContent() {
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        return mainContent;
    }

    private void showKullanicilar() {
        mainContent.removeAll();

        H2 baslik = new H2("Kullanıcı Yönetimi");
        Paragraph aciklama = new Paragraph(
            "Sistemdeki tüm kullanıcılar. Pasife alınan kullanıcılar giriş yapamaz.");

        Button yeniKullaniciBtn = new Button("+ Yeni Kullanıcı Ekle", e -> yeniKullaniciDialogAc());
        yeniKullaniciBtn.getStyle()
            .set("background-color", "#1B2A3B")
            .set("color", "white");

        HorizontalLayout ustBar = new HorizontalLayout(baslik, yeniKullaniciBtn);
        ustBar.setWidthFull();
        ustBar.setAlignItems(Alignment.CENTER);
        ustBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        grid.removeAllColumns();
        grid.addColumn(User::getNameSurname).setHeader("Ad Soyad").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("E-posta").setAutoWidth(true);
        grid.addColumn(u -> rolLabel(u.getRole())).setHeader("Rol");
        grid.addColumn(u -> u.getMusteriDegeri() != null
                ? musteriDegeriLabel(u.getMusteriDegeri()) : "-")
            .setHeader("Müşteri Değeri");
        grid.addComponentColumn(u -> {
            Span durum = new Span(u.isActive() ? "Aktif" : "Pasif");
            durum.getStyle()
                .set("padding", "4px 8px")
                .set("border-radius", "4px")
                .set("font-size", "12px")
                .set("font-weight", "bold");
            if (u.isActive()) {
                durum.getStyle().set("background", "#d4edda").set("color", "#155724");
            } else {
                durum.getStyle().set("background", "#f8d7da").set("color", "#721c24");
            }
            return durum;
        }).setHeader("Durum");
        grid.addComponentColumn(u -> {
            HorizontalLayout islemler = new HorizontalLayout();

            Button toggleBtn = new Button(
                u.isActive() ? "Pasife Al" : "Aktife Al",
                e -> {
                    userService.setActive(u.getUserId(), !u.isActive());
                    Notification.show(
                        u.isActive() ? "Kullanıcı pasife alındı." : "Kullanıcı aktive edildi.",
                        3000, Notification.Position.TOP_CENTER);
                    showKullanicilar();
                }
            );
            if (u.isActive()) {
                toggleBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            } else {
                toggleBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            }

            Button duzenleBtn = new Button("Düzenle", e -> duzenleDialogAc(u));
            duzenleBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

            islemler.add(toggleBtn, duzenleBtn);
            return islemler;
        }).setHeader("İşlem");

        grid.setWidthFull();
        grid.setItems(userService.findAll());

        mainContent.add(ustBar, aciklama, grid);
    }

    private void yeniKullaniciDialogAc() {
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
        rolBox.setItemLabelGenerator(this::rolLabel);
        rolBox.setWidthFull();

        ComboBox<MusteriDegeri> musteriDegeriBox = new ComboBox<>("Müşteri Değeri");
        musteriDegeriBox.setItems(MusteriDegeri.values());
        musteriDegeriBox.setItemLabelGenerator(this::musteriDegeriLabel);
        musteriDegeriBox.setWidthFull();
        musteriDegeriBox.setVisible(false);

        rolBox.addValueChangeListener(e -> {
            boolean isCustomer = e.getValue() != null && e.getValue() == Role.CUSTOMER;
            musteriDegeriBox.setVisible(isCustomer);
            if (!isCustomer) musteriDegeriBox.clear();
        });

        Button kaydetBtn = new Button("Kaydet", e -> {
            try {
                User user = new User();
                user.setNameSurname(adSoyadField.getValue());
                user.setEmail(emailField.getValue());
                user.setRole(rolBox.getValue());
                user.setMusteriDegeri(musteriDegeriBox.isVisible()
                    ? musteriDegeriBox.getValue() : null);

                userService.createUser(user, sifreField.getValue());
                Notification.show("Kullanıcı oluşturuldu.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                showKullanicilar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        kaydetBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        dialog.add(new VerticalLayout(adSoyadField, emailField, sifreField, rolBox, musteriDegeriBox));
        dialog.getFooter().add(iptalBtn, kaydetBtn);
        dialog.open();
    }

    private void duzenleDialogAc(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kullanıcı Düzenle — " + user.getNameSurname());
        dialog.setWidth("420px");

        TextField adSoyadField = new TextField("Ad Soyad");
        adSoyadField.setValue(user.getNameSurname());
        adSoyadField.setWidthFull();

        EmailField emailField = new EmailField("E-posta");
        emailField.setValue(user.getEmail());
        emailField.setWidthFull();

        Span rolBilgi = new Span("Rol: " + rolLabel(user.getRole()));
        rolBilgi.getStyle().set("color", "#666").set("font-size", "13px");

        ComboBox<MusteriDegeri> musteriDegeriBox = new ComboBox<>("Müşteri Değeri");
        musteriDegeriBox.setItems(MusteriDegeri.values());
        musteriDegeriBox.setItemLabelGenerator(this::musteriDegeriLabel);
        musteriDegeriBox.setWidthFull();

        if (user.getRole() == Role.CUSTOMER) {
            musteriDegeriBox.setVisible(true);
            if (user.getMusteriDegeri() != null) {
                musteriDegeriBox.setValue(user.getMusteriDegeri());
            }
        } else {
            musteriDegeriBox.setVisible(false);
        }

        Button kaydetBtn = new Button("Kaydet", e -> {
            try {
                user.setNameSurname(adSoyadField.getValue());
                user.setEmail(emailField.getValue());
                if (user.getRole() == Role.CUSTOMER) {
                    user.setMusteriDegeri(musteriDegeriBox.getValue());
                }
                userService.updateUser(user);
                Notification.show("Kullanıcı güncellendi.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                showKullanicilar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        kaydetBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        dialog.add(new VerticalLayout(rolBilgi, adSoyadField, emailField, musteriDegeriBox));
        dialog.getFooter().add(iptalBtn, kaydetBtn);
        dialog.open();
    }

    private String rolLabel(Role role) {
    return switch (role) {
        case CUSTOMER      -> "Müşteri";
        case PRODUCT_OWNER -> "Ürün Sorumlusu";
        case DEVELOPER     -> "Geliştirici";
        case ADMIN         -> "Admin";
        case SCRUM_MASTER  -> "Scrum Master";
    };
}

    private String musteriDegeriLabel(MusteriDegeri md) {
        return switch (md) {
            case VIP          -> "VIP";
            case BUYUK        -> "Büyük";
            case ORTA         -> "Orta";
            case KUCUK        -> "Küçük";
            case IC_KULLANICI -> "İç Kullanıcı";
        };
    }
}