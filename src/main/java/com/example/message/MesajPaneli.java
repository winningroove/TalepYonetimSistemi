package com.example.message;

import com.example.enums.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.example.util.DateUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.List;

/**
 * Talep detay dialoglarında kullanılan yeniden kullanılabilir mesajlaşma paneli.
 * <p>
 * İki kanalı da tek yerde toplar: müşteri ↔ ürün sorumlusu (dış) ve ekip içi
 * (dahili) kanal. Daha önce bu kod POView / DeveloperView / ScrumMasterView /
 * CustomerView içinde ayrı ayrı kopyalanmıştı; artık tek sınıftan gelir.
 *
 * Kullanım:
 *   MesajPaneli.musteriKanali(requestId, currentUserId, requestMessageService, userService)
 *   MesajPaneli.ekipKanali(requestId, currentUserId, requestMessageService, userService)
 */
public class MesajPaneli extends Composite<VerticalLayout> {

    private final Long requestId;
    private final Long currentUserId;
    private final RequestMessageService requestMessageService;
    private final UserService userService;
    private final boolean internal;

    private MesajPaneli(Long requestId, Long currentUserId,
                        RequestMessageService requestMessageService,
                        UserService userService, boolean internal) {
        this.requestId = requestId;
        this.currentUserId = currentUserId;
        this.requestMessageService = requestMessageService;
        this.userService = userService;
        this.internal = internal;
        build();
    }

    /** Müşteri ↔ ürün sorumlusu (dış) kanalı. */
    public static MesajPaneli musteriKanali(Long requestId, Long currentUserId,
                                            RequestMessageService requestMessageService,
                                            UserService userService) {
        return new MesajPaneli(requestId, currentUserId, requestMessageService, userService, false);
    }

    /** Ekip içi (dahili) kanal — müşteri görmez; PO / SM / geliştirici arası. */
    public static MesajPaneli ekipKanali(Long requestId, Long currentUserId,
                                         RequestMessageService requestMessageService,
                                         UserService userService) {
        return new MesajPaneli(requestId, currentUserId, requestMessageService, userService, true);
    }

    private void build() {
        VerticalLayout panel = getContent();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidthFull();

        H4 baslik = new H4(internal ? "Ekip Notları (Dahili)" : "Mesajlar");
        baslik.getStyle().set("margin", "12px 0 6px 0");
        if (internal) baslik.getStyle().set("color", "#856404");

        VerticalLayout liste = new VerticalLayout();
        liste.setPadding(false);
        liste.setSpacing(false);
        liste.setWidthFull();
        liste.getStyle()
            .set("max-height", "240px").set("overflow-y", "auto")
            .set("border-radius", "6px").set("padding", "8px");
        if (internal) {
            liste.getStyle().set("background", "#fffdf5").set("border", "1px solid #ffe08a");
        } else {
            liste.getStyle().set("background", "#f8f9fa");
        }

        Runnable yukle = () -> {
            liste.removeAll();
            List<RequestMessage> mesajlar = internal
                ? requestMessageService.getInternalMessages(requestId)
                : requestMessageService.getMessages(requestId);
            if (mesajlar.isEmpty()) {
                Span yok = new Span(internal ? "Henüz ekip notu yok." : "Henüz mesaj yok.");
                yok.getStyle().set("color", "#888").set("font-size", "12px");
                liste.add(yok);
            } else {
                mesajlar.forEach(m -> liste.add(mesajBalonu(m)));
            }
        };
        yukle.run();

        TextArea girdi = new TextArea();
        girdi.setPlaceholder(internal ? "Ekip notu yazın..." : "Mesaj yazın...");
        girdi.setWidthFull();
        girdi.setMaxHeight("100px");

        Button gonder = new Button(internal ? "Not Ekle" : "Gönder", e -> {
            try {
                if (internal) {
                    requestMessageService.sendInternalMessage(requestId, currentUserId, girdi.getValue());
                } else {
                    requestMessageService.sendMessage(requestId, currentUserId, girdi.getValue());
                }
                girdi.clear();
                yukle.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        gonder.getStyle().set("background-color", internal ? "#856404" : "#1B2A3B").set("color", "white");

        HorizontalLayout gonderSatir = new HorizontalLayout(girdi, gonder);
        gonderSatir.setWidthFull();
        gonderSatir.setAlignItems(Alignment.END);
        gonderSatir.expand(girdi);

        if (internal) {
            Span aciklama = new Span("Bu kanal müşteriye kapalıdır; yalnızca ürün sorumlusu, scrum master ve geliştiriciler görür.");
            aciklama.getStyle().set("font-size", "11px").set("color", "#888").set("display", "block").set("margin-bottom", "6px");
            panel.add(baslik, aciklama, liste, gonderSatir);
        } else {
            panel.add(baslik, liste, gonderSatir);
        }
    }

    private Div mesajBalonu(RequestMessage m) {
        User sender = userService.findById(m.getSenderId()).orElse(null);
        String ad = sender != null ? sender.getNameSurname() : "Bilinmeyen";
        String rol = sender != null ? rolKisa(sender.getRole()) : "";
        boolean benim = currentUserId != null && currentUserId.equals(m.getSenderId());

        Div balon = new Div();
        balon.getStyle()
            .set("background", benim ? "#d1e7ff" : "#ffffff")
            .set("border", "1px solid #e0e0e0").set("border-radius", "6px")
            .set("padding", "6px 10px").set("max-width", "75%");

        Span ust = new Span(ad + " · " + rol + " · " + DateUtil.format(m.getCreatedAt()));
        ust.getStyle()
            .set("font-size", "11px").set("color", "#666")
            .set("font-weight", "bold").set("display", "block").set("margin-bottom", "2px");

        Span govde = new Span(m.getBody());
        govde.getStyle().set("white-space", "pre-wrap").set("font-size", "13px");

        balon.add(ust, govde);

        Div satir = new Div(balon);
        satir.getStyle()
            .set("display", "flex")
            .set("justify-content", benim ? "flex-end" : "flex-start")
            .set("width", "100%")
            .set("margin-bottom", "6px");
        return satir;
    }

    private String rolKisa(Role role) {
        return switch (role) {
            case CUSTOMER      -> "Müşteri";
            case PRODUCT_OWNER -> "Ürün Sorumlusu";
            case DEVELOPER     -> "Geliştirici";
            case SCRUM_MASTER  -> "Scrum Master";
            case ADMIN         -> "Admin";
        };
    }
}
