package com.example.views;
import com.example.dialog.SmAtamaDialog;
import com.example.dialog.SmGorevDetayDialog;
import com.example.enums.WorkflowStatus;
import com.example.prioritization.PrioritizationService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.example.util.Brand;
import com.example.util.GridSearch;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("scrum-master")
@PageTitle("Scrum Master Paneli")
@RolesAllowed("SCRUM_MASTER")
public class ScrumMasterView extends HorizontalLayout {

    private final WorkflowService workflowService;
    private final RequestService requestService;
    private final UserService userService;
    private final PrioritizationService prioritizationService;
    private final com.example.company.CompanyService companyService;
    private final com.example.message.RequestMessageService requestMessageService;
    private final com.example.notification.NotificationService notificationService;
    private final com.example.notification.NotificationBroadcaster notificationBroadcaster;
    private final com.example.activity.ActivityLogService activityLogService;


    private String currentUserName;
    private Long currentUserId;
    private final VerticalLayout mainContent = new VerticalLayout();
public ScrumMasterView(WorkflowService workflowService,
                       RequestService requestService,
                       UserService userService,
                       PrioritizationService prioritizationService,
                       com.example.company.CompanyService companyService,
                       com.example.message.RequestMessageService requestMessageService,
                       com.example.notification.NotificationService notificationService,
                       com.example.notification.NotificationBroadcaster notificationBroadcaster,
                       com.example.activity.ActivityLogService activityLogService) {
    this.workflowService = workflowService;
    this.requestService = requestService;
    this.userService = userService;
    this.prioritizationService = prioritizationService;
    this.companyService = companyService;
    this.requestMessageService = requestMessageService;
    this.notificationService = notificationService;
    this.notificationBroadcaster = notificationBroadcaster;
    this.activityLogService = activityLogService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserName = u.getNameSurname();
            currentUserId = u.getUserId();
        });

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showSprintBoard();
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

        Span altBaslik = new Span("Scrum Master Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        HorizontalLayout bildirimSatir = new HorizontalLayout(
            new Span("Bildirimler"),
            new com.example.notification.NotificationBell(notificationService, notificationBroadcaster, currentUserId,
                reqId -> workflowService.findByRequestId(reqId).ifPresent(this::openGorevDetay)));
        bildirimSatir.setAlignItems(Alignment.CENTER);
        bildirimSatir.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("margin-top", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle()
            .set("color", "#aaaaaa")
            .set("margin-bottom", "8px")
            .set("margin-top", "24px");

        Button sprintBtn      = menuButton("Sprint Board");
        Button atanmamisBtn   = menuButton("Atanmamış Görevler");
        Button tamamlananBtn  = menuButton("Tamamlanan Görevler");

        sprintBtn.addClickListener(e -> showSprintBoard());
        atanmamisBtn.addClickListener(e -> showAtanmamisGorevler());
        tamamlananBtn.addClickListener(e -> showTamamlananlar());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        HorizontalLayout profilSatiri = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Scrum Master",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, requestService, userService)));

        sidebar.add(Brand.sidebarLogo(), baslik, altBaslik, bildirimSatir, menuBaslik, sprintBtn, atanmamisBtn, tamamlananBtn);
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

    // ── Sprint Board ──
    private void showSprintBoard() {
        mainContent.removeAll();

        H2 baslik = new H2("Sprint Board");
        Paragraph aciklama = new Paragraph(
            "Tüm aktif görevler ve durumları. Geliştiricilere görev atayabilirsiniz.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> sirketAdiByRequest(w.getRequestId())).setHeader("Şirket").setAutoWidth(true);
        grid.addColumn(w -> talepTarihi(w.getRequestId())).setHeader("Tarih").setAutoWidth(true);
        grid.addColumn(w -> developerAdi(w.getDeveloperId())).setHeader("Geliştirici").setAutoWidth(true);
        grid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
        grid.addComponentColumn(w -> {
            boolean atanmamis = w.getDeveloperId() == null;
            Button ataBtn = new Button(
                atanmamis ? "Geliştirici Ata" : "Yeniden Ata",
                e -> openAtama(w)
            );
            if (atanmamis) {
                ataBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            } else {
                ataBtn.getStyle().set("background", "#fff3cd").set("color", "#856404");
            }
            return ataBtn;
        }).setHeader("İşlem");
        grid.addComponentColumn(w -> {
            Button detayBtn = new Button("Detay", e -> openGorevDetay(w));
            detayBtn.getStyle().set("background-color", "#036baa").set("color", "white");
            return detayBtn;
        }).setHeader("Detay").setAutoWidth(true).setFlexGrow(0);
        grid.setWidthFull();

        List<Workflow> aktifGorevler = workflowService.getAllActiveWorkflows();
        var arama = GridSearch.create(grid, aktifGorevler,
            "Ara: talep, şirket, geliştirici, tarih...", this::gorevAranabilir);
        grid.setItems(aktifGorevler);

        mainContent.add(baslik, aciklama, arama, grid);
    }

    // ── Atanmamış Görevler ──
    private void showAtanmamisGorevler() {
        mainContent.removeAll();

        H2 baslik = new H2("Atanmamış Görevler");
        Paragraph aciklama = new Paragraph(
            "Henüz bir geliştiriciye atanmamış görevler.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> sirketAdiByRequest(w.getRequestId())).setHeader("Şirket").setAutoWidth(true);
        grid.addColumn(w -> talepTarihi(w.getRequestId())).setHeader("Tarih").setAutoWidth(true);
        grid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
        grid.addComponentColumn(w -> {
            Button ataBtn = new Button("Geliştirici Ata", e -> openAtama(w));
            ataBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            return ataBtn;
        }).setHeader("İşlem");
        grid.setWidthFull();

        List<Workflow> atanmamislar = workflowService.getUnassignedWorkflows();
        var arama = GridSearch.create(grid, atanmamislar,
            "Ara: talep, şirket, tarih...", this::gorevAranabilir);
        grid.setItems(atanmamislar);

        mainContent.add(baslik, aciklama, arama, grid);
    }

    // ── Tamamlanan Görevler ──
    private void showTamamlananlar() {
        mainContent.removeAll();

        H2 baslik = new H2("Tamamlanan Görevler");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> sirketAdiByRequest(w.getRequestId())).setHeader("Şirket").setAutoWidth(true);
        grid.addColumn(w -> developerAdi(w.getDeveloperId())).setHeader("Geliştirici").setAutoWidth(true);
        grid.addColumn(w -> DateUtil.format(w.getUpdatedAt())).setHeader("Tamamlanma Tarihi");
        grid.addComponentColumn(w -> durumBadge(WorkflowStatus.DONE)).setHeader("Durum");
        grid.setWidthFull();

        List<Workflow> tumGorevler = workflowService.getAllWorkflows();
        List<Workflow> tamamlananlar = tumGorevler.stream()
            .filter(w -> w.getWorkflowStatus() == WorkflowStatus.DONE)
            .toList();

        var arama = GridSearch.create(grid, tamamlananlar,
            "Ara: talep, şirket, geliştirici, tarih...", this::gorevAranabilir);
        grid.setItems(tamamlananlar);

        mainContent.add(baslik, arama, grid);
    }

    /** Geliştirici atama penceresini açar (görünüme özgü başlığı hesaplayıp dialog sınıfına verir). */
    private void openAtama(Workflow w) {
        SmAtamaDialog.open(w, talepBasligi(w.getRequestId()), this::showSprintBoard,
            userService, workflowService, prioritizationService);
    }

    /** Görev detay penceresini açar (şirket/geliştirici/durum etiketlerini hesaplayıp dialog sınıfına verir). */
    private void openGorevDetay(Workflow w) {
        SmGorevDetayDialog.open(w, currentUserId,
            sirketAdiByRequest(w.getRequestId()),
            developerAdi(w.getDeveloperId()),
            durumBadge(w.getWorkflowStatus()).getText(),
            this::showSprintBoard,
            requestService, requestMessageService, userService, activityLogService, workflowService);
    }

    private String talepBasligi(Long requestId) {
        return requestService.findById(requestId)
            .map(r -> r.getTitle())
            .orElse("Bilinmiyor");
    }

    private String talepTarihi(Long requestId) {
        return requestService.findById(requestId)
            .map(r -> DateUtil.format(r.getCreatedAt()))
            .orElse("-");
    }

    private String sirketAdiByRequest(Long requestId) {
        return requestService.findById(requestId)
            .flatMap(r -> userService.findById(r.getCustomerId()))
            .map(u -> companyService.getName(u.getCompanyId()))
            .orElse("-");
    }

    /** Görev için aranabilir metin: talep, şirket, geliştirici, durum, tarih. */
    private String gorevAranabilir(Workflow w) {
        return talepBasligi(w.getRequestId())
            + " " + sirketAdiByRequest(w.getRequestId())
            + " " + developerAdi(w.getDeveloperId())
            + " " + w.getWorkflowStatus().name()
            + " " + talepTarihi(w.getRequestId());
    }

    private String developerAdi(Long developerId) {
        if (developerId == null) return "Atanmadı";
        return userService.findById(developerId)
            .map(User::getNameSurname)
            .orElse("Bilinmiyor");
    }

    private Span durumBadge(WorkflowStatus status) {
        String label = switch (status) {
            case BACKLOG     -> "Sırada";
            case IN_PROGRESS -> "Devam Ediyor";
            case TESTING     -> "Test Aşamasında";
            case DONE        -> "Tamamlandı";
        };
        Span badge = new Span(label);
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px");
        switch (status) {
            case BACKLOG     -> badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
            case IN_PROGRESS -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case TESTING     -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case DONE        -> badge.getStyle().set("background", "#d4edda").set("color", "#155724");
        }
        return badge;
    }
}