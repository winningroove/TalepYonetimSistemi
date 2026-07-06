package com.example.dialog;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.Role;
import com.example.prioritization.PrioritizationService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Scrum Master'ın bir göreve geliştirici atadığı ve çaba tahminini girdiği pencere.
 * Çaba tahmini kaydedilince nihai öncelik skoru yeniden hesaplanır.
 */
public final class SmAtamaDialog {

    private SmAtamaDialog() {}

    public static void open(Workflow workflow, String baslik, Runnable onSuccess,
                            UserService userService,
                            WorkflowService workflowService,
                            PrioritizationService prioritizationService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Gelistirici Ata — " + baslik);

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
                if (onSuccess != null) onSuccess.run();
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
}
