package com.example.views;

import com.example.company.Company;
import com.example.company.CompanyService;
import com.example.dialog.KullaniciDialog;
import com.example.dialog.OnayDialog;
import com.example.dialog.SirketDialog;
import com.example.enums.MusteriDegeri;
import com.example.enums.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.example.util.Brand;
import com.example.util.GridSearch;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("admin")
@PageTitle("Yönetim Paneli")
@RolesAllowed("ADMIN")
public class AdminView extends HorizontalLayout {

    private final UserService userService;
    private final CompanyService companyService;
    private final com.example.notification.NotificationService notificationService;
    private final com.example.notification.NotificationBroadcaster notificationBroadcaster;
    private final com.example.activity.ActivityLogService activityLogService;
    private final com.example.prioritization.PrioritizationConfigService prioritizationConfigService;
    private String currentUserName;
    private Long currentUserId;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<User> grid = new Grid<>(User.class, false);

    public AdminView(UserService userService, CompanyService companyService,
                      com.example.notification.NotificationService notificationService,
                      com.example.notification.NotificationBroadcaster notificationBroadcaster,
                      com.example.activity.ActivityLogService activityLogService,
                      com.example.prioritization.PrioritizationConfigService prioritizationConfigService) {
        this.userService = userService;
        this.companyService = companyService;
        this.notificationService = notificationService;
        this.notificationBroadcaster = notificationBroadcaster;
        this.activityLogService = activityLogService;
        this.prioritizationConfigService = prioritizationConfigService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserName = u.getNameSurname();
            currentUserId = u.getUserId();
        });

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

        Span altBaslik = new Span("Yönetim Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        HorizontalLayout bildirimSatir = new HorizontalLayout(
            new Span("Bildirimler"),
            new com.example.notification.NotificationBell(notificationService, notificationBroadcaster, currentUserId,
                reqId -> { /* şifremi unuttum bildirimlerinin ilişkili talebi yok */ }));
        bildirimSatir.setAlignItems(Alignment.CENTER);
        bildirimSatir.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("margin-top", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle()
            .set("color", "#aaaaaa")
            .set("margin-bottom", "8px")
            .set("margin-top", "24px");

        Button kullanicilarBtn = menuButton("Kullanıcı Yönetimi");
        kullanicilarBtn.addClickListener(e -> showKullanicilar());

        Button sirketlerBtn = menuButton("Şirket Yönetimi");
        sirketlerBtn.addClickListener(e -> showSirketler());

        Button skorAyarBtn = menuButton("Skor Ayarları");
        skorAyarBtn.addClickListener(e -> showSkorAyarlari());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        HorizontalLayout profilSatiri = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Admin",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, null, userService)));

        sidebar.add(Brand.sidebarLogo(), baslik, altBaslik, bildirimSatir, menuBaslik, kullanicilarBtn, sirketlerBtn, skorAyarBtn);
        sidebar.addAndExpand(new Div());
        sidebar.add(divider, profilSatiri, buildLogoutButton());

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
            btn.getStyle().set("background", "rgba(255,255,255,0.15)").set("border-left", "3px solid rgba(255,255,255,0.9)"));
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

        Button yeniKullaniciBtn = new Button("+ Yeni Kullanıcı Ekle",
            e -> KullaniciDialog.yeni(companyService, userService, this::showKullanicilar,
                this::rolLabel, this::musteriDegeriLabel));
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
        grid.addColumn(u -> u.getRole() == Role.CUSTOMER
                ? companyService.getName(u.getCompanyId()) : "-")
            .setHeader("Şirket");
        grid.addColumn(u -> u.getRole() == Role.CUSTOMER
                ? sirketDegeriLabel(u.getCompanyId()) : "-")
            .setHeader("Şirket Değeri");
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
            islemler.setPadding(false);
            islemler.setSpacing(true);
            islemler.getStyle().set("flex-wrap", "nowrap");

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

            Button duzenleBtn = new Button("Düzenle",
                e -> KullaniciDialog.duzenle(u, companyService, userService, this::showKullanicilar,
                    this::rolLabel, this::musteriDegeriLabel));
            duzenleBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

            Button silBtn = new Button("Sil", e -> OnayDialog.open("Kullanıcıyı Sil",
                "\"" + u.getNameSurname() + "\" kullanıcısını kalıcı olarak silmek istediğinize emin misiniz? "
                    + "Bu işlem geri alınamaz.",
                "Kullanıcı silindi.", this::showKullanicilar,
                () -> userService.deleteUser(u.getUserId())));
            silBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            islemler.add(toggleBtn, duzenleBtn, silBtn);
            return islemler;
        }).setHeader("İşlem").setAutoWidth(true).setFlexGrow(0);

        grid.setWidthFull();

        List<User> kullanicilar = userService.findAll();
        var arama = GridSearch.create(grid, kullanicilar,
            "Ara: ad, e-posta, rol, şirket...",
            u -> u.getNameSurname() + " " + u.getEmail() + " " + rolLabel(u.getRole())
                + " " + (u.getRole() == Role.CUSTOMER
                    ? companyService.getName(u.getCompanyId()) + " " + sirketDegeriLabel(u.getCompanyId())
                    : ""));
        grid.setItems(kullanicilar);

        mainContent.add(ustBar, aciklama, arama, grid);
    }

    private void showSirketler() {
        mainContent.removeAll();

        H2 baslik = new H2("Şirket Yönetimi");
        

        Button yeniSirketBtn = new Button("+ Yeni Şirket Ekle",
            e -> SirketDialog.yeni(companyService, this::showSirketler, this::musteriDegeriLabel));
        yeniSirketBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        HorizontalLayout ustBar = new HorizontalLayout(baslik, yeniSirketBtn);
        ustBar.setWidthFull();
        ustBar.setAlignItems(Alignment.CENTER);
        ustBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Grid<Company> sirketGrid = new Grid<>(Company.class, false);
        sirketGrid.addColumn(Company::getName).setHeader("Şirket Adı").setAutoWidth(true);
        sirketGrid.addColumn(c -> c.getMusteriDegeri() != null
                ? musteriDegeriLabel(c.getMusteriDegeri()) : "-")
            .setHeader("Şirket Değeri");
        sirketGrid.addColumn(c -> DateUtil.format(c.getCreatedAt()))
            .setHeader("Oluşturulma");
        sirketGrid.addComponentColumn(c -> {
            HorizontalLayout islemler = new HorizontalLayout();
            islemler.setPadding(false);
            islemler.getStyle().set("flex-wrap", "nowrap");
            Button duzenleBtn = new Button("Düzenle",
                e -> SirketDialog.duzenle(c, companyService, this::showSirketler, this::musteriDegeriLabel));
            duzenleBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            Button silBtn = new Button("Sil", e -> OnayDialog.open("Şirketi Sil",
                "\"" + c.getName() + "\" şirketini kalıcı olarak silmek istediğinize emin misiniz? "
                    + "Bu işlem geri alınamaz.",
                "Şirket silindi.", this::showSirketler,
                () -> companyService.deleteCompany(c.getCompanyId())));
            silBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            islemler.add(duzenleBtn, silBtn);
            return islemler;
        }).setHeader("İşlem").setAutoWidth(true).setFlexGrow(0);
        sirketGrid.setWidthFull();

        List<Company> sirketler = companyService.findAll();
        var arama = GridSearch.create(sirketGrid, sirketler,
            "Ara: şirket adı, değer...",
            c -> c.getName()
                + " " + (c.getMusteriDegeri() != null ? musteriDegeriLabel(c.getMusteriDegeri()) : "")
                + " " + DateUtil.format(c.getCreatedAt()));
        sirketGrid.setItems(sirketler);

        mainContent.add(ustBar, arama, sirketGrid);
    }

    private void showSkorAyarlari() {
        mainContent.removeAll();
        mainContent.add(new com.example.prioritization.SkorAyarlariForm(prioritizationConfigService));
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

    /** Müşterinin bağlı olduğu şirketin değer etiketi (miras). */
    private String sirketDegeriLabel(Long companyId) {
        return companyService.findById(companyId)
                .map(Company::getMusteriDegeri)
                .map(this::musteriDegeriLabel)
                .orElse("-");
    }
}