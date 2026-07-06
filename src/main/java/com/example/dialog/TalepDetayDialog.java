package com.example.dialog;

import com.example.activity.ActivityLogService;
import com.example.activity.ActivityTimeline;
import com.example.message.MesajPaneli;
import com.example.message.RequestMessageService;
import com.example.request.Request;
import com.example.request.RequestFileService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Ürün sorumlusunun gördüğü talep detay penceresi: bilgiler + dosyalar + mesajlar + aktivite zaman çizelgesi. */
public final class TalepDetayDialog {

    private TalepDetayDialog() {}

    public static void open(Request r, Long currentUserId,
                            RequestFileService requestFileService,
                            RequestMessageService requestMessageService,
                            UserService userService,
                            ActivityLogService activityLogService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Detayı — #" + r.getRequestId());
        dialog.setWidth("520px");

        VerticalLayout icerik = new VerticalLayout();
        icerik.setPadding(false);
        icerik.setSpacing(true);

        String musteriAd = userService.findById(r.getCustomerId())
            .map(User::getNameSurname).orElse("Bilinmiyor");
        Span musteriSpan = new Span("Müşteri: " + musteriAd);
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

        icerik.add(DialogSupport.dosyaEkleri(
            requestFileService.getFilesByRequestId(r.getRequestId()), false));

        icerik.add(MesajPaneli.musteriKanali(r.getRequestId(), currentUserId, requestMessageService, userService));
        icerik.add(MesajPaneli.ekipKanali(r.getRequestId(), currentUserId, requestMessageService, userService));
        icerik.add(new ActivityTimeline(
            activityLogService.getByRequestId(r.getRequestId()),
            id -> userService.findById(id).map(User::getNameSurname).orElse("Sistem")));

        dialog.add(icerik);
        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }
}
