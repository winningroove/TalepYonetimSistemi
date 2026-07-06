package com.example.dialog;

import com.example.activity.ActivityLogService;
import com.example.activity.ActivityTimeline;
import com.example.enums.WorkflowStatus;
import com.example.message.MesajPaneli;
import com.example.message.RequestMessageService;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Geliştiricinin gördüğü görev detay penceresi: talep bilgileri + ekli dosyalar +
 * ekip notları + aktivite zaman çizelgesi. Başlanmış görev (Devam/Test) Scrum
 * Master'a geri gönderilebilir.
 */
public final class DevGorevDetayDialog {

    private DevGorevDetayDialog() {}

    public static void open(Workflow workflow, Long currentUserId, Runnable onSuccess,
                            RequestService requestService,
                            RequestFileService requestFileService,
                            RequestMessageService requestMessageService,
                            UserService userService,
                            ActivityLogService activityLogService,
                            WorkflowService workflowService) {
        Dialog dialog = new Dialog();

        requestService.findById(workflow.getRequestId()).ifPresent(request -> {
            dialog.setHeaderTitle("Görev Detayı — " + request.getTitle());

            VerticalLayout icerik = new VerticalLayout();
            icerik.setPadding(false);

            icerik.add(new Span("Talep Başlığı: " + request.getTitle()));
            icerik.add(new Span("Açıklama: " + request.getDescription()));
            icerik.add(new Span("Durum: " + workflow.getWorkflowStatus()));
            icerik.add(new Span("Tarih: " + DateUtil.format(request.getCreatedAt())));

            icerik.add(DialogSupport.dosyaEkleri(
                requestFileService.getFilesByRequestId(request.getRequestId()), true));

            icerik.add(MesajPaneli.ekipKanali(request.getRequestId(), currentUserId,
                requestMessageService, userService));
            icerik.add(new ActivityTimeline(
                activityLogService.getByRequestId(request.getRequestId()),
                id -> userService.findById(id).map(User::getNameSurname).orElse("Sistem")));

            dialog.add(icerik);

            // Başlanmış görev (Devam/Test) Scrum Master'a geri gönderilebilir
            WorkflowStatus durum = workflow.getWorkflowStatus();
            if (durum == WorkflowStatus.IN_PROGRESS || durum == WorkflowStatus.TESTING) {
                Button geriBtn = new Button("⬅ Scrum Master'a Geri Gönder",
                    e -> GeriGonderDialog.open(
                        "Scrum Master'a Geri Gönder",
                        "Görev tekrar 'Sırada' durumuna dönecek ve Scrum Master'a bildirilecek. "
                            + "Gerekçe ekip notlarına kaydedilir.",
                        "Görev Scrum Master'a geri gönderildi.",
                        dialog, onSuccess,
                        gerekce -> workflowService.sendBackToScrumMaster(
                            workflow.getTaskId(), workflow.getVersion(), currentUserId, gerekce)));
                geriBtn.getStyle().set("background", "#fff3cd").set("color", "#856404");
                dialog.getFooter().add(geriBtn);
            }
        });

        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }
}
