package com.example.views;

import com.example.enums.*;
import com.example.prioritization.Prioritization;
import com.example.prioritization.PrioritizationService;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;

@Route("po")
@PageTitle("Ürün Sorumlusu Paneli")
@RolesAllowed("PRODUCT_OWNER")
public class POView extends HorizontalLayout {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final WorkflowService workflowService;
    private final UserService userService;
    private final RequestFileService requestFileService;
    private final com.example.company.CompanyService companyService;

    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public POView(RequestService requestService,
                  PrioritizationService prioritizationService,
                  WorkflowService workflowService,
                  UserService userService, RequestFileService requestFileService,
                  com.example.company.CompanyService companyService) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.workflowService = workflowService;
        this.userService = userService;
        this.requestFileService = requestFileService;
        this.companyService = companyService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> currentUserName = u.getNameSurname());

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showOnceliklendirmeHavuzu();
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

        Span altBaslik = new Span("Ürün Sorumlusu Yönetim Paneli");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle()
            .set("color", "#aaaaaa")
            .set("margin-bottom", "8px")
            .set("margin-top", "24px");

        Button gelenTaleplerBtn = menuButton("Gelen Talepler");
        Button oncelikHavuzuBtn = menuButton("Talep Sıralaması");
        Button isAkislariBtn    = menuButton("İş Akışları (Sprint)");

        gelenTaleplerBtn.addClickListener(e -> showGelenTalepler());
        oncelikHavuzuBtn.addClickListener(e -> showOnceliklendirmeHavuzu());
        isAkislariBtn.addClickListener(e -> showIsAkislari());

        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Ürün Sorumlusu)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, menuBaslik,
            gelenTaleplerBtn, oncelikHavuzuBtn, isAkislariBtn);
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

    private void showGelenTalepler() {
        mainContent.removeAll();

        H2 baslik = new H2("Gelen Talepler");
        Paragraph aciklama = new Paragraph(
            "Yeni ve inceleme aşamasındaki talepler. İncelemeye alıp doğrudan önceliklendirin veya reddedin.");

        grid.removeAllColumns();
        grid.addColumn(r -> "#" + r.getRequestId()).setHeader("ID").setWidth("80px")
            .setComparator(Comparator.comparingLong(Request::getRequestId));
        grid.addColumn(r -> musteri(r.getCustomerId())).setHeader("Müşteri").setAutoWidth(true)
            .setComparator((a, b) -> musteri(a.getCustomerId()).compareTo(musteri(b.getCustomerId())));
        grid.addColumn(r -> sirketAdi(r.getCustomerId())).setHeader("Şirket").setAutoWidth(true)
            .setComparator((a, b) -> sirketAdi(a.getCustomerId()).compareTo(sirketAdi(b.getCustomerId())));
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true)
            .setComparator(Comparator.comparing(Request::getTitle));
        grid.addColumn(r -> DateUtil.format(r.getCreatedAt())).setHeader("Tarih")
            .setComparator(Comparator.comparing(Request::getCreatedAt));
        grid.addComponentColumn(r -> gelenTalepDurumBadge(r.getStatus())).setHeader("Durum")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::kopyaUyariBadge).setHeader("Kopya")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(r -> gelenTalepIslemButonlari(r)).setHeader("İşlem")
            .setAutoWidth(true).setFlexGrow(0);
        grid.setWidthFull();

        List<Request> talepler = new ArrayList<>();
        talepler.addAll(requestService.getByStatus(RequestStatus.NEW));
        talepler.addAll(requestService.getByStatus(RequestStatus.UNDER_REVIEW));
        requestService.getByStatus(RequestStatus.PRIORITIZED).stream()
            .filter(r -> workflowService.findByRequestId(r.getRequestId()).isEmpty())
            .forEach(talepler::add);
        talepler.sort(Comparator.comparing(Request::getCreatedAt));

        var arama = GridSearch.create(grid, talepler,
            "Ara: talep, müşteri, şirket, tarih...", this::talepAranabilir);

        grid.setItems(talepler);

        mainContent.add(baslik, aciklama, arama, grid);
    }

    private Span gelenTalepDurumBadge(RequestStatus status) {
        String label = switch (status) {
            case NEW          -> "Yeni";
            case UNDER_REVIEW -> "İncelemede";
            case PRIORITIZED  -> "Önceliklendirildi";
            default           -> status.name();
        };
        Span badge = new Span(label);
        badge.getStyle()
            .set("padding", "4px 8px").set("border-radius", "4px")
            .set("font-size", "12px").set("font-weight", "bold");
        switch (status) {
            case NEW          -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case UNDER_REVIEW -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case PRIORITIZED  -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            default           -> {}
        }
        return badge;
    }

    private HorizontalLayout gelenTalepIslemButonlari(Request r) {
        HorizontalLayout layout = new HorizontalLayout();
        if (r.getStatus() == RequestStatus.NEW) {
            Button incelemeBtn = new Button("İncelemeye Al", e -> {
                try {
                    requestService.takeUnderReview(r.getRequestId());
                    showGelenTalepler();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            incelemeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            layout.add(incelemeBtn);
        } else if (r.getStatus() == RequestStatus.UNDER_REVIEW) {
            Button oncelikBtn = new Button("Önceliklendir",
                e -> onceliklendirmeDialogAc(r, this::showGelenTalepler));
            oncelikBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

            Button reddetBtn = new Button("Reddet", e -> reddetDialogAc(r, this::showGelenTalepler));
            reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            layout.add(oncelikBtn, reddetBtn);
        } else if (r.getStatus() == RequestStatus.PRIORITIZED) {
            Button isAkisiBtn = new Button("İş Akışına Çevir", e -> {
                try {
                    workflowService.createWorkflow(r.getRequestId());
                    Notification.show("Talep iş akışına alındı.", 3000, Notification.Position.TOP_CENTER);
                    showGelenTalepler();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            isAkisiBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            layout.add(isAkisiBtn);
        }
        return layout;
    }

    /** Aynı şirketten benzer başlıklı talep varsa uyarı rozeti; tıklanınca birleştirme dialog'u açılır. */
    private com.vaadin.flow.component.Component kopyaUyariBadge(Request r) {
        List<Request> kopyalar = requestService.findPotentialDuplicates(r);
        if (kopyalar.isEmpty()) {
            Span bos = new Span("-");
            bos.getStyle().set("color", "#bbb");
            return bos;
        }
        Button uyari = new Button("⚠ Olası kopya (" + kopyalar.size() + ")",
            e -> mergeDialogAc(r, kopyalar));
        uyari.getStyle()
            .set("background", "#fff3cd").set("color", "#856404")
            .set("font-size", "12px").set("font-weight", "bold")
            .set("border", "1px solid #ffe08a").set("cursor", "pointer");
        return uyari;
    }

    private void mergeDialogAc(Request secili, List<Request> kopyalar) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Olası Kopya Talepler");
        dialog.setWidth("560px");

        VerticalLayout icerik = new VerticalLayout();
        icerik.setPadding(false);
        icerik.setSpacing(true);

        Paragraph aciklama = new Paragraph(
            "Aynı şirketten aynı başlıkla gelen talepler. Ana talebi seçin; diğerleri ona "
            + "bağlanıp 'Birleştirildi' durumuna geçecek. İş akışı ve önceliklendirme yalnızca "
            + "ana talep üzerinden yürür.");
        aciklama.getStyle().set("font-size", "13px").set("color", "#555");

        // Grup = seçili talep + kopyaları
        List<Request> grup = new ArrayList<>();
        grup.add(secili);
        grup.addAll(kopyalar);
        grup.sort(Comparator.comparing(Request::getCreatedAt));

        RadioButtonGroup<Request> anaSecim = new RadioButtonGroup<>();
        anaSecim.setLabel("Ana talep (korunacak)");
        anaSecim.setItems(grup);
        anaSecim.setItemLabelGenerator(r ->
            "#" + r.getRequestId() + " — " + musteri(r.getCustomerId())
            + " (" + DateUtil.format(r.getCreatedAt()) + ")");
        anaSecim.setValue(grup.get(0)); // en eski talep varsayılan ana talep
        anaSecim.getStyle().set("margin-top", "8px");

        icerik.add(aciklama, anaSecim);

        Button birlestirBtn = new Button("Birleştir", e -> {
            Request ana = anaSecim.getValue();
            if (ana == null) {
                Notification.show("Ana talebi seçiniz.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                int sayac = 0;
                for (Request r : grup) {
                    if (!r.getRequestId().equals(ana.getRequestId())) {
                        requestService.mergeDuplicate(ana.getRequestId(), r.getRequestId());
                        sayac++;
                    }
                }
                Notification.show(sayac + " talep #" + ana.getRequestId() + " ile birleştirildi.",
                    3000, Notification.Position.TOP_CENTER);
                dialog.close();
                showGelenTalepler();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        birlestirBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        dialog.add(icerik);
        dialog.getFooter().add(iptalBtn, birlestirBtn);
        dialog.open();
    }

    private void showOnceliklendirmeHavuzu() {
        mainContent.removeAll();

        H2 baslik = new H2("Talep Sıralaması");
        Paragraph aciklama = new Paragraph(
            "Tüm aktif talepler öncelik skoruna göre sıralanmaktadır. Not: Geliştirici çaba tahmini Scrum Master tarafından girilecektir.");

        grid.removeAllColumns();

        grid.addColumn(r -> "#" + r.getRequestId())
            .setHeader("ID").setWidth("80px")
            .setComparator(Comparator.comparingLong(Request::getRequestId));

        grid.addColumn(r -> musteri(r.getCustomerId()))
            .setHeader("Müşteri").setAutoWidth(true)
            .setComparator((a, b) -> musteri(a.getCustomerId()).compareTo(musteri(b.getCustomerId())));

        grid.addColumn(r -> sirketAdi(r.getCustomerId()))
            .setHeader("Şirket").setAutoWidth(true)
            .setComparator((a, b) -> sirketAdi(a.getCustomerId()).compareTo(sirketAdi(b.getCustomerId())));

        grid.addColumn(Request::getTitle)
            .setHeader("Başlık").setAutoWidth(true)
            .setComparator(Comparator.comparing(Request::getTitle));

        var skorColumn = grid.addComponentColumn(r -> skorBadge(r))
            .setHeader("Skor")
            .setComparator(Comparator.comparingInt(r ->
                prioritizationService.findByRequestId(r.getRequestId())
                    .map(Prioritization::getPriorityScore).orElse(0)));

        grid.addComponentColumn(r -> {
            Button detayBtn = new Button("Detay", e -> talepDetayDialogAc(r));
            detayBtn.getStyle().set("font-size", "12px");
            return detayBtn;
        }).setHeader("Detay").setWidth("90px").setFlexGrow(0);

        grid.addComponentColumn(r -> islemButonlari(r))
            .setHeader("İşlem")
            .setComparator(Comparator.comparingInt(r -> pipelineSirasi(r)))
            .setAutoWidth(true).setFlexGrow(0);
        grid.setWidthFull();

        List<Request> aktifTalepler = requestService.getAllActiveRequests();
        var arama = GridSearch.create(grid, aktifTalepler,
            "Ara: talep, müşteri, şirket, tarih...", this::talepAranabilir);

        grid.setItems(aktifTalepler);
        grid.sort(List.of(new GridSortOrder<>(skorColumn, SortDirection.DESCENDING)));

        mainContent.add(baslik, aciklama, arama, grid);
    }

    private void showIsAkislari() {
        mainContent.removeAll();

        H2 baslik = new H2("İş Akışları (Sprint)");

        Grid<Request> isAkisiGrid = new Grid<>(Request.class, false);
        isAkisiGrid.addColumn(r -> "#" + r.getRequestId()).setHeader("ID").setWidth("80px");
        isAkisiGrid.addColumn(r -> musteri(r.getCustomerId())).setHeader("Müşteri").setAutoWidth(true);
        isAkisiGrid.addColumn(r -> sirketAdi(r.getCustomerId())).setHeader("Şirket").setAutoWidth(true);
        isAkisiGrid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        isAkisiGrid.addComponentColumn(r ->
            workflowService.findByRequestId(r.getRequestId())
                .map(w -> workflowBadge(w.getWorkflowStatus()))
                .orElse(new Span("-"))
        ).setHeader("Durum");
        isAkisiGrid.setWidthFull();

        List<Request> isAkisindakiler = requestService.getAllActiveRequests().stream()
            .filter(r -> workflowService.findByRequestId(r.getRequestId()).isPresent())
            .toList();

        var arama = GridSearch.create(isAkisiGrid, isAkisindakiler,
            "Ara: talep, müşteri, şirket, tarih...", this::talepAranabilir);

        isAkisiGrid.setItems(isAkisindakiler);

        mainContent.add(baslik, arama, isAkisiGrid);
    }

    private Span skorBadge(Request r) {
    return prioritizationService.findByRequestId(r.getRequestId())
        .map(p -> {
            // Çaba tahmini henüz girilmemiş
            if (p.getGelistiriciMudahalesi() == null) {
                Span badge = new Span("Çaba Tahmini Bekleniyor");
                badge.getStyle()
                    .set("padding", "4px 8px")
                    .set("border-radius", "4px")
                    .set("background", "#fff3cd")
                    .set("color", "#856404");
                return badge;
            }

            int skor = p.getPriorityScore();
            String label = prioritizationService.getLabel(skor);
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
            Span badge = new Span("Henuz Skorlanmadi");
            badge.getStyle()
                .set("padding", "4px 8px")
                .set("border-radius", "4px")
                .set("background", "#e0e0e0")
                .set("color", "#666");
            return badge;
        });
}

    private HorizontalLayout islemButonlari(Request r) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setPadding(false);
        layout.getStyle().set("flex-wrap", "nowrap");

        if (r.getStatus() == RequestStatus.NEW) {
            Button btn = new Button("İncelemeye Al", e -> {
                try {
                    requestService.takeUnderReview(r.getRequestId());
                    showOnceliklendirmeHavuzu();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            layout.add(btn);

        } else if (r.getStatus() == RequestStatus.UNDER_REVIEW) {
            Button oncelikBtn = new Button("Önceliklendir", e -> onceliklendirmeDialogAc(r));
            oncelikBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

            Button reddetBtn = new Button("Reddet", e -> reddetDialogAc(r));
            reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            layout.add(oncelikBtn, reddetBtn);

        } else if (r.getStatus() == RequestStatus.PRIORITIZED) {
    workflowService.findByRequestId(r.getRequestId()).ifPresentOrElse(
        w -> {
            if (w.getWorkflowStatus() == WorkflowStatus.DONE) {
                layout.add(workflowBadge(WorkflowStatus.DONE));
            } else {
                // Çaba tahmini girildi mi kontrol et
                boolean cabaGirildi = prioritizationService
                    .findByRequestId(r.getRequestId())
                    .map(p -> p.getGelistiriciMudahalesi() != null)
                    .orElse(false);

                VerticalLayout durumLayout = new VerticalLayout();
                durumLayout.setPadding(false);
                durumLayout.setSpacing(false);

                durumLayout.add(workflowBadge(w.getWorkflowStatus()));

                if (!cabaGirildi) {
                    Span bekliyor = new Span("⏳ Çaba tahmini bekleniyor");
                    bekliyor.getStyle()
                        .set("color", "#856404")
                        .set("font-size", "12px")
                        .set("font-style", "italic");
                    durumLayout.add(bekliyor);
                }

                layout.add(durumLayout);
            }
        },
        () -> {
            Button isAkisiBtn = new Button("İş Akışına Çevir", e -> {
                try {
                    workflowService.createWorkflow(r.getRequestId());
                    Notification.show("Talep iş akışına alındı.",
                        3000, Notification.Position.TOP_CENTER);
                    showOnceliklendirmeHavuzu();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            isAkisiBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
            layout.add(isAkisiBtn);
        }
    );
}

        return layout;
    }

    private void onceliklendirmeDialogAc(Request request) {
        onceliklendirmeDialogAc(request, this::showOnceliklendirmeHavuzu);
    }

    private void onceliklendirmeDialogAc(Request request, Runnable onSuccess) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Önceliklendirme — #" + request.getRequestId());
        dialog.setWidth("550px");

        int musteriDegeriPuan = userService.getMusteriDegeriPuan(request.getCustomerId());
        String musteriDegeriLabel = switch (musteriDegeriPuan) {
            case 5 -> "VIP (5)";
            case 4 -> "Büyük (4)";
            case 3 -> "Orta (3)";
            case 2 -> "Küçük (2)";
            default -> "İç Kullanıcı (1)";
        };

        Span talepBaslikSpan = new Span("Talep: " + request.getTitle());
        talepBaslikSpan.getStyle().set("font-weight", "bold");

        Span beklemeSuresi = new Span("Bekleme Süresi Puanı: " +
            prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));

        Span musteriDegeriSpan = new Span("Şirket Değeri: " + musteriDegeriLabel);
        musteriDegeriSpan.getStyle().set("color", "#2C6FAC").set("font-weight", "bold");

        // Requester Credibility (güvenilirlik) — talep sahibinin geçmiş performansı
        var credStats = requestService.getCredibilityStats(request.getCustomerId());
        int credibilityScore = prioritizationService.calculateCredibilityScore(request.getCustomerId());
        String credText;
        if (credStats.total() < 5) {
            credText = String.format(
                "Güvenilirlik Skoru: 0 (yetersiz geçmiş — toplam %d talep, en az 5 gerekir)",
                credStats.total());
        } else {
            credText = String.format(
                "Güvenilirlik Skoru: %+d  (Toplam %d Onaylanan %d Reddedilen %d)",
                credibilityScore, credStats.total(), credStats.approved(), credStats.rejected());
        }
        Span credibilitySpan = new Span(credText);
        credibilitySpan.getStyle().set("font-weight", "bold");
        if (credibilityScore > 0)      credibilitySpan.getStyle().set("color", "#1e7e34"); // ödül
        else if (credibilityScore < 0) credibilitySpan.getStyle().set("color", "#c0392b"); // ceza
        else                           credibilitySpan.getStyle().set("color", "#666");

        Span smNotu = new Span("Not: Geliştirici çaba tahmini Scrum Master tarafından girilecektir.");
        smNotu.getStyle().set("color", "#888").set("font-size", "12px").set("font-style", "italic");

        ComboBox<Integer> isEtkisiBox = new ComboBox<>("İş Etkisi");
        isEtkisiBox.setItems(1, 2, 3, 4, 5);
        isEtkisiBox.setItemLabelGenerator(v -> switch (v) {
            case 5 -> "5 - Sistem tamamen çalışmıyor";
            case 4 -> "4 - Kritik iş süreci etkileniyor";
            case 3 -> "3 - Kısmi etki, geçici çözüm var";
            case 2 -> "2 - Küçük etki, işler yavaşlıyor";
            default -> "1 - Kozmetik / Görsel";
        });
        isEtkisiBox.setWidthFull();

        ComboBox<Integer> acilyetBox = new ComboBox<>("Aciliyet");
        acilyetBox.setItems(1, 2, 3, 4, 5);
        acilyetBox.setItemLabelGenerator(v -> switch (v) {
            case 5 -> "5 - Bugün çözülmeli";
            case 4 -> "4 - Bu hafta içinde";
            case 3 -> "3 - Bu ay içinde";
            case 2 -> "2 - Önümüzdeki sprint'e planlanabilir";
            default -> "1 - Esnek, zaman bağımsız";
        });
        acilyetBox.setWidthFull();

        ComboBox<IsTipi> isTipiBox = new ComboBox<>("İş Tipi");
        isTipiBox.setItems(IsTipi.values());
        isTipiBox.setItemLabelGenerator(v -> switch (v) {
            case GUVENLIK_ACIGI  -> "Güvenlik Açığı";
            case KRITIK_BUG      -> "Kritik Bug";
            case BUG             -> "Bug";
            case PERFORMANS      -> "Performans Sorunu";
            case ENTEGRASYON     -> "Entegrasyon";
            case FEATURE_REQUEST -> "Yeni Özellik";
            case ENHANCEMENT     -> "İyileştirme";
            case DOKUMANTASYON   -> "Dokümantasyon";
        });
        isTipiBox.setWidthFull();

        ComboBox<YoneticiTakdiri> takdirBox = new ComboBox<>("Yönetici Takdiri");
        takdirBox.setItems(YoneticiTakdiri.values());
        takdirBox.setItemLabelGenerator(v -> switch (v) {
            case YOK       -> "Yok / Normal (0)";
            case ONEMLI    -> "Önemli — Birim Hedefi (+5)";
            case STRATEJIK -> "Stratejik — Şirket Hedefi (+10)";
            case KRITIK    -> "Kritik — Acil Müdahale (+15)";
        });
        takdirBox.setValue(YoneticiTakdiri.YOK);
        takdirBox.setWidthFull();

        Span skorSpan  = new Span("Tahmini Skor: —");
        Span labelSpan = new Span("");
        skorSpan.getStyle().set("font-weight", "bold").set("font-size", "16px");

        Runnable skorGuncelle = () -> {
            if (isEtkisiBox.getValue() != null && acilyetBox.getValue() != null
                    && isTipiBox.getValue() != null && takdirBox.getValue() != null) {

                Prioritization temp = new Prioritization();
                temp.setIsEtkisi(isEtkisiBox.getValue());
                temp.setAciliyet(acilyetBox.getValue());
                temp.setMusteriDegeriPuan(musteriDegeriPuan);
                temp.setIsTipi(isTipiBox.getValue());
                temp.setIsTimiPuan(isTipiBox.getValue().getPuan());
                temp.setBeklemeSuresiPuan(
                    prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));
                temp.setGelistiriciMudahalesi(GelistiriciMudahalesi.ORTA);

                int credibility = prioritizationService.calculateCredibilityScore(request.getCustomerId());
                int skor = prioritizationService.calculateFinalScore(temp, takdirBox.getValue(), credibility);
                skorSpan.setText("Tahmini Skor: " + skor);
                labelSpan.setText(" — " + prioritizationService.getLabel(skor));
            }
        };

        isEtkisiBox.addValueChangeListener(e -> skorGuncelle.run());
        acilyetBox.addValueChangeListener(e -> skorGuncelle.run());
        isTipiBox.addValueChangeListener(e -> skorGuncelle.run());
        takdirBox.addValueChangeListener(e -> skorGuncelle.run());

        Button kaydetBtn = new Button("Değerleri Kaydet", e -> {
            if (isEtkisiBox.getValue() == null || acilyetBox.getValue() == null
                    || isTipiBox.getValue() == null || takdirBox.getValue() == null) {
                Notification.show("Tüm alanları seçiniz.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                Prioritization p = new Prioritization();
                p.setRequestId(request.getRequestId());
                p.setIsEtkisi(isEtkisiBox.getValue());
                p.setAciliyet(acilyetBox.getValue());
                p.setIsTipi(isTipiBox.getValue());

                requestService.updateYoneticiTakdiri(request.getRequestId(), takdirBox.getValue());

                prioritizationService.savePrioritizationByPO(
                    p, request.getCustomerId(), request.getCreatedAt());

                Notification.show("Önceliklendirme kaydedildi. Scrum Master çaba tahminini girecektir.",
                    4000, Notification.Position.TOP_CENTER);
                dialog.close();
                onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        kaydetBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        // Detayları/dosyaları gördükten sonra doğrudan reddetme seçeneği
        Button reddetBtn = new Button("Reddet", e -> {
            dialog.close();
            reddetDialogAc(request);
        });
        reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout skorLayout = new HorizontalLayout(skorSpan, labelSpan);
        skorLayout.setAlignItems(Alignment.BASELINE);

        // Dosyalar
List<RequestFile> dosyalar = requestFileService.getFilesByRequestId(request.getRequestId());
VerticalLayout dosyaLayout = new VerticalLayout();
dosyaLayout.setPadding(false);
dosyaLayout.setSpacing(false);

if (!dosyalar.isEmpty()) {
    H4 dosyaBaslik = new H4("Ekli Dosyalar");
    dosyaBaslik.getStyle().set("margin-bottom", "4px");
    dosyaLayout.add(dosyaBaslik);

    for (RequestFile dosya : dosyalar) {
        Anchor link = new Anchor(
            "data:application/octet-stream;base64," +
            java.util.Base64.getEncoder().encodeToString(dosya.getFileData()),
            "📎 " + dosya.getFileName() + " (" + formatFileSize(dosya.getFileSize()) + ")"
        );
        link.getElement().setAttribute("download", dosya.getFileName());
        link.getStyle().set("display", "block").set("margin-bottom", "4px");
        dosyaLayout.add(link);
    }
} else {
    Span yok = new Span("Ekli dosya yok.");
    yok.getStyle().set("color", "#888").set("font-size", "12px");
    dosyaLayout.add(yok);
}
      VerticalLayout icerik = new VerticalLayout(
    talepBaslikSpan, beklemeSuresi, musteriDegeriSpan, credibilitySpan, smNotu,
    dosyaLayout,                    // bunu ekle
    isEtkisiBox, acilyetBox, isTipiBox,
    takdirBox, skorLayout
);
            icerik.setPadding(false);

        dialog.add(icerik);
        dialog.getFooter().add(iptalBtn, reddetBtn, kaydetBtn);
        dialog.open();
    }

    private Span workflowBadge(WorkflowStatus status) {
        String label = switch (status) {
            case BACKLOG     -> "Backlog";
            case IN_PROGRESS -> "Devam Ediyor";
            case TESTING     -> "Test Aşamasında";
            case DONE        -> "✓ Tamamlandı";
        };
        Span badge = new Span(label);
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px")
            .set("font-size", "12px").set("font-weight", "bold");
        switch (status) {
            case BACKLOG     -> badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
            case IN_PROGRESS -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case TESTING     -> badge.getStyle().set("background", "#ffe8cc").set("color", "#7d3c00");
            case DONE        -> badge.getStyle().set("background", "#d4edda").set("color", "#155724");
        }
        return badge;
    }

    private void reddetDialogAc(Request request) {
        reddetDialogAc(request, this::showOnceliklendirmeHavuzu);
    }

    private void reddetDialogAc(Request request, Runnable onSuccess) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talebi Reddet — #" + request.getRequestId());

        TextArea gerekcaField = new TextArea("Ret Gerekçesi");
        gerekcaField.setWidthFull();
        gerekcaField.setMinHeight("120px");

        Button reddetBtn = new Button("Reddet", e -> {
            try {
                requestService.rejectRequest(request.getRequestId(), gerekcaField.getValue());
                Notification.show("Talep reddedildi.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        dialog.add(new VerticalLayout(gerekcaField));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), reddetBtn);
        dialog.open();
    }
    private String formatFileSize(Long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
    return (bytes / (1024 * 1024)) + " MB";
}


    private void talepDetayDialogAc(Request r) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Detayı — #" + r.getRequestId());
        dialog.setWidth("520px");

        VerticalLayout icerik = new VerticalLayout();
        icerik.setPadding(false);
        icerik.setSpacing(true);

        Span musteriSpan = new Span("Müşteri: " + musteri(r.getCustomerId()));
        musteriSpan.getStyle().set("color", "#555").set("font-size", "13px");

        Span tarihSpan = new Span("Tarih: " + DateUtil.format(r.getCreatedAt()));
        tarihSpan.getStyle().set("color", "#555").set("font-size", "13px");

        H4 baslikLabel = new H4(r.getTitle());
        baslikLabel.getStyle().set("margin", "8px 0 4px 0");

        Paragraph aciklama = new Paragraph(r.getDescription());
        aciklama.getStyle()
            .set("background", "#f8f9fa")
            .set("border-radius", "6px")
            .set("padding", "12px")
            .set("font-size", "13px")
            .set("white-space", "pre-wrap");

        icerik.add(musteriSpan, tarihSpan, baslikLabel, aciklama);

        List<RequestFile> dosyalar = requestFileService.getFilesByRequestId(r.getRequestId());
        if (!dosyalar.isEmpty()) {
            H4 dosyaBaslik = new H4("Ekli Dosyalar");
            dosyaBaslik.getStyle().set("margin-bottom", "4px");
            icerik.add(dosyaBaslik);
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
        }

        dialog.add(icerik);
        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }

    private int pipelineSirasi(Request r) {
        return switch (r.getStatus()) {
            case NEW          -> 0;
            case UNDER_REVIEW -> 1;
            case REJECTED     -> 7;
            case DUPLICATE    -> 6;
            case PRIORITIZED  -> workflowService.findByRequestId(r.getRequestId())
                .map(w -> switch (w.getWorkflowStatus()) {
                    case BACKLOG     -> 2;
                    case IN_PROGRESS -> 3;
                    case TESTING     -> 4;
                    case DONE        -> 5;
                })
                .orElse(2);
        };
    }

    private String musteri(Long customerId) {
        return userService.findById(customerId)
            .map(User::getNameSurname)
            .orElse("Bilinmiyor");
    }

    private String sirketAdi(Long customerId) {
        return userService.findById(customerId)
            .map(u -> companyService.getName(u.getCompanyId()))
            .orElse("-");
    }

    /** Bir talep için aranabilir metin: id, müşteri, şirket, başlık, tarih. */
    private String talepAranabilir(Request r) {
        return "#" + r.getRequestId()
            + " " + musteri(r.getCustomerId())
            + " " + sirketAdi(r.getCustomerId())
            + " " + r.getTitle()
            + " " + DateUtil.format(r.getCreatedAt());
    }
}