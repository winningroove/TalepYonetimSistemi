package com.example.dialog;

import com.example.activity.ActivityLogService;
import com.example.activity.ActivityTimeline;
import com.example.enums.WorkflowStatus;
import com.example.message.MesajPaneli;
import com.example.message.RequestMessageService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Scrum Master'ın gördüğü görev detay penceresi: talep bilgileri + ekip notları +
 * aktivite zaman çizelgesi. Sırada bekleyen (başlanmamış) görev Ürün Sorumlusuna
 * geri gönderilebilir.
 */
public final class SmGorevDetayDialog {

    private SmGorevDetayDialog() {}

    public static void open(Workflow workflow, Long currentUserId,
                            String sirket, String gelistirici, String durumText,
                            Runnable onSuccess,
                            RequestService requestService,
                            RequestMessageService requestMessageService,
                            UserService userService,
                            ActivityLogService activityLogService,
                            WorkflowService workflowService) {
        Dialog dialog = new Dialog();
        dialog.setWidth("520px");

        requestService.findById(workflow.getRequestId()).ifPresent(request -> {
            dialog.setHeaderTitle("Görev Detayı — " + request.getTitle());

            VerticalLayout icerik = new VerticalLayout();
            icerik.setPadding(false);
            icerik.setSpacing(true);

            icerik.add(new Span("Şirket: " + sirket));
            icerik.add(new Span("Geliştirici: " + gelistirici));
            icerik.add(new Span("Durum: " + durumText));
            icerik.add(new Span("Tarih: " + DateUtil.format(request.getCreatedAt())));

            Paragraph aciklama = new Paragraph(request.getDescription());
            aciklama.getStyle()
                .set("background", "#f8f9fa").set("border-radius", "6px")
                .set("padding", "12px").set("font-size", "13px").set("white-space", "pre-wrap");
            icerik.add(aciklama);

            icerik.add(MesajPaneli.ekipKanali(request.getRequestId(), currentUserId,
                requestMessageService, userService));
            icerik.add(new ActivityTimeline(
                activityLogService.getByRequestId(request.getRequestId()),
                id -> userService.findById(id).map(User::getNameSurname).orElse("Sistem")));

            dialog.add(icerik);

            // Sırada bekleyen (başlanmamış) görev Ürün Sorumlusuna geri gönderilebilir
            if (workflow.getWorkflowStatus() == WorkflowStatus.BACKLOG) {
                Button geriBtn = new Button("⬅ Ürün Sorumlusuna Geri Gönder",
                    e -> GeriGonderDialog.open(
                        "Ürün Sorumlusuna Geri Gönder",
                        "Görev iş akışından çıkarılacak ve talep tekrar 'İncelemede' durumuna dönecek. "
                            + "Ürün sorumlusu yeniden değerlendirebilir. Gerekçe ekip notlarına kaydedilir.",
                        "Talep Ürün Sorumlusuna geri gönderildi.",
                        dialog, onSuccess,
                        gerekce -> workflowService.sendBackToProductOwner(
                            workflow.getTaskId(), currentUserId, gerekce)));
                geriBtn.getStyle().set("background", "#fff3cd").set("color", "#856404");
                dialog.getFooter().add(geriBtn);
            }
        });

        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }
}
