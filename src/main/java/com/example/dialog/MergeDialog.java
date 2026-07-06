package com.example.dialog;

import com.example.request.Request;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Olası kopya talepleri tek ana talepte birleştirme penceresi. */
public final class MergeDialog {

    private MergeDialog() {}

    public static void open(Request secili, List<Request> kopyalar, Runnable onSuccess,
                            RequestService requestService, UserService userService) {
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
            "#" + r.getRequestId() + " — " + musteri(r.getCustomerId(), userService)
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
                if (onSuccess != null) onSuccess.run();
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

    private static String musteri(Long customerId, UserService userService) {
        return userService.findById(customerId).map(User::getNameSurname).orElse("Bilinmiyor");
    }
}
