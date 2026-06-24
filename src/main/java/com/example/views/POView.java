package com.example.views;

import com.example.enums.*;
import com.example.model.*;
import com.example.service.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route("po")
@PageTitle("Ürün Sorumlusu Paneli")
@RolesAllowed("ROLE_PRODUCT_OWNER")
public class POView extends HorizontalLayout {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final WorkflowService workflowService;
    private final UserService userService;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public POView(RequestService requestService,
                  PrioritizationService prioritizationService,
                  WorkflowService workflowService,
                  UserService userService) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.workflowService = workflowService;
        this.userService = userService;

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
        menuBaslik.getStyle().set("color", "#aaaaaa").set("margin-bottom", "8px").set("margin-top", "24px");

        Button gelenTaleplerBtn  = menuButton("• Gelen Talepler");
        Button oncelikHavuzuBtn  = menuButton("• Önceliklendirme Havuzu");
        Button isAkislariBtn     = menuButton("• İş Akışları (Sprint)");

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
        sidebar.add(divider, girisYapan, kullaniciAdi);

        return sidebar;
    }

    private Button menuButton(String text) {
        Button btn = new Button(text);
        btn.getStyle()
            .set("color", "white")
            .set("background", "transparent")
            .set("border", "none")
            .set("text-align", "left")
            .set("width", "100%")
            .set("cursor", "pointer")
            .set("padding", "8px 0");
        return btn;
    }

    private VerticalLayout buildMainContent() {
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        return mainContent;
    }

    // ── Gelen Talepler (NEW statüsündekiler) ──
    private void showGelenTalepler() {
        mainContent.removeAll();

        H2 baslik = new H2("Gelen Talepler");
        Paragraph aciklama = new Paragraph("Müşterilerden gelen yeni talepler. İncelemeye almak için talebi seçin.");

        grid.removeAllColumns();
        grid.addColumn(r -> "#" + r.getRequestId()).setHeader("ID").setWidth("80px");
        grid.addColumn(r -> musteri(r.getCustomerId())).setHeader("Müşteri").setAutoWidth(true);
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addColumn(r -> r.getCreatedAt().toLocalDate()).setHeader("Tarih");
        grid.addComponentColumn(r -> {
            Button btn = new Button("İncelemeye Al", e -> {
                try {
                    requestService.takeUnderReview(r.getRequestId());
                    showGelenTalepler();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return btn;
        }).setHeader("İşlem");
        grid.setWidthFull();
        grid.setItems(requestService.getByStatus(RequestStatus.NEW));

        mainContent.add(baslik, aciklama, grid);
    }

    // ── Önceliklendirme Havuzu ──
    private void showOnceliklendirmeHavuzu() {
        mainContent.removeAll();

        H2 baslik = new H2("Önceliklendirme Bekleyen Müşteri Talepleri");
        Paragraph aciklama = new Paragraph(
            "Talepler, PO tarafından girilen öncelik skoruna göre otomatik olarak yukarıdan aşağıya sıralanmaktadır.");

        grid.removeAllColumns();
        grid.addColumn(r -> "#" + r.getRequestId()).setHeader("ID").setWidth("80px");
        grid.addColumn(r -> musteri(r.getCustomerId())).setHeader("Müşteri").setAutoWidth(true);
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addComponentColumn(r -> skorBadge(r)).setHeader("Skor");
        grid.addComponentColumn(r -> islemButonlari(r)).setHeader("İşlem");
        grid.setWidthFull();

        List<Request> talepler = requestService.getAllActiveRequests();
        talepler.sort((a, b) -> {
            int skorA = prioritizationService.findByRequestId(a.getRequestId())
                .map(p -> p.getPriorityScore()).orElse(0);
            int skorB = prioritizationService.findByRequestId(b.getRequestId())
                .map(p -> p.getPriorityScore()).orElse(0);
            return Integer.compare(skorB, skorA);
        });
        grid.setItems(talepler);

        mainContent.add(baslik, aciklama, grid);
    }

    // ── İş Akışları ──
    private void showIsAkislari() {
        mainContent.removeAll();

        H2 baslik = new H2("İş Akışları (Sprint)");

        Grid<Request> isAkisiGrid = new Grid<>(Request.class, false);
        isAkisiGrid.addColumn(r -> "#" + r.getRequestId()).setHeader("ID").setWidth("80px");
        isAkisiGrid.addColumn(r -> musteri(r.getCustomerId())).setHeader("Müşteri").setAutoWidth(true);
        isAkisiGrid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        isAkisiGrid.addComponentColumn(r ->
            workflowService.findByRequestId(r.getRequestId())
                .map(w -> {
                    Span s = new Span(w.getWorkflowStatus().name());
                    if (w.getWorkflowStatus() == WorkflowStatus.DONE) {
                        s.getStyle().set("color", "green").set("font-weight", "bold");
                    }
                    return s;
                }).orElse(new Span("-"))
        ).setHeader("Durum");
        isAkisiGrid.setWidthFull();

        List<Request> isAkisindakiler = requestService.getAllActiveRequests().stream()
            .filter(r -> workflowService.findByRequestId(r.getRequestId()).isPresent())
            .toList();
        isAkisiGrid.setItems(isAkisindakiler);

        mainContent.add(baslik, isAkisiGrid);
    }

    private Span skorBadge(Request r) {
        return prioritizationService.findByRequestId(r.getRequestId())
            .map(p -> {
                int skor = p.getPriorityScore();
                String label = prioritizationService.getLabel(skor);
                Span badge = new Span(skor + " (" + label + ")");
                badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px")
                    .set("font-weight", "bold");
                if (skor >= 81)      badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
                else if (skor >= 61) badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
                else if (skor >= 41) badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
                else                 badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
                return badge;
            })
            .orElseGet(() -> {
                Span badge = new Span("Atanmadı");
                badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px")
                    .set("background", "#e0e0e0").set("color", "#666");
                return badge;
            });
    }

    private HorizontalLayout islemButonlari(Request r) {
        HorizontalLayout layout = new HorizontalLayout();

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
                        Span done = new Span("✓ Tamamlandı");
                        done.getStyle().set("color", "green").set("font-weight", "bold");
                        layout.add(done);
                    } else {
                        Span durum = new Span("İş Akışında: " + w.getWorkflowStatus());
                        layout.add(durum);
                    }
                },
                () -> {
                    Button isAkisiBtn = new Button("İş Akışına Çevir", e -> {
                        try {
                            workflowService.createWorkflow(r.getRequestId());
                            Notification.show("Talep iş akışına alındı.", 3000, Notification.Position.TOP_CENTER);
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

        Span talepBaslik = new Span("Talep: " + request.getTitle());
        talepBaslik.getStyle().set("font-weight", "bold");

        Span beklemeSuresi = new Span("Bekleme Süresi Puanı: " +
            prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));

        Span musteriDegeriSpan = new Span("Müşteri Değeri: " + musteriDegeriLabel);
        musteriDegeriSpan.getStyle().set("color", "#2C6FAC").set("font-weight", "bold");

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

        ComboBox<YoneticiMudahalesi> yoneticiBox = new ComboBox<>("Yönetici Müdahalesi");
        yoneticiBox.setItems(YoneticiMudahalesi.values());
        yoneticiBox.setItemLabelGenerator(v -> switch (v) {
            case IPTAL             -> "Yapılmayacak (×0.0)";
            case NOTR              -> "Nötr (×1.0)";
            case YONETICI_ONAYLI  -> "Yönetici Onaylı (×1.2)";
            case SOZLESME_ZORUNLU -> "Sözleşme Gereği Zorunlu (×1.5)";
        });
        yoneticiBox.setWidthFull();

        ComboBox<GelistiriciMudahalesi> gelistiriciBox = new ComboBox<>("Geliştirici Çaba Tahmini");
        gelistiriciBox.setItems(GelistiriciMudahalesi.values());
        gelistiriciBox.setItemLabelGenerator(v -> switch (v) {
            case QUICK_WIN  -> "Quick Win (< 1 gün) +10";
            case DUSUK      -> "Düşük (1-3 gün) +5";
            case ORTA       -> "Orta (1-2 hafta) 0";
            case YUKSEK     -> "Yüksek (> 2 hafta) -5";
            case COK_YUKSEK -> "Çok Yüksek / Belirsiz -10";
        });
        gelistiriciBox.setWidthFull();

        Span skorSpan  = new Span("Hesaplanan Skor: —");
        Span labelSpan = new Span("");
        skorSpan.getStyle().set("font-weight", "bold").set("font-size", "18px");

        Runnable skorGuncelle = () -> {
            if (isEtkisiBox.getValue() != null && acilyetBox.getValue() != null
                    && isTipiBox.getValue() != null && yoneticiBox.getValue() != null
                    && gelistiriciBox.getValue() != null) {

                Prioritization temp = new Prioritization();
                temp.setIsEtkisi(isEtkisiBox.getValue());
                temp.setAciliyet(acilyetBox.getValue());
                temp.setMusteriDegeriPuan(musteriDegeriPuan);
                temp.setIsTipi(isTipiBox.getValue());
                temp.setIsTimiPuan(isTipiBox.getValue().getPuan());
                temp.setBeklemeSuresiPuan(
                    prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));
                temp.setYoneticiMudahalesi(yoneticiBox.getValue());
                temp.setGelistiriciMudahalesi(gelistiriciBox.getValue());

                int skor = prioritizationService.calculateScore(temp);
                skorSpan.setText("Hesaplanan Skor: " + skor);
                labelSpan.setText(" — " + prioritizationService.getLabel(skor));
            }
        };

        isEtkisiBox.addValueChangeListener(e -> skorGuncelle.run());
        acilyetBox.addValueChangeListener(e -> skorGuncelle.run());
        isTipiBox.addValueChangeListener(e -> skorGuncelle.run());
        yoneticiBox.addValueChangeListener(e -> skorGuncelle.run());
        gelistiriciBox.addValueChangeListener(e -> skorGuncelle.run());

        Button kaydetBtn = new Button("Değerleri Kaydet", e -> {
            if (isEtkisiBox.getValue() == null || acilyetBox.getValue() == null
                    || isTipiBox.getValue() == null || yoneticiBox.getValue() == null
                    || gelistiriciBox.getValue() == null) {
                Notification.show("Tüm alanları seçiniz.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                Prioritization p = new Prioritization();
                p.setRequestId(request.getRequestId());
                p.setIsEtkisi(isEtkisiBox.getValue());
                p.setAciliyet(acilyetBox.getValue());
                p.setIsTipi(isTipiBox.getValue());
                p.setYoneticiMudahalesi(yoneticiBox.getValue());
                p.setGelistiriciMudahalesi(gelistiriciBox.getValue());

                prioritizationService.savePrioritization(
                    p, request.getCustomerId(), request.getCreatedAt());

                Notification.show("Önceliklendirme kaydedildi.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                showOnceliklendirmeHavuzu();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        kaydetBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        HorizontalLayout skorLayout = new HorizontalLayout(skorSpan, labelSpan);
        skorLayout.setAlignItems(Alignment.BASELINE);

        VerticalLayout icerik = new VerticalLayout(
            talepBaslik, beklemeSuresi, musteriDegeriSpan,
            isEtkisiBox, acilyetBox, isTipiBox,
            yoneticiBox, gelistiriciBox, skorLayout
        );
        icerik.setPadding(false);

        dialog.add(icerik);
        dialog.getFooter().add(iptalBtn, kaydetBtn);
        dialog.open();
    }

    private void reddetDialogAc(Request request) {
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
                showOnceliklendirmeHavuzu();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        dialog.add(new VerticalLayout(gerekcaField));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), reddetBtn);
        dialog.open();
    }

    private String musteri(Long customerId) {
        return userService.findById(customerId)
            .map(User::getNameSurname)
            .orElse("Bilinmiyor");
    }
}