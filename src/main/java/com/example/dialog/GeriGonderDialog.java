package com.example.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.function.Consumer;

/**
 * Gerekçeyle bir görevi bir önceki role geri gönderme penceresi.
 * Hem Geliştirici → Scrum Master hem de Scrum Master → Ürün Sorumlusu akışında kullanılır.
 */
public final class GeriGonderDialog {

    private GeriGonderDialog() {}

    /**
     * @param baslik        pencere başlığı
     * @param aciklama      kullanıcıya gösterilecek açıklama
     * @param basariMesaji  gönderim başarılı olunca gösterilecek bildirim
     * @param parent        geri gönderme başarılıysa kapatılacak üst (detay) pencere; null olabilir
     * @param onSuccess     başarıdan sonra çalışacak yenileme (ör. listeyi tazele); null olabilir
     * @param serviceCall   gerekçeyi alıp geri gönderme servisini çağıran işlem
     */
    public static void open(String baslik, String aciklama, String basariMesaji,
                            Dialog parent, Runnable onSuccess, Consumer<String> serviceCall) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(baslik);
        dialog.setWidth("460px");

        Paragraph p = new Paragraph(aciklama);
        p.getStyle().set("font-size", "13px").set("color", "#555");

        TextArea gerekce = new TextArea("Gerekçe");
        gerekce.setWidthFull();
        gerekce.setMinHeight("120px");

        Button gonderBtn = new Button("Geri Gönder", e -> {
            try {
                serviceCall.accept(gerekce.getValue());
                Notification.show(basariMesaji, 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                if (parent != null) parent.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        gonderBtn.getStyle().set("background-color", "#856404").set("color", "white");

        dialog.add(new VerticalLayout(p, gerekce));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), gonderBtn);
        dialog.open();
    }
}
