package com.example.views;
import com.example.dialog.SmAtamaDialog;
import com.example.dialog.SmGorevDetayDialog;
import com.example.enums.WorkflowStatus;
import com.example.prioritization.PrioritizationService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.AppSidebar;
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
        var bell = new com.example.notification.NotificationBell(
            notificationService, notificationBroadcaster, currentUserId,
            reqId -> workflowService.findByRequestId(reqId).ifPresent(this::openGorevDetay));
        var profil = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Scrum Master",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, requestService, userService)));

        return AppSidebar.build("Scrum Master Paneli", bell, List.of(
            AppSidebar.menuButton("Sprint Board", this::showSprintBoard),
            AppSidebar.menuButton("Atanmamış Görevler", this::showAtanmamisGorevler),
            AppSidebar.menuButton("Tamamlanan Görevler", this::showTamamlananlar)
        ), profil);
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
        grid.addComponentColumn(w -> com.example.dialog.DialogSupport.workflowBadge(w.getWorkflowStatus())).setHeader("Durum");
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
        grid.addComponentColumn(w -> com.example.dialog.DialogSupport.workflowBadge(w.getWorkflowStatus())).setHeader("Durum");
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
        grid.addComponentColumn(w -> com.example.dialog.DialogSupport.workflowBadge(WorkflowStatus.DONE)).setHeader("Durum");
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
            com.example.dialog.DialogSupport.workflowBadge(w.getWorkflowStatus()).getText(),
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
}