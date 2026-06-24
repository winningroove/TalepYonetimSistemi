// views/customer/CustomerView.java
package com.example.views;

import com.example.enums.RequestStatus;
import com.example.model.Request;
import com.example.model.User;
import com.example.service.RequestService;
import com.example.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("customer")
@PageTitle("Taleplerim")
@RolesAllowed("ROLE_CUSTOMER")
public class CustomerView extends VerticalLayout {

    private final RequestService requestService;
    private final UserService userService;
    private final Grid<Request> grid = new Grid<>(Request.class, false);
    private Long currentUserId;

    public CustomerView(RequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;

        // Giriş yapan kullanıcıyı bul
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> currentUserId = u.getUserId());

        setSizeFull();
        setPadding(true);

        H2 baslik = new H2("Taleplerim");

        Button yeniTalepButton = new Button("+ Yeni Talep", e -> yeniTalepDialogAc());

        HorizontalLayout ustBar = new HorizontalLayout(baslik, yeniTalepButton);
        ustBar.setWidthFull();
        ustBar.setAlignItems(Alignment.CENTER);

        // Grid kolonları
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addColumn(r -> r.getCreatedAt().toLocalDate()).setHeader("Tarih");
        grid.addComponentColumn(r -> durumBadge(r.getStatus())).setHeader("Durum");
        grid.addComponentColumn(r -> {
            Button detayBtn = new Button("Detay", e -> detayDialogAc(r));
            return detayBtn;
        }).setHeader("İşlem");

        grid.setWidthFull();

        add(ustBar, grid);
        listeYenile();
    }

    private void listeYenile() {
        if (currentUserId != null) {
            List<Request> talepler = requestService.getCustomerRequests(currentUserId);
            grid.setItems(talepler);
        }
    }

    private void yeniTalepDialogAc() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yeni Talep Oluştur");

        TextField baslikField = new TextField("Talep Başlığı");
        baslikField.setWidthFull();
        baslikField.setMaxLength(200);

        TextArea aciklamaField = new TextArea("Talep Detayı ve Açıklama");
        aciklamaField.setWidthFull();
        aciklamaField.setMinHeight("150px");

        Button gondерButton = new Button("Talebi Gönder", e -> {
            try {
                Request request = new Request();
                request.setCustomerId(currentUserId);
                request.setTitle(baslikField.getValue());
                request.setDescription(aciklamaField.getValue());
                requestService.createRequest(request);

                Notification.show("Talebiniz alındı.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                listeYenile();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button iptalButton = new Button("İptal", e -> dialog.close());

        dialog.add(new VerticalLayout(baslikField, aciklamaField));
        dialog.getFooter().add(iptalButton, gondерButton);
        dialog.open();
    }

    private void detayDialogAc(Request request) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Detayı");

        VerticalLayout icerik = new VerticalLayout();
        icerik.add(new Span("Başlık: " + request.getTitle()));
        icerik.add(new Span("Açıklama: " + request.getDescription()));
        icerik.add(new Span("Durum: " + request.getStatus()));
        icerik.add(new Span("Tarih: " + request.getCreatedAt().toLocalDate()));

        if (request.getStatus() == RequestStatus.REJECTED && request.getRejectionReason() != null) {
            icerik.add(new Span("Ret Gerekçesi: " + request.getRejectionReason()));
        }

        dialog.add(icerik);
        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }

    private Span durumBadge(RequestStatus status) {
        Span badge = new Span(status.name());
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px");
        switch (status) {
            case NEW          -> badge.getStyle().set("background", "#e0e0e0").set("color", "#333");
            case UNDER_REVIEW -> badge.getStyle().set("background", "#fff9c4").set("color", "#7d6608");
            case PRIORITIZED  -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case REJECTED     -> badge.getStyle().set("background", "#f8d7da").set("color", "#721c24");
        }
        return badge;
    }
}