package com.example.views;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.IsTipi;
import com.example.enums.RequestStatus;
import com.example.enums.WorkflowStatus;
import com.example.enums.YoneticiMudahalesi;
import com.example.model.Prioritization;
import com.example.model.Request;
import com.example.service.PrioritizationService;
import com.example.service.RequestService;
import com.example.service.UserService;
import com.example.service.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.example.enums.WorkflowStatus;

import java.util.List;

@Route("po")
@PageTitle("Önceliklendirme Havuzu")
@RolesAllowed("ROLE_PRODUCT_OWNER")
public class POView extends VerticalLayout {

    private final RequestService requestService;
    private final PrioritizationService prioritizationService;
    private final WorkflowService workflowService;
    private final Grid<Request> grid = new Grid<>(Request.class, false);
    private final UserService userService;


    public POView(RequestService requestService,
                  PrioritizationService prioritizationService,
                  WorkflowService workflowService, UserService userService) {
        this.requestService = requestService;
        this.prioritizationService = prioritizationService;
        this.workflowService = workflowService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        H2 baslik = new H2("Önceliklendirme Havuzu");

        // Grid kolonları
        grid.addColumn(Request::getRequestId).setHeader("ID").setWidth("80px");
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addComponentColumn(r -> durumBadge(r.getStatus())).setHeader("Durum");
        grid.addColumn(r -> r.getCreatedAt().toLocalDate()).setHeader("Tarih");
        grid.addComponentColumn(this::islemButonlari).setHeader("İşlem");

        grid.setWidthFull();

        add(baslik, grid);
        listeYenile();
    }

    private void listeYenile() {
        List<Request> talepler = requestService.getAllActiveRequests();
        grid.setItems(talepler);
    }

