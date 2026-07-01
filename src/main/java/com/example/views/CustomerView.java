package com.example.views;

import com.example.enums.RequestStatus;
import com.example.enums.Role;
import com.example.enums.WorkflowStatus;
import com.example.message.RequestMessage;
import com.example.message.RequestMessageService;
import com.example.request.Request;
import com.example.request.RequestFile;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
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

public CustomerView(RequestService requestService,
                    UserService userService,
                    RequestFileService requestFileService,
                    WorkflowService workflowService,
                    RequestMessageService requestMessageService,
                    com.example.notification.NotificationService notificationService,
                    com.example.notification.NotificationBroadcaster notificationBroadcaster) {
    this.requestService = requestService;
    this.userService = userService;
    this.requestFileService = requestFileService;
    this.workflowService = workflowService;
    this.requestMessageService = requestMessageService;
    this.notificationService = notificationService;
    this.notificationBroadcaster = notificationBroadcaster;


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
                reqId -> requestService.findById(reqId).ifPresent(this::detayDialogAc)));
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

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Müşteri)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, bildirimSatir, menuBaslik, yeniTalepBtn, taleplerimBtn);
        sidebar.addAndExpand(new Div()); // boşluğu aşağı it
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
            Button detayBtn = new Button("Detay", e -> detayDialogAc(r));
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

    private void detayDialogAc(Request request) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Talep Detayı");

    VerticalLayout icerik = new VerticalLayout();
    icerik.add(new Span("Başlık: " + request.getTitle()));
    icerik.add(new Span("Açıklama: " + request.getDescription()));
    icerik.add(new Span("Durum: " + durumMetni(request)));
    icerik.add(new Span("Tarih: " + DateUtil.format(request.getCreatedAt())));

    if (request.getStatus() == RequestStatus.REJECTED
            && request.getRejectionReason() != null) {
        Span ret = new Span("Ret Gerekçesi: " + request.getRejectionReason());
        ret.getStyle().set("color", "red");
        icerik.add(ret);
    }

    // Dosyalar
    List<RequestFile> dosyalar = requestFileService.getFilesByRequestId(request.getRequestId());
    if (!dosyalar.isEmpty()) {
        H4 dosyaBaslik = new H4("Ekli Dosyalar");
        icerik.add(dosyaBaslik);

        for (RequestFile dosya : dosyalar) {
            Anchor link = new Anchor(
                getDownloadUrl(dosya),
                dosya.getFileName() + " (" + formatFileSize(dosya.getFileSize()) + ")"
            );
            link.getElement().setAttribute("download", dosya.getFileName());
            icerik.add(link);
        }
    }

    icerik.add(mesajBolumu(request.getRequestId()));

    dialog.setWidth("520px");
    dialog.add(icerik);
    dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
    dialog.open();
}

private String getDownloadUrl(RequestFile dosya) {
    return "data:application/octet-stream;base64," +
        java.util.Base64.getEncoder().encodeToString(dosya.getFileData());
}

private String formatFileSize(Long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
    return (bytes / (1024 * 1024)) + " MB";
}

private VerticalLayout mesajBolumu(Long requestId) {
    VerticalLayout panel = new VerticalLayout();
    panel.setPadding(false);
    panel.setSpacing(false);
    panel.setWidthFull();

    H4 baslik = new H4("Mesajlar");
    baslik.getStyle().set("margin", "12px 0 6px 0");

    VerticalLayout liste = new VerticalLayout();
    liste.setPadding(false);
    liste.setSpacing(false);
    liste.setWidthFull();
    liste.getStyle()
        .set("max-height", "240px").set("overflow-y", "auto")
        .set("background", "#f8f9fa").set("border-radius", "6px").set("padding", "8px");

    Runnable yukle = () -> {
        liste.removeAll();
        List<RequestMessage> mesajlar = requestMessageService.getMessages(requestId);
        if (mesajlar.isEmpty()) {
            Span yok = new Span("Henüz mesaj yok.");
            yok.getStyle().set("color", "#888").set("font-size", "12px");
            liste.add(yok);
        } else {
            mesajlar.forEach(m -> liste.add(mesajBalonu(m)));
        }
    };
    yukle.run();

    TextArea girdi = new TextArea();
    girdi.setPlaceholder("Mesaj yazın...");
    girdi.setWidthFull();
    girdi.setMaxHeight("100px");

    Button gonder = new Button("Gönder", e -> {
        try {
            requestMessageService.sendMessage(requestId, currentUserId, girdi.getValue());
            girdi.clear();
            yukle.run();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    });
    gonder.getStyle().set("background-color", "#1B2A3B").set("color", "white");

    HorizontalLayout gonderSatir = new HorizontalLayout(girdi, gonder);
    gonderSatir.setWidthFull();
    gonderSatir.setAlignItems(Alignment.END);
    gonderSatir.expand(girdi);

    panel.add(baslik, liste, gonderSatir);
    return panel;
}

private Div mesajBalonu(RequestMessage m) {
    User sender = userService.findById(m.getSenderId()).orElse(null);
    String ad = sender != null ? sender.getNameSurname() : "Bilinmeyen";
    String rol = sender != null ? rolKisa(sender.getRole()) : "";
    boolean benim = currentUserId != null && currentUserId.equals(m.getSenderId());

    Div balon = new Div();
    balon.getStyle()
        .set("background", benim ? "#d1e7ff" : "#ffffff")
        .set("border", "1px solid #e0e0e0").set("border-radius", "6px")
        .set("padding", "6px 10px").set("max-width", "75%");

    Span ust = new Span(ad + " · " + rol + " · " + DateUtil.format(m.getCreatedAt()));
    ust.getStyle()
        .set("font-size", "11px").set("color", "#666")
        .set("font-weight", "bold").set("display", "block").set("margin-bottom", "2px");

    Span govde = new Span(m.getBody());
    govde.getStyle().set("white-space", "pre-wrap").set("font-size", "13px");

    balon.add(ust, govde);

    Div satir = new Div(balon);
    satir.getStyle()
        .set("display", "flex")
        .set("justify-content", benim ? "flex-end" : "flex-start")
        .set("width", "100%")
        .set("margin-bottom", "6px");
    return satir;
}

private String rolKisa(Role role) {
    return switch (role) {
        case CUSTOMER      -> "Müşteri";
        case PRODUCT_OWNER -> "Ürün Sorumlusu";
        case DEVELOPER     -> "Geliştirici";
        case SCRUM_MASTER  -> "Scrum Master";
        case ADMIN         -> "Admin";
    };
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