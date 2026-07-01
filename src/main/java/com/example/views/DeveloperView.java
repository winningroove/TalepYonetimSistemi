package com.example.views;

import com.example.enums.WorkflowStatus;
import com.example.prioritization.PrioritizationService;
import com.example.request.Request;
import com.example.request.RequestFile;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
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

@Route("developer")
@PageTitle("Geliştirici Paneli")
@RolesAllowed("DEVELOPER")
public class DeveloperView extends HorizontalLayout {

    private final WorkflowService workflowService;
    private final RequestService requestService;
    private final UserService userService;
    private final RequestFileService requestFileService;
    private final PrioritizationService prioritizationService;
    private final com.example.company.CompanyService companyService;
    private final com.example.notification.NotificationService notificationService;
    private final com.example.notification.NotificationBroadcaster notificationBroadcaster;
    private final com.example.message.RequestMessageService requestMessageService;

    private Long currentUserId;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();

    public DeveloperView(WorkflowService workflowService,
                         RequestService requestService,
                         UserService userService,
                         RequestFileService requestFileService,
                         PrioritizationService prioritizationService,
                         com.example.company.CompanyService companyService,
                         com.example.notification.NotificationService notificationService,
                         com.example.notification.NotificationBroadcaster notificationBroadcaster,
                         com.example.message.RequestMessageService requestMessageService) {
        this.workflowService = workflowService;
        this.requestService = requestService;
        this.userService = userService;
        this.requestFileService = requestFileService;
        this.prioritizationService = prioritizationService;
        this.companyService = companyService;
        this.notificationService = notificationService;
        this.notificationBroadcaster = notificationBroadcaster;
        this.requestMessageService = requestMessageService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserId = u.getUserId();
            currentUserName = u.getNameSurname();
        });

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showGorevlerim();
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

        Span altBaslik = new Span("Geliştirici Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        HorizontalLayout bildirimSatir = new HorizontalLayout(
            new Span("Bildirimler"),
            new com.example.notification.NotificationBell(notificationService, notificationBroadcaster, currentUserId,
                reqId -> workflowService.findByRequestId(reqId).ifPresent(this::gorevDetayDialogAc)));
        bildirimSatir.setAlignItems(Alignment.CENTER);
        bildirimSatir.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("margin-top", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle()
            .set("color", "#aaaaaa")
            .set("margin-bottom", "8px")
            .set("margin-top", "24px");

        Button gorevlerimBtn = menuButton("Görevlerim");
        Button tamamlananBtn = menuButton("Tamamlanan Görevler");

        gorevlerimBtn.addClickListener(e -> showGorevlerim());
        tamamlananBtn.addClickListener(e -> showTamamlananlar());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Geliştirici)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, bildirimSatir, menuBaslik, gorevlerimBtn, tamamlananBtn);
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

   private void showGorevlerim() {
    mainContent.removeAll();

    H2 baslik = new H2("Görevlerim");
    Paragraph aciklama = new Paragraph("Size atanan aktif görevler. En yüksek öncelikli görevler üstte listelenir.");

    Grid<Workflow> grid = new Grid<>(Workflow.class, false);
    grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
    grid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
    grid.addComponentColumn(w -> {
        return prioritizationService.findByRequestId(w.getRequestId())
            .map(p -> {
                if (p.getGelistiriciMudahalesi() == null) {
                    Span badge = new Span("Hesaplanmadı");
                    badge.getStyle()
                        .set("padding", "4px 8px")
                        .set("border-radius", "4px")
                        .set("background", "#fff3cd")
                        .set("color", "#856404");
                    return badge;
                }
                int skor = p.getPriorityScore();
                String label;
                if (skor >= 85)      label = "Kritik";
                else if (skor >= 69) label = "Yüksek";
                else if (skor >= 53) label = "Orta";
                else if (skor >= 37) label = "Düşük";
                else                 label = "Çok Düşük";
                Span badge = new Span(skor + " (" + label + ")");
                badge.getStyle()
                    .set("padding", "4px 8px")
                    .set("border-radius", "4px")
                    .set("font-weight", "bold");
                if (skor >= 85)      badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
                else if (skor >= 69) badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
                else if (skor >= 53) badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
                else                 badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
                return badge;
            })
            .orElseGet(() -> {
                Span badge = new Span("-");
                badge.getStyle().set("color", "#888");
                return badge;
            });
    }).setHeader("Öncelik");
    grid.addComponentColumn(this::durumGuncelleButonu).setHeader("İşlem");
    grid.addComponentColumn(w -> {
        Button detayBtn = new Button("Detay", e -> gorevDetayDialogAc(w));
        detayBtn.getStyle().set("background-color", "#2C6FAC").set("color", "white");
        return detayBtn;
    }).setHeader("Detay");
    grid.setWidthFull();

    List<Workflow> gorevler = currentUserId != null
        ? new java.util.ArrayList<>(workflowService.getDeveloperWorkflows(currentUserId))
        : new java.util.ArrayList<>();
    gorevler.sort((a, b) -> {
        int skorA = prioritizationService.findByRequestId(a.getRequestId())
            .map(p -> p.getPriorityScore()).orElse(0);
        int skorB = prioritizationService.findByRequestId(b.getRequestId())
            .map(p -> p.getPriorityScore()).orElse(0);
        return Integer.compare(skorB, skorA);
    });

    var arama = GridSearch.create(grid, gorevler,
        "Ara: talep, şirket, durum, tarih...", this::gorevAranabilir);
    grid.setItems(gorevler);

    mainContent.add(baslik, aciklama, arama, grid);
}
   

    private void showTamamlananlar() {
        mainContent.removeAll();

        H2 baslik = new H2("Tamamlanan Görevler");
        Paragraph aciklama = new Paragraph("Tamamladığınız görevlerin listesi.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> sirketAdiByRequest(w.getRequestId())).setHeader("Şirket").setAutoWidth(true);
        grid.addComponentColumn(w -> {
            Span done = new Span("Tamamlandı");
            done.getStyle().set("color", "green").set("font-weight", "bold");
            return done;
        }).setHeader("Durum");
        grid.addColumn(w -> DateUtil.format(w.getUpdatedAt())).setHeader("Tamamlanma Tarihi");
        grid.setWidthFull();

        List<Workflow> tamamlananlar = currentUserId != null
            ? workflowService.getDoneWorkflowsByDeveloper(currentUserId)
            : List.of();

        var arama = GridSearch.create(grid, tamamlananlar,
            "Ara: talep, şirket, tarih...",
            w -> talepBasligi(w.getRequestId()) + " " + sirketAdiByRequest(w.getRequestId())
                + " " + DateUtil.format(w.getUpdatedAt()));
        grid.setItems(tamamlananlar);

        mainContent.add(baslik, aciklama, arama, grid);
    }

    private Button durumGuncelleButonu(Workflow workflow) {
    WorkflowStatus current = workflow.getWorkflowStatus();

    WorkflowStatus next = switch (current) {
        case BACKLOG     -> WorkflowStatus.IN_PROGRESS;
        case IN_PROGRESS -> WorkflowStatus.TESTING;
        case TESTING     -> WorkflowStatus.DONE;
        default          -> null;
    };

    if (next == null) {
        Button done = new Button("✓ Tamamlandı");
        done.getStyle().set("background-color", "#155724").set("color", "white").set("cursor", "default");
        done.setEnabled(false);
        return done;
    }

    String butonText = switch (next) {
        case IN_PROGRESS -> "▶ Başla";
        case TESTING     -> "Teste Gönder";
        case DONE        -> "✔ Tamamla";
        default          -> "";
    };

    String bgColor = switch (next) {
        case IN_PROGRESS -> "#1B6EC2";
        case TESTING     -> "#B45309";
        case DONE        -> "#166534";
        default          -> "#1B2A3B";
    };

    Button btn = new Button(butonText, e -> {
        try {
            workflowService.updateStatus(workflow.getTaskId(), next, workflow.getVersion());
            showGorevlerim();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    });
    btn.getStyle().set("background-color", bgColor).set("color", "white");
    return btn;
}
    

    private void gorevDetayDialogAc(Workflow workflow) {
        Dialog dialog = new Dialog();

        requestService.findById(workflow.getRequestId()).ifPresent(request -> {
            dialog.setHeaderTitle("Görev Detayı — " + request.getTitle());

            VerticalLayout icerik = new VerticalLayout();
            icerik.setPadding(false);

            icerik.add(new Span("Talep Başlığı: " + request.getTitle()));
            icerik.add(new Span("Açıklama: " + request.getDescription()));
            icerik.add(new Span("Durum: " + workflow.getWorkflowStatus()));
            icerik.add(new Span("Tarih: " + DateUtil.format(request.getCreatedAt())));

            List<RequestFile> dosyalar = requestFileService.getFilesByRequestId(request.getRequestId());

            H4 dosyaBaslik = new H4("Ekli Dosyalar");
            dosyaBaslik.getStyle().set("margin-top", "16px").set("margin-bottom", "4px");
            icerik.add(dosyaBaslik);

            if (!dosyalar.isEmpty()) {
                for (RequestFile dosya : dosyalar) {
                    Anchor link = new Anchor(
                        "data:application/octet-stream;base64," +
                        java.util.Base64.getEncoder().encodeToString(dosya.getFileData()),
                        "📎 " + dosya.getFileName() + " (" + formatFileSize(dosya.getFileSize()) + ")"
                    );
                    link.getElement().setAttribute("download", dosya.getFileName());
                    link.getStyle().set("display", "block").set("margin-bottom", "4px");
                    icerik.add(link);
                }
            } else {
                Span yok = new Span("Ekli dosya yok.");
                yok.getStyle().set("color", "#888").set("font-size", "12px");
                icerik.add(yok);
            }

            icerik.add(ekipMesajBolumu(request.getRequestId()));

            dialog.add(icerik);

            // Başlanmış görev (Devam/Test) Scrum Master'a geri gönderilebilir
            WorkflowStatus durum = workflow.getWorkflowStatus();
            if (durum == WorkflowStatus.IN_PROGRESS || durum == WorkflowStatus.TESTING) {
                Button geriBtn = new Button("⬅ Scrum Master'a Geri Gönder",
                    e -> geriGonderSMDialogAc(workflow, dialog));
                geriBtn.getStyle().set("background", "#fff3cd").set("color", "#856404");
                dialog.getFooter().add(geriBtn);
            }
        });

        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }

    /** Geliştirici görevi gerekçeyle Scrum Master'a geri gönderir. */
    private void geriGonderSMDialogAc(Workflow workflow, Dialog parent) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Scrum Master'a Geri Gönder");
        dialog.setWidth("460px");

        Paragraph aciklama = new Paragraph(
            "Görev tekrar 'Sırada' durumuna dönecek ve Scrum Master'a bildirilecek. "
            + "Gerekçe ekip notlarına kaydedilir.");
        aciklama.getStyle().set("font-size", "13px").set("color", "#555");

        TextArea gerekce = new TextArea("Gerekçe");
        gerekce.setWidthFull();
        gerekce.setMinHeight("120px");

        Button gonderBtn = new Button("Geri Gönder", e -> {
            try {
                workflowService.sendBackToScrumMaster(
                    workflow.getTaskId(), workflow.getVersion(), currentUserId, gerekce.getValue());
                Notification.show("Görev Scrum Master'a geri gönderildi.",
                    3000, Notification.Position.TOP_CENTER);
                dialog.close();
                parent.close();
                showGorevlerim();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        gonderBtn.getStyle().set("background-color", "#856404").set("color", "white");

        dialog.add(new VerticalLayout(aciklama, gerekce));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), gonderBtn);
        dialog.open();
    }

    /** Ekip içi (dahili) yorum kanalı — müşteri görmez; PO/SM/Geliştirici arası. */
    private VerticalLayout ekipMesajBolumu(Long requestId) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidthFull();

        H4 baslik = new H4("Ekip Notları (Dahili)");
        baslik.getStyle().set("margin", "12px 0 6px 0").set("color", "#856404");

        Span aciklama = new Span("Bu kanal müşteriye kapalıdır; yalnızca ürün sorumlusu, scrum master ve geliştiriciler görür.");
        aciklama.getStyle().set("font-size", "11px").set("color", "#888").set("display", "block").set("margin-bottom", "6px");

        VerticalLayout liste = new VerticalLayout();
        liste.setPadding(false);
        liste.setSpacing(false);
        liste.setWidthFull();
        liste.getStyle()
            .set("max-height", "240px").set("overflow-y", "auto")
            .set("background", "#fffdf5").set("border", "1px solid #ffe08a")
            .set("border-radius", "6px").set("padding", "8px");

        Runnable yukle = () -> {
            liste.removeAll();
            List<com.example.message.RequestMessage> mesajlar = requestMessageService.getInternalMessages(requestId);
            if (mesajlar.isEmpty()) {
                Span yok = new Span("Henüz ekip notu yok.");
                yok.getStyle().set("color", "#888").set("font-size", "12px");
                liste.add(yok);
            } else {
                mesajlar.forEach(m -> liste.add(mesajBalonu(m)));
            }
        };
        yukle.run();

        TextArea girdi = new TextArea();
        girdi.setPlaceholder("Ekip notu yazın...");
        girdi.setWidthFull();
        girdi.setMaxHeight("100px");

        Button gonder = new Button("Not Ekle", e -> {
            try {
                requestMessageService.sendInternalMessage(requestId, currentUserId, girdi.getValue());
                girdi.clear();
                yukle.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        gonder.getStyle().set("background-color", "#856404").set("color", "white");

        HorizontalLayout gonderSatir = new HorizontalLayout(girdi, gonder);
        gonderSatir.setWidthFull();
        gonderSatir.setAlignItems(Alignment.END);
        gonderSatir.expand(girdi);

        panel.add(baslik, aciklama, liste, gonderSatir);
        return panel;
    }

    private Div mesajBalonu(com.example.message.RequestMessage m) {
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

    private String rolKisa(com.example.enums.Role role) {
        return switch (role) {
            case CUSTOMER      -> "Müşteri";
            case PRODUCT_OWNER -> "Ürün Sorumlusu";
            case DEVELOPER     -> "Geliştirici";
            case SCRUM_MASTER  -> "Scrum Master";
            case ADMIN         -> "Admin";
        };
    }

    private String talepBasligi(Long requestId) {
        return requestService.findById(requestId)
            .map(Request::getTitle)
            .orElse("Bilinmiyor");
    }

    private String sirketAdiByRequest(Long requestId) {
        return requestService.findById(requestId)
            .flatMap(r -> userService.findById(r.getCustomerId()))
            .map(u -> companyService.getName(u.getCompanyId()))
            .orElse("-");
    }

    /** Görev için aranabilir metin: talep başlığı, şirket, durum, talep tarihi. */
    private String gorevAranabilir(Workflow w) {
        String tarih = requestService.findById(w.getRequestId())
            .map(r -> DateUtil.format(r.getCreatedAt())).orElse("");
        return talepBasligi(w.getRequestId())
            + " " + sirketAdiByRequest(w.getRequestId())
            + " " + w.getWorkflowStatus().name()
            + " " + tarih;
    }

    private String formatFileSize(Long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    private Span durumBadge(WorkflowStatus status) {
        Span badge = new Span(status.name());
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px");
        switch (status) {
            case BACKLOG     -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case IN_PROGRESS -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case TESTING     -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case DONE        -> badge.getStyle().set("background", "#d4edda").set("color", "#155724");
        }
        return badge;
    }
}