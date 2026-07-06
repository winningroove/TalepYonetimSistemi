package com.example.dialog;

import com.example.activity.ActivityLogService;
import com.example.activity.ActivityTimeline;
import com.example.enums.RequestStatus;
import com.example.message.MesajPaneli;
import com.example.message.RequestMessageService;
import com.example.request.Request;
import com.example.request.RequestFileService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Müşterinin kendi talebini gördüğü detay penceresi: bilgiler + ret gerekçesi +
 * ekli dosyalar + müşteri ↔ ürün sorumlusu mesajları + aktivite zaman çizelgesi.
 * Ekip (dahili) kanalı burada gösterilmez — müşteri görmemeli.
 */
public final class MusteriDetayDialog {

    private MusteriDetayDialog() {}

    public static void open(Request request, Long currentUserId, String durumText,
                            RequestFileService requestFileService,
                            RequestMessageService requestMessageService,
                            UserService userService,
                            ActivityLogService activityLogService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Detayı");
        dialog.setWidth("520px");

        VerticalLayout icerik = new VerticalLayout();
        icerik.add(new Span("Başlık: " + request.getTitle()));
        icerik.add(new Span("Açıklama: " + request.getDescription()));
        icerik.add(new Span("Durum: " + durumText));
        icerik.add(new Span("Tarih: " + DateUtil.format(request.getCreatedAt())));

        if (request.getStatus() == RequestStatus.REJECTED
                && request.getRejectionReason() != null) {
            Span ret = new Span("Ret Gerekçesi: " + request.getRejectionReason());
            ret.getStyle().set("color", "red");
            icerik.add(ret);
        }

        icerik.add(DialogSupport.dosyaEkleri(
            requestFileService.getFilesByRequestId(request.getRequestId()), false));

        icerik.add(MesajPaneli.musteriKanali(request.getRequestId(), currentUserId,
            requestMessageService, userService));
        icerik.add(new ActivityTimeline(
            activityLogService.getByRequestId(request.getRequestId()),
            id -> userService.findById(id).map(User::getNameSurname).orElse("Sistem")));

        dialog.add(icerik);
        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }
}
