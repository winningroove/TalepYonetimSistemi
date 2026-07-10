package com.example.views;

import com.example.dialog.DevGorevDetayDialog;
import com.example.enums.WorkflowStatus;
import com.example.prioritization.PrioritizationService;
import com.example.request.Request;
import com.example.request.RequestFile;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.AppSidebar;
import com.example.util.DateUtil;
import com.example.util.Brand;
import com.example.util.GridSearch;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowService;
import com.vaadin.flow.component.button.Button;
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

@Route("developer")
@PageTitle("Geliştirici Paneli")
@RolesAllowed("DEVELOPER")
public class DeveloperView extends HorizontalLayout {

    private final WorkflowService workflowService;
    private final RequestService requestService;
    private final UserService userService;
    private final RequestFileService requestFileService;
    private final PrioritizationService prioritizationService;
    private final com.example.company.CompanyService companyService;
    private final com.example.notification.NotificationService notificationService;
    private final com.example.notification.NotificationBroadcaster notificationBroadcaster;
    private final com.example.message.RequestMessageService requestMessageService;
    private final com.example.activity.ActivityLogService activityLogService;

    private Long currentUserId;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();

    public DeveloperView(WorkflowService workflowService,
                         RequestService requestService,
                         UserService userService,
                         RequestFileService requestFileService,
                         PrioritizationService prioritizationService,
                         com.example.company.CompanyService companyService,
                         com.example.notification.NotificationService notificationService,
                         com.example.notification.NotificationBroadcaster notificationBroadcaster,
                         com.example.message.RequestMessageService requestMessageService,
                         com.example.activity.ActivityLogService activityLogService) {
        this.workflowService = workflowService;
        this.requestService = requestService;
        this.userService = userService;
        this.requestFileService = requestFileService;
        this.prioritizationService = prioritizationService;
        this.companyService = companyService;
        this.notificationService = notificationService;
        this.notificationBroadcaster = notificationBroadcaster;
        this.requestMessageService = requestMessageService;
        this.activityLogService = activityLogService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserId = u.getUserId();
            currentUserName = u.getNameSurname();
        });

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showGorevlerim();
    }

    private VerticalLayout buildSidebar() {
        var bell = new com.example.notification.NotificationBell(
            notificationService, notificationBroadcaster, currentUserId,
            reqId -> workflowService.findByRequestId(reqId).ifPresent(w -> DevGorevDetayDialog.open(
                w, currentUserId, this::showGorevlerim, requestService, requestFileService,
                requestMessageService, userService, activityLogService, workflowService)));
        var profil = com.example.dialog.ProfileDialog.sidebarProfileRow(
            currentUserName, "Geliştirici",
            () -> userService.findById(currentUserId).ifPresent(u ->
                com.example.dialog.ProfileDialog.open(u, companyService, activityLogService, requestService, userService)));

        return AppSidebar.build("Geliştirici Paneli", bell, List.of(
            AppSidebar.menuButton("Görevlerim", this::showGorevlerim),
            AppSidebar.menuButton("Tamamlanan Görevler", this::showTamamlananlar)
        ), profil);
    }

    private VerticalLayout buildMainContent() {
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        return mainContent;
    }

   private void showGorevlerim() {
    mainContent.removeAll();

    H2 baslik = new H2("Görevlerim");
    Paragraph aciklama = new Paragraph("Size atanan aktif görevler. En yüksek öncelikli görevler üstte listelenir.");

    Grid<Workflow> grid = new Grid<>(Workflow.class, false);
    grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
    grid.addComponentColumn(w -> com.example.dialog.DialogSupport.workflowBadge(w.getWorkflowStatus())).setHeader("Durum");
    grid.addComponentColumn(w -> {
        return prioritizationService.findByRequestId(w.getRequestId())
            .map(p -> {
                if (p.getGelistiriciMudahalesi() == null) {
                    Span badge = new Span("Hesaplanmadı");
                    badge.getStyle()
                        .set("padding", "4px 8px")
                        .set("border-radius", "4px")
                        .set("background", "#fff3cd")
                        .set("color", "#856404");
                    return badge;
                }
                int skor = p.getPriorityScore();
                String label;
                if (skor >= 85)      label = "Kritik";
                else if (skor >= 69) label = "Yüksek";
                else if (skor >= 53) label = "Orta";
                else if (skor >= 37) label = "Düşük";
                else                 label = "Çok Düşük";
                Span badge = new Span(skor + " (" + label + ")");
                badge.getStyle()
                    .set("padding", "4px 8px")
                    .set("border-radius", "4px")
                    .set("font-weight", "bold");
                if (skor >= 85)      badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
                else if (skor >= 69) badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
                else if (skor >= 53) badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
                else                 badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
                return badge;
            })
            .orElseGet(() -> {
                Span badge = new Span("-");
                badge.getStyle().set("color", "#888");
                return badge;
            });
    }).setHeader("Öncelik");
    grid.addComponentColumn(this::durumGuncelleButonu).setHeader("İşlem");
    grid.addComponentColumn(w -> {
        Button detayBtn = new Button("Detay", e -> DevGorevDetayDialog.open(
            w, currentUserId, this::showGorevlerim, requestService, requestFileService,
            requestMessageService, userService, activityLogService, workflowService));
        detayBtn.getStyle().set("background-color", "#036baa").set("color", "white");
        return detayBtn;
    }).setHeader("Detay");
    grid.setWidthFull();

    List<Workflow> gorevler = currentUserId != null
        ? new java.util.ArrayList<>(workflowService.getDeveloperWorkflows(currentUserId))
        : new java.util.ArrayList<>();
    gorevler.sort((a, b) -> {
        int skorA = prioritizationService.findByRequestId(a.getRequestId())
            .map(p -> p.getPriorityScore()).orElse(0);
        int skorB = prioritizationService.findByRequestId(b.getRequestId())
            .map(p -> p.getPriorityScore()).orElse(0);
        return Integer.compare(skorB, skorA);
    });

    var arama = GridSearch.create(grid, gorevler,
        "Ara: talep, şirket, durum, tarih...", this::gorevAranabilir);
    grid.setItems(gorevler);

    mainContent.add(baslik, aciklama, arama, grid);
}
   

    private void showTamamlananlar() {
        mainContent.removeAll();

        H2 baslik = new H2("Tamamlanan Görevler");
        Paragraph aciklama = new Paragraph("Tamamladığınız görevlerin listesi.");

        Grid<Workflow> grid = new Grid<>(Workflow.class, false);
        grid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        grid.addColumn(w -> sirketAdiByRequest(w.getRequestId())).setHeader("Şirket").setAutoWidth(true);
        grid.addComponentColumn(w -> {
            Span done = new Span("Tamamlandı");
            done.getStyle().set("color", "green").set("font-weight", "bold");
            return done;
        }).setHeader("Durum");
        grid.addColumn(w -> DateUtil.format(w.getUpdatedAt())).setHeader("Tamamlanma Tarihi");
        grid.setWidthFull();

        List<Workflow> tamamlananlar = currentUserId != null
            ? workflowService.getDoneWorkflowsByDeveloper(currentUserId)
            : List.of();

        var arama = GridSearch.create(grid, tamamlananlar,
            "Ara: talep, şirket, tarih...",
            w -> talepBasligi(w.getRequestId()) + " " + sirketAdiByRequest(w.getRequestId())
                + " " + DateUtil.format(w.getUpdatedAt()));
        grid.setItems(tamamlananlar);

        mainContent.add(baslik, aciklama, arama, grid);
    }

    private Button durumGuncelleButonu(Workflow workflow) {
    WorkflowStatus current = workflow.getWorkflowStatus();

    WorkflowStatus next = switch (current) {
        case BACKLOG     -> WorkflowStatus.IN_PROGRESS;
        case IN_PROGRESS -> WorkflowStatus.TESTING;
        case TESTING     -> WorkflowStatus.DONE;
        default          -> null;
    };

    if (next == null) {
        Button done = new Button("✓ Tamamlandı");
        done.getStyle().set("background-color", "#155724").set("color", "white").set("cursor", "default");
        done.setEnabled(false);
        return done;
    }

    String butonText = switch (next) {
        case IN_PROGRESS -> "▶ Başla";
        case TESTING     -> "Teste Gönder";
        case DONE        -> "✔ Tamamla";
        default          -> "";
    };

    String bgColor = switch (next) {
        case IN_PROGRESS -> "#1B6EC2";
        case TESTING     -> "#B45309";
        case DONE        -> "#166534";
        default          -> "#1B2A3B";
    };

    Button btn = new Button(butonText, e -> {
        try {
            workflowService.updateStatus(workflow.getTaskId(), next, workflow.getVersion());
            showGorevlerim();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    });
    btn.getStyle().set("background-color", bgColor).set("color", "white");
    return btn;
}
    

    private String talepBasligi(Long requestId) {
        return requestService.findById(requestId)
            .map(Request::getTitle)
            .orElse("Bilinmiyor");
    }

    private String sirketAdiByRequest(Long requestId) {
        return requestService.findById(requestId)
            .flatMap(r -> userService.findById(r.getCustomerId()))
            .map(u -> companyService.getName(u.getCompanyId()))
            .orElse("-");
    }

    /** Görev için aranabilir metin: talep başlığı, şirket, durum, talep tarihi. */
    private String gorevAranabilir(Workflow w) {
        String tarih = requestService.findById(w.getRequestId())
            .map(r -> DateUtil.format(r.getCreatedAt())).orElse("");
        return talepBasligi(w.getRequestId())
            + " " + sirketAdiByRequest(w.getRequestId())
            + " " + w.getWorkflowStatus().name()
            + " " + tarih;
    }

}