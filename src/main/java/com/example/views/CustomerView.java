package com.example.views;

import com.example.enums.RequestStatus;
import com.example.model.Request;
import com.example.model.User;
import com.example.service.RequestService;
import com.example.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("customer")
@PageTitle("Müşteri Portalı")
@RolesAllowed("ROLE_CUSTOMER")
public class CustomerView extends HorizontalLayout {

    private final RequestService requestService;
    private final UserService userService;
    private Long currentUserId;
    private String currentUserName;

    private final VerticalLayout mainContent = new VerticalLayout();
    private final Grid<Request> grid = new Grid<>(Request.class, false);

    public CustomerView(RequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findByEmail(email).ifPresent(u -> {
            currentUserId = u.getUserId();
            currentUserName = u.getNameSurname();
        });

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildSidebar(), buildMainContent());
        showTaleplerim();
    }

    private VerticalLayout buildSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("250px");
        sidebar.setHeightFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(false);
        sidebar.getStyle()
            .set("background-color", "#1B2A3B")
            .set("color", "white")
            .set("flex-shrink", "0");

        // Başlık
        H3 baslik = new H3("Talep Yönetim Sistemi");
        baslik.getStyle().set("color", "white").set("margin-top", "0");

        Span altBaslik = new Span("Müşteri Portalı");
        altBaslik.getStyle().set("color", "#aaaaaa").set("font-size", "12px");

        // Menü
        H5 menuBaslik = new H5("Menü");
        menuBaslik.getStyle().set("color", "#aaaaaa").set("margin-bottom", "8px").set("margin-top", "24px");

        Button yeniTalepBtn = menuButton("• Yeni Talep Ekle");
        Button taleplerimBtn = menuButton("• Taleplerim");

        yeniTalepBtn.addClickListener(e -> showYeniTalepFormu());
        taleplerimBtn.addClickListener(e -> showTaleplerim());

        // Giriş yapan kullanıcı
        Div divider = new Div();
        divider.getStyle()
            .set("border-top", "1px solid #444")
            .set("margin-top", "auto")
            .set("padding-top", "16px")
            .set("width", "100%");

        Span girisYapan = new Span("Giriş Yapan:");
        girisYapan.getStyle().set("color", "#aaaaaa").set("font-size", "12px").set("display", "block");

        Span kullaniciAdi = new Span(currentUserName + " (Müşteri)");
        kullaniciAdi.getStyle().set("color", "white").set("font-size", "13px");

        sidebar.add(baslik, altBaslik, menuBaslik, yeniTalepBtn, taleplerimBtn);
        sidebar.addAndExpand(new Div()); // boşluğu aşağı it
        sidebar.add(divider, girisYapan, kullaniciAdi);

        return sidebar;
    }

    private Button menuButton(String text) {
        Button btn = new Button(text);
        btn.getStyle()
            .set("color", "white")
            .set("background", "transparent")
            .set("border", "none")
            .set("text-align", "left")
            .set("width", "100%")
            .set("cursor", "pointer")
            .set("padding", "8px 0");
        return btn;
    }

    private VerticalLayout buildMainContent() {
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        return mainContent;
    }

    private void showYeniTalepFormu() {
        mainContent.removeAll();

        H2 baslik = new H2("Yeni Destek / Geliştirme Talebi Bildir");

        TextField talepBasligi = new TextField("Talep Başlığı");
        talepBasligi.setPlaceholder("Örn: Ödeme ekranında kredi kartı hata uyarısı alınıyor...");
        talepBasligi.setWidthFull();

        TextArea talepDetayi = new TextArea("Talep Detayı ve Açıklama");
        talepDetayi.setPlaceholder("Yaşanan problemi veya eklenmesini istediğiniz özelliği detaylıca yazınız...");
        talepDetayi.setWidthFull();
        talepDetayi.setMinHeight("150px");

        Button gondерBtn = new Button("Talebi Gönder");
        gondерBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        gondерBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        gondерBtn.addClickListener(e -> {
            try {
                Request request = new Request();
                request.setCustomerId(currentUserId);
                request.setTitle(talepBasligi.getValue());
                request.setDescription(talepDetayi.getValue());
                requestService.createRequest(request);
                Notification.show("Talebiniz alındı.", 3000, Notification.Position.TOP_CENTER);
                showTaleplerim();
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        HorizontalLayout btnLayout = new HorizontalLayout(gondерBtn);
        btnLayout.setWidthFull();
        btnLayout.setJustifyContentMode(JustifyContentMode.END);

        mainContent.add(baslik, talepBasligi, talepDetayi, btnLayout);
    }

    private void showTaleplerim() {
        mainContent.removeAll();

        H2 baslik = new H2("Taleplerim");

        grid.removeAllColumns();
        grid.addColumn(Request::getTitle).setHeader("Başlık").setAutoWidth(true);
        grid.addColumn(r -> r.getCreatedAt().toLocalDate()).setHeader("Tarih");
        grid.addComponentColumn(r -> durumBadge(r.getStatus())).setHeader("Durum");
        grid.addComponentColumn(r -> {
            Button detayBtn = new Button("Detay", e -> detayDialogAc(r));
            return detayBtn;
        }).setHeader("İşlem");
        grid.setWidthFull();

        if (currentUserId != null) {
            grid.setItems(requestService.getCustomerRequests(currentUserId));
        }

        mainContent.add(baslik, grid);
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
            Span ret = new Span("Ret Gerekçesi: " + request.getRejectionReason());
            ret.getStyle().set("color", "red");
            icerik.add(ret);
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