    private HorizontalLayout islemButonlari(Request request) {
        HorizontalLayout layout = new HorizontalLayout();

        // İncelemeye Al butonu
        if (request.getStatus() == RequestStatus.NEW) {
            Button incelemeButton = new Button("İncelemeye Al", e -> {
                try {
                    requestService.takeUnderReview(request.getRequestId());
                    listeYenile();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            layout.add(incelemeButton);
        }

        // Önceliklendir butonu
        if (request.getStatus() == RequestStatus.UNDER_REVIEW) {
            Button oncelikButton = new Button("Önceliklendir", e ->
                onceliklendirmeDialogAc(request));
            Button reddetButton = new Button("Reddet", e ->
                reddetDialogAc(request));
            layout.add(oncelikButton, reddetButton);
        }

        // İş akışına çevir butonu
  if (request.getStatus() == RequestStatus.PRIORITIZED) {
    workflowService.findByRequestId(request.getRequestId()).ifPresentOrElse(
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
            Button isAkisiButton = new Button("İş Akışına Çevir", e -> {
                try {
                    workflowService.createWorkflow(request.getRequestId());
                    Notification.show("Talep iş akışına alındı.",
                        3000, Notification.Position.TOP_CENTER);
                    listeYenile();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            layout.add(isAkisiButton);
        }
    );
}

        return layout;
    }

    private void onceliklendirmeDialogAc(Request request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Önceliklendirme — #" + request.getRequestId());
        dialog.setWidth("500px");

        // Readonly bilgiler
        Span talepBaslik = new Span("Talep: " + request.getTitle());
        Span beklemeSuresi = new Span("Bekleme: " +
    prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()) + " puan");

// Bunu ekle
int musteriDegeriPuan = userService.getMusteriDegeriPuan(request.getCustomerId());
String musteriDegeriLabel = switch (musteriDegeriPuan) {
    case 5 -> "VIP (5)";
    case 4 -> "Büyük (4)";
    case 3 -> "Orta (3)";
    case 2 -> "Küçük (2)";
    default -> "İç Kullanıcı (1)";
};
Span musteriDegeriSpan = new Span("Müşteri Değeri: " + musteriDegeriLabel);

        // Kriterler
       // isEtkisiField yerine
ComboBox<Integer> isEtkisiBox = new ComboBox<>("İş Etkisi");
isEtkisiBox.setItems(1, 2, 3, 4, 5);
isEtkisiBox.setItemLabelGenerator(v -> switch (v) {
    case 5 -> "5 - Sistem tamamen çalışmıyor (Production Down)";
    case 4 -> "4 - Kritik iş süreci etkileniyor, alternatif yok";
    case 3 -> "3 - Kısmi etki, geçici çözüm mevcut";
    case 2 -> "2 - Küçük etki, işler yavaşlıyor";
    default -> "1 - Kozmetik / Görsel";
});
isEtkisiBox.setWidthFull();

// acilyetField yerine
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
        isTipiBox.setWidthFull();

        ComboBox<YoneticiMudahalesi> yoneticiBox = new ComboBox<>("Yönetici Müdahalesi");
        yoneticiBox.setItems(YoneticiMudahalesi.values());
        yoneticiBox.setWidthFull();

        ComboBox<GelistiriciMudahalesi> gelistiriciBox = new ComboBox<>("Geliştirici Çaba Tahmini");
        gelistiriciBox.setItems(GelistiriciMudahalesi.values());
        gelistiriciBox.setWidthFull();

        // Anlık skor gösterimi
        Span skorSpan = new Span("Hesaplanan Skor: —");
        Span labelSpan = new Span("");

        // Her alan değiştiğinde skoru güncelle
      Runnable skorGuncelle = () -> {
    if (isEtkisiBox.getValue() != null && acilyetBox.getValue() != null
            && isTipiBox.getValue() != null && yoneticiBox.getValue() != null
            && gelistiriciBox.getValue() != null) {

        Prioritization temp = new Prioritization();
        temp.setIsEtkisi(isEtkisiBox.getValue());
        temp.setAciliyet(acilyetBox.getValue());
        
                temp.setMusteriDegeriPuan(3); // preview için orta değer
                temp.setIsTipi(isTipiBox.getValue());
                temp.setIsTimiPuan(isTipiBox.getValue().getPuan());
                temp.setBeklemeSuresiPuan(
                    prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));
                temp.setYoneticiMudahalesi(yoneticiBox.getValue());
                temp.setGelistiriciMudahalesi(gelistiriciBox.getValue());

                int skor = prioritizationService.calculateScore(temp);
                skorSpan.setText("Hesaplanan Skor: " + skor);
                labelSpan.setText(prioritizationService.getLabel(skor));
            }
        };

        isEtkisiBox.addValueChangeListener(e -> skorGuncelle.run());
        acilyetBox.addValueChangeListener(e -> skorGuncelle.run());
        isTipiBox.addValueChangeListener(e -> skorGuncelle.run());
        yoneticiBox.addValueChangeListener(e -> skorGuncelle.run());
        gelistiriciBox.addValueChangeListener(e -> skorGuncelle.run());

        Button kaydetButton = new Button("Değerleri Kaydet", e -> {
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

                Notification.show("Önceliklendirme kaydedildi.",
                    3000, Notification.Position.TOP_CENTER);
                dialog.close();
                listeYenile();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button iptalButton = new Button("İptal", e -> dialog.close());

        VerticalLayout icerik = new VerticalLayout(
            talepBaslik, beklemeSuresi,
            musteriDegeriSpan,
            isEtkisiBox, acilyetBox,
            isTipiBox, yoneticiBox, gelistiriciBox,
            new HorizontalLayout(skorSpan, labelSpan)
        );

        dialog.add(icerik);
        dialog.getFooter().add(iptalButton, kaydetButton);
        dialog.open();
    }

    private void reddetDialogAc(Request request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talebi Reddet — #" + request.getRequestId());

        TextArea gerekcaField = new TextArea("Ret Gerekçesi");
        gerekcaField.setWidthFull();
        gerekcaField.setMinHeight("120px");

        Button reddetButton = new Button("Reddet", e -> {
            try {
                requestService.rejectRequest(request.getRequestId(), gerekcaField.getValue());
                Notification.show("Talep reddedildi.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                listeYenile();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button iptalButton = new Button("İptal", e -> dialog.close());

        dialog.add(new VerticalLayout(gerekcaField));
        dialog.getFooter().add(iptalButton, reddetButton);
        dialog.open();
    }

    private Span durumBadge(RequestStatus status) {
        Span badge = new Span(status.name());
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px");
        switch (status) {
            case NEW          -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case UNDER_REVIEW -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case PRIORITIZED  -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case REJECTED     -> badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
        }
        return badge;
    }
}