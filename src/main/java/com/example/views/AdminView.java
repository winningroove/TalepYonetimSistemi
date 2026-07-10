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
import com.example.util.AppSidebar;
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
        var bell = new com.example.notification.NotificationBell(
            notificationService, notificationBroadcaster, currentUserId,
            reqId -> { /* şifremi unuttum bildirimlerinin ilişkili talebi yok */ });
        var profil = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Admin",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, null, userService)));

        return AppSidebar.build("Yönetim Paneli", bell, List.of(
            AppSidebar.menuButton("Kullanıcı Yönetimi", this::showKullanicilar),
            AppSidebar.menuButton("Şirket Yönetimi", this::showSirketler),
            AppSidebar.menuButton("Skor Ayarları", this::showSkorAyarlari)
        ), profil);
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