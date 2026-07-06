package com.example.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;

/**
 * Genel amaçlı "emin misiniz?" onay penceresi (kalıcı silme gibi geri alınamaz işlemler için).
 * Aynı kalıp hem kullanıcı hem şirket silme akışında kullanılır.
 */
public final class OnayDialog {

    private OnayDialog() {}

    /**
     * @param baslik       pencere başlığı
     * @param mesaj        onay metni
     * @param basariMesaji işlem başarılıysa gösterilecek bildirim
     * @param onSuccess    başarıdan sonra çalışacak yenileme; null olabilir
     * @param islem        onaylandığında çalışacak (hata fırlatabilir) işlem
     */
    public static void open(String baslik, String mesaj, String basariMesaji,
                            Runnable onSuccess, Runnable islem) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(baslik);

        Paragraph p = new Paragraph(mesaj);

        Button silBtn = new Button("Sil", e -> {
            try {
                islem.run();
                Notification.show(basariMesaji, 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        silBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        dialog.add(p);
        dialog.getFooter().add(iptalBtn, silBtn);
        dialog.open();
    }
}
