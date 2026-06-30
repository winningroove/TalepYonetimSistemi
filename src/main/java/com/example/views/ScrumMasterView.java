package com.example.views;
import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.Role;
import com.example.enums.WorkflowStatus;
import com.example.prioritization.PrioritizationService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
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


    private String currentUserName;
    private final VerticalLayout mainContent = new VerticalLayout();
public ScrumMasterView(WorkflowService workflowService,
                       RequestService requestService,
                       UserService userService,
                       PrioritizationService prioritizationService) {
    this.workflowService = workflowService;
    this.requestService = requestService;
    this.userService = userService;
    this.prioritizationService = prioritizationService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> currentUserName = u.getNameSurname());

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
            .set("background-color", "#1B2A3B")
            .set("color", "white")
            .set("flex-shrink", "0");

        H3 baslik = new H3("Talep Yönetim Sistemi");
        baslik.getStyle().set("color", "white").set("margin-top", "0");

        Span altBaslik = new Span("Scrum Master Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

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

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Scrum Master)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, menuBaslik, sprintBtn, atanmamisBtn, tamamlananBtn);
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

    // ── Sprint Board ──
    private void showSprintBoard() {
        mainContent.removeAll();

        H2 baslik = new H2("Sprint Board");
        Paragraph aciklama = new Paragraph(
            "Tüm aktif görevler ve durumları. Geliştiricilere görev atayabilirsiniz.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> developerAdi(w.getDeveloperId())).setHeader("Geliştirici").setAutoWidth(true);
        grid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
        grid.addComponentColumn(w -> {
            boolean atanmamis = w.getDeveloperId() == null;
            Button ataBtn = new Button(
                atanmamis ? "Geliştirici Ata" : "Yeniden Ata",
                e -> atamaDiyaloguAc(w)
            );
            if (atanmamis) {
                ataBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            } else {
                ataBtn.getStyle().set("background", "#fff3cd").set("color", "#856404");
            }
            return ataBtn;
        }).setHeader("İşlem");
        grid.setWidthFull();
        grid.setItems(workflowService.getAllActiveWorkflows());

        mainContent.add(baslik, aciklama, grid);
    }

    // ── Atanmamış Görevler ──
    private void showAtanmamisGorevler() {
        mainContent.removeAll();

        H2 baslik = new H2("Atanmamış Görevler");
        Paragraph aciklama = new Paragraph(
            "Henüz bir geliştiriciye atanmamış görevler.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
        grid.addComponentColumn(w -> {
            Button ataBtn = new Button("Geliştirici Ata", e -> atamaDiyaloguAc(w));
            ataBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            return ataBtn;
        }).setHeader("İşlem");
        grid.setWidthFull();
        grid.setItems(workflowService.getUnassignedWorkflows());

        mainContent.add(baslik, aciklama, grid);
    }

    // ── Tamamlanan Görevler ──
    private void showTamamlananlar() {
        mainContent.removeAll();

        H2 baslik = new H2("Tamamlanan Görevler");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> developerAdi(w.getDeveloperId())).setHeader("Geliştirici").setAutoWidth(true);
        grid.addColumn(w -> w.getUpdatedAt().toLocalDate()).setHeader("Tamamlanma Tarihi");
        grid.addComponentColumn(w -> durumBadge(WorkflowStatus.DONE)).setHeader("Durum");
        grid.setWidthFull();

        List<Workflow> tumGorevler = workflowService.getAllWorkflows();
        List<Workflow> tamamlananlar = tumGorevler.stream()
            .filter(w -> w.getWorkflowStatus() == WorkflowStatus.DONE)
            .toList();
        grid.setItems(tamamlananlar);

        mainContent.add(baslik, grid);
    }

    // ── Geliştirici Atama Dialogu ──
 private void atamaDiyaloguAc(Workflow workflow) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Gelistirici Ata — " + talepBasligi(workflow.getRequestId()));

    List<User> developerlar = userService.findAllActive().stream()
        .filter(u -> u.getRole() == Role.DEVELOPER)
        .toList();

    ComboBox<User> developerBox = new ComboBox<>("Gelistirici Sec");
    developerBox.setItems(developerlar);
    developerBox.setItemLabelGenerator(User::getNameSurname);
    developerBox.setWidthFull();

    if (workflow.getDeveloperId() != null) {
        developerlar.stream()
            .filter(u -> u.getUserId().equals(workflow.getDeveloperId()))
            .findFirst()
            .ifPresent(developerBox::setValue);
    }

    ComboBox<GelistiriciMudahalesi> cabaBox = new ComboBox<>("Caba Tahmini");
    cabaBox.setItems(GelistiriciMudahalesi.values());
    cabaBox.setItemLabelGenerator(v -> switch (v) {
        case QUICK_WIN  -> "Quick Win (< 1 gun) +10";
        case DUSUK      -> "Dusuk (1-3 gun) +5";
        case ORTA       -> "Orta (1-2 hafta) 0";
        case YUKSEK     -> "Yuksek (> 2 hafta) -5";
        case COK_YUKSEK -> "Cok Yuksek / Belirsiz -10";
    });
    cabaBox.setWidthFull();

    // Mevcut deger varsa set et
    prioritizationService.findByRequestId(workflow.getRequestId())
        .ifPresent(p -> {
            if (p.getGelistiriciMudahalesi() != null) {
                cabaBox.setValue(p.getGelistiriciMudahalesi());
            }
        });

    Button ataBtn = new Button("Ata", e -> {
        if (developerBox.getValue() == null) {
            Notification.show("Gelistirici seciniz.", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (cabaBox.getValue() == null) {
            Notification.show("Caba tahmini seciniz.", 3000, Notification.Position.MIDDLE);
            return;
        }
        try {
            // Gelistirici ata
            workflowService.assignDeveloperBySM(
                workflow.getTaskId(),
                developerBox.getValue().getUserId(),
                workflow.getVersion()
            );

            // Caba tahminini kaydet ve nihai skoru hesapla
            // (yonetici takdiri ve guvenilirlik skoru servis icinde talepten okunur)
            prioritizationService.updateGelistiriciMudahalesi(
                workflow.getRequestId(),
                cabaBox.getValue()
            );

            Notification.show("Gelistirici atandi, skor guncellendi.",
                3000, Notification.Position.TOP_CENTER);
            dialog.close();
            showSprintBoard();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    });
    ataBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

    Button iptalBtn = new Button("Iptal", e -> dialog.close());

    dialog.add(new VerticalLayout(developerBox, cabaBox));
    dialog.getFooter().add(iptalBtn, ataBtn);
    dialog.open();
}

    private String talepBasligi(Long requestId) {
        return requestService.findById(requestId)
            .map(r -> r.getTitle())
            .orElse("Bilinmiyor");
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