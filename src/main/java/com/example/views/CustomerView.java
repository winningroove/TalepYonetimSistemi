package com.example.views;

import com.example.dialog.MusteriDetayDialog;
import com.example.enums.RequestStatus;
import com.example.enums.WorkflowStatus;
import com.example.message.RequestMessageService;
import com.example.request.Request;
import com.example.request.RequestFile;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.example.util.Brand;
import com.example.util.GridSearch;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;

@Route("customer")
@PageTitle("Müşteri Portalı")
@RolesAllowed("CUSTOMER")
public class CustomerView extends HorizontalLayout {

    private final RequestService requestService;
    private final UserService userService;
    private Long currentUserId;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<Request> grid = new Grid<>(Request.class, false);
    private final RequestFileService requestFileService;
    private final WorkflowService workflowService;
    private final RequestMessageService requestMessageService;
    private final com.example.notification.NotificationService notificationService;
    private final com.example.notification.NotificationBroadcaster notificationBroadcaster;
    private final com.example.activity.ActivityLogService activityLogService;
    private final com.example.company.CompanyService companyService;

public CustomerView(RequestService requestService,
                    UserService userService,
                    RequestFileService requestFileService,
                    WorkflowService workflowService,
                    RequestMessageService requestMessageService,
                    com.example.notification.NotificationService notificationService,
                    com.example.notification.NotificationBroadcaster notificationBroadcaster,
                    com.example.activity.ActivityLogService activityLogService,
                    com.example.company.CompanyService companyService) {
    this.requestService = requestService;
    this.userService = userService;
    this.requestFileService = requestFileService;
    this.workflowService = workflowService;
    this.requestMessageService = requestMessageService;
    this.notificationService = notificationService;
    this.notificationBroadcaster = notificationBroadcaster;
    this.activityLogService = activityLogService;
    this.companyService = companyService;


        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserId = u.getUserId();
            currentUserName = u.getNameSurname();
        });

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showTaleplerim();
    }

    private VerticalLayout buildSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("250px");
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

        // Başlık
        H3 baslik = new H3("Talep Yönetim Sistemi");
        baslik.getStyle().set("color", "white").set("margin-top", "0");

        Span altBaslik = new Span("Müşteri Portalı");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        HorizontalLayout bildirimSatir = new HorizontalLayout(
            new Span("Bildirimler"),
            new com.example.notification.NotificationBell(notificationService, notificationBroadcaster, currentUserId,
                reqId -> requestService.findById(reqId).ifPresent(r -> MusteriDetayDialog.open(
                    r, currentUserId, durumMetni(r), requestFileService, requestMessageService,
                    userService, activityLogService))));
        bildirimSatir.setAlignItems(Alignment.CENTER);
        bildirimSatir.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("margin-top", "12px");

        // Menü
        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle().set("color", "#aaaaaa").set("margin-bottom", "8px").set("margin-top", "24px");

        Button yeniTalepBtn = menuButton("Yeni Talep Ekle");
        Button taleplerimBtn = menuButton("Taleplerim");

        yeniTalepBtn.addClickListener(e -> showYeniTalepFormu());
        taleplerimBtn.addClickListener(e -> showTaleplerim());

        // Giriş yapan kullanıcı
        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        HorizontalLayout profilSatiri = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Müşteri",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, requestService, userService)));

        sidebar.add(Brand.sidebarLogo(), baslik, altBaslik, bildirimSatir, menuBaslik, yeniTalepBtn, taleplerimBtn);
        sidebar.addAndExpand(new Div()); // boşluğu aşağı it
        sidebar.add(divider, profilSatiri, new com.example.util.ThemeToggle(), buildLogoutButton());

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
        mainContent.setSpacing(true);
        return mainContent;
    }

    private void showYeniTalepFormu() {
    mainContent.removeAll();

    H2 baslik = new H2("Yeni Destek / Geliştirme Talebi Bildir");

    TextField talepBasligi = new TextField("Talep Başlığı");
    talepBasligi.setPlaceholder("Örn: Ödeme ekranında kredi kartı hata uyarısı alınıyor...");
    talepBasligi.setWidthFull();

    TextArea talepDetayi = new TextArea("Talep Detayı ve Açıklama");
    talepDetayi.setPlaceholder("Yaşanan problemi veya eklenmesini istediğiniz özelliği detaylıca yazınız...");
    talepDetayi.setWidthFull();
    talepDetayi.setMinHeight("150px");

    // Dosya upload
    MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
    Upload upload = new Upload(buffer);
    upload.setAcceptedFileTypes(
        "image/*", "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ".txt", ".xlsx", ".xls"
    );
    upload.setMaxFileSize(10 * 1024 * 1024); // 10 MB
    upload.setMaxFiles(5);
    upload.setDropLabel(new Span("Dosyaları buraya sürükleyin veya seçin (max 5 dosya, 10 MB)"));

    Span uploadNot = new Span("Desteklenen: PDF, Word, Excel, resim, metin dosyaları");
    uploadNot.getStyle().set("color", "#888").set("font-size", "12px");

    Button gondерBtn = new Button("Talebi Gönder");
    gondерBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    gondерBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

   gondерBtn.addClickListener(e -> {
    try {
        Request request = new Request();
        request.setCustomerId(currentUserId);
        request.setTitle(talepBasligi.getValue());
        request.setDescription(talepDetayi.getValue());
        requestService.createRequest(request);

        // En son eklenen talebin ID'sini al
        requestService.findLastRequestIdByCustomer(currentUserId).ifPresent(requestId -> {
            for (String fileName : buffer.getFiles()) {
                try {
                    byte[] fileData = buffer.getInputStream(fileName).readAllBytes();
                    requestFileService.saveFile(requestId, fileName, fileData);
                } catch (Exception ex) {
                    Notification.show("Dosya yüklenemedi: " + fileName,
                        3000, Notification.Position.MIDDLE);
                }
            }
        });

        Notification.show("Talebiniz alındı.", 3000, Notification.Position.TOP_CENTER);
        showTaleplerim();
    } catch (IllegalArgumentException ex) {
        Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
    }
});

    HorizontalLayout btnLayout = new HorizontalLayout(gondерBtn);
    btnLayout.setWidthFull();
    btnLayout.setJustifyContentMode(JustifyContentMode.END);

    mainContent.add(baslik, talepBasligi, talepDetayi, upload, uploadNot, btnLayout);
}

    private void showTaleplerim() {
        mainContent.removeAll();

        H2 baslik = new H2("Taleplerim");

        grid.removeAllColumns();
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addColumn(r -> DateUtil.format(r.getCreatedAt())).setHeader("Tarih");
        grid.addComponentColumn(this::durumBadgeFor).setHeader("Durum");
        grid.addComponentColumn(r -> {
            Button detayBtn = new Button("Detay", e -> MusteriDetayDialog.open(
                r, currentUserId, durumMetni(r), requestFileService, requestMessageService,
                userService, activityLogService));
            return detayBtn;
        }).setHeader("İşlem");
        grid.setWidthFull();

        List<Request> talepler = currentUserId != null
            ? requestService.getCustomerRequests(currentUserId)
            : List.of();

        var arama = GridSearch.create(grid, talepler, "Ara: başlık, durum, tarih...",
            r -> r.getTitle() + " " + durumMetni(r) + " " + DateUtil.format(r.getCreatedAt()));

        grid.setItems(talepler);

        mainContent.add(baslik, arama, grid);
    }

    private String durumLabel(RequestStatus status) {
        return switch (status) {
            case NEW          -> "Yeni";
            case UNDER_REVIEW -> "İncelemede";
            case PRIORITIZED  -> "Önceliklendirildi";
            case REJECTED     -> "Reddedildi";
            case DUPLICATE    -> "Birleştirildi";
        };
    }

    private Span durumBadge(RequestStatus status) {
        Span badge = new Span(durumLabel(status));
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px").set("font-weight", "bold");
        switch (status) {
            case NEW          -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case UNDER_REVIEW -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case PRIORITIZED  -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case REJECTED     -> badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
            case DUPLICATE    -> badge.getStyle().set("background", "#e2d9f3").set("color", "#4b2e83");
        }
        return badge;
    }

    private boolean isTamamlandi(Request r) {
        return r.getStatus() == RequestStatus.PRIORITIZED
            && workflowService.findByRequestId(r.getRequestId())
                .map(w -> w.getWorkflowStatus() == WorkflowStatus.DONE)
                .orElse(false);
    }

    private String durumMetni(Request r) {
        return isTamamlandi(r) ? "Tamamlandı" : durumLabel(r.getStatus());
    }

    private Span durumBadgeFor(Request r) {
        if (isTamamlandi(r)) {
            Span badge = new Span("Tamamlandı");
            badge.getStyle()
                .set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px")
                .set("background", "#d4edda").set("color", "#155724").set("font-weight", "bold");
            return badge;
        }
        if (r.getStatus() == RequestStatus.DUPLICATE) {
            String ref = r.getMergedInto() != null ? " #" + r.getMergedInto() : "";
            Span badge = new Span("Birleştirildi" + ref);
            badge.getStyle()
                .set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px")
                .set("background", "#e2d9f3").set("color", "#4b2e83").set("font-weight", "bold");
            return badge;
        }
        return durumBadge(r.getStatus());
    }
}