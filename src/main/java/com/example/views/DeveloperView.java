// views/developer/DeveloperView.java
package com.example.views;

import com.example.enums.WorkflowStatus;
import com.example.model.Request;
import com.example.model.User;
import com.example.model.Workflow;
import com.example.service.RequestService;
import com.example.service.UserService;
import com.example.service.WorkflowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("developer")
@PageTitle("Görevlerim")
@RolesAllowed("ROLE_DEVELOPER")
public class DeveloperView extends VerticalLayout {

    private final WorkflowService workflowService;
    private final RequestService requestService;
    private final UserService userService;
    private final Grid<Workflow> gorevGrid = new Grid<>(Workflow.class, false);
    private final Grid<Workflow> havuzGrid = new Grid<>(Workflow.class, false);
    private Long currentUserId;

    public DeveloperView(WorkflowService workflowService,
                         RequestService requestService,
                         UserService userService) {
        this.workflowService = workflowService;
        this.requestService = requestService;
        this.userService = userService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> currentUserId = u.getUserId());

        setSizeFull();
        setPadding(true);

        // Görevlerim bölümü
        H2 gorevBaslik = new H2("Görevlerim");
        gorevGrid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        gorevGrid.addComponentColumn(w -> durumBadge(w.getWorkflowStatus())).setHeader("Durum");
        gorevGrid.addComponentColumn(this::durumGuncelleButonu).setHeader("İşlem");
        gorevGrid.setWidthFull();

        // Havuz bölümü
        H3 havuzBaslik = new H3("Atanmamış Görev Havuzu");
        havuzGrid.addColumn(w -> talepBasligi(w.getRequestId())).setHeader("Talep").setAutoWidth(true);
        havuzGrid.addComponentColumn(w -> {
            Button ustlenButton = new Button("Görevi Üstlen", e -> {
                try {
                    workflowService.assignDeveloper(
                        w.getTaskId(), currentUserId, w.getVersion());
                    Notification.show("Görev üstlenildi.",
                        3000, Notification.Position.TOP_CENTER);
                    listeYenile();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });
            return ustlenButton;
        }).setHeader("İşlem");
        havuzGrid.setWidthFull();

  H3 tamamlananBaslik = new H3("Tamamlanan Görevler");
Grid<Workflow> tamamlananGrid = new Grid<>(Workflow.class, false);
tamamlananGrid.addColumn(w -> talepBasligi(w.getRequestId()))
    .setHeader("Talep").setAutoWidth(true);
tamamlananGrid.addComponentColumn(w -> {
    Span done = new Span("✓ Tamamlandı");
    done.getStyle().set("color", "green").set("font-weight", "bold");
    return done;
}).setHeader("Durum");
tamamlananGrid.setWidthFull();

List<Workflow> tamamlananlar = workflowService.getDoneWorkflowsByDeveloper(currentUserId);
tamamlananGrid.setItems(tamamlananlar != null ? tamamlananlar : List.of());

add(gorevBaslik, gorevGrid, havuzBaslik, havuzGrid, tamamlananBaslik, tamamlananGrid);

        add(gorevBaslik, gorevGrid, havuzBaslik, havuzGrid);
        listeYenile();
    }

    private void listeYenile() {
        if (currentUserId != null) {
            List<Workflow> gorevler = workflowService.getDeveloperWorkflows(currentUserId);
            gorevGrid.setItems(gorevler);

            List<Workflow> havuz = workflowService.getUnassignedWorkflows();
            havuzGrid.setItems(havuz);
        }
    }

    private Button durumGuncelleButonu(Workflow workflow) {
        WorkflowStatus current = workflow.getWorkflowStatus();

        WorkflowStatus next = switch (current) {
            case BACKLOG     -> WorkflowStatus.IN_PROGRESS;
            case IN_PROGRESS -> WorkflowStatus.TESTING;
            case TESTING     -> WorkflowStatus.DONE;
            default          -> null;
        };

        if (next == null) return new Button("Tamamlandı");

        String butonText = switch (next) {
            case IN_PROGRESS -> "Başla";
            case TESTING     -> "Teste Gönder";
            case DONE        -> "Tamamla";
            default          -> "";
        };

        Button button = new Button(butonText, e -> {
            try {
                workflowService.updateStatus(
                    workflow.getTaskId(), next, workflow.getVersion());
                listeYenile();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        return button;
    }

    private String talepBasligi(Long requestId) {
        return requestService.findById(requestId)
                .map(Request::getTitle)
                .orElse("Bilinmiyor");
    }

    private Span durumBadge(WorkflowStatus status) {
        Span badge = new Span(status.name());
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px");
        switch (status) {
            case BACKLOG     -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case IN_PROGRESS -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case TESTING     -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case DONE        -> badge.getStyle().set("background", "#d4edda").set("color", "#155724");
        }
        return badge;
    }
}