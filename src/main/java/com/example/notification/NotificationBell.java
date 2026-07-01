package com.example.notification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kenar çubuğunda yer alan canlı bildirim zili. Açık olduğu sürece kullanıcısı
 * için broadcaster'a kayıt olur; bir olay olunca okunmamış sayısı anında güncellenir
 * (Vaadin @Push ile tarayıcıya canlı yansır). Tıklanınca son bildirimleri gösterir
 * ve hepsini okundu işaretler.
 */
public class NotificationBell extends Composite<Button> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final NotificationService service;
    private final NotificationBroadcaster broadcaster;
    private final Long userId;
    /** Bir bildirime (talebi olan) tıklanınca ilgili talebi açar; her panel kendi detayını verir. */
    private final Consumer<Long> onOpenRequest;
    private Registration registration;

    public NotificationBell(NotificationService service, NotificationBroadcaster broadcaster,
                            Long userId, Consumer<Long> onOpenRequest) {
        this.service = service;
        this.broadcaster = broadcaster;
        this.userId = userId;
        this.onOpenRequest = onOpenRequest;

        Button btn = getContent();
        btn.setIcon(VaadinIcon.BELL.create());
        btn.getStyle()
            .set("cursor", "pointer")
            .set("color", "white")
            .set("background", "transparent")
            .set("font-weight", "bold");
        btn.addClickListener(e -> openPanel());
        refreshBadge();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (userId == null) return;
        UI ui = attachEvent.getUI();
        registration = broadcaster.register(userId, () -> ui.access(this::refreshBadge));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void refreshBadge() {
        if (userId == null) return;
        int unread = service.getUnreadCount(userId);
        Button btn = getContent();
        btn.setText(unread > 0 ? " " + unread : "");
        btn.getStyle().set("color", unread > 0 ? "#ffd24a" : "white");
    }

    private void openPanel() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Bildirimler");
        dialog.setWidth("420px");

        VerticalLayout liste = new VerticalLayout();
        liste.setPadding(false);
        liste.setSpacing(false);
        liste.setWidthFull();

        Runnable listeyiDoldur = () -> {
            liste.removeAll();
            List<UserNotification> bildirimler = service.getRecent(userId);
            if (bildirimler.isEmpty()) {
                Span yok = new Span("Bildirim yok.");
                yok.getStyle().set("color", "#888").set("font-size", "13px");
                liste.add(yok);
                return;
            }
            for (UserNotification n : bildirimler) {
                Div satir = new Div();
                satir.setWidthFull();
                satir.getStyle()
                    .set("padding", "8px 10px")
                    .set("border-bottom", "1px solid #eee")
                    .set("background", n.isRead() ? "#ffffff" : "#eef5ff");

                Span msg = new Span(n.getMessage());
                msg.getStyle().set("display", "block").set("font-size", "13px");

                Span zaman = new Span(n.getCreatedAt().format(FMT));
                zaman.getStyle().set("font-size", "11px").set("color", "#888");

                satir.add(msg, zaman);

                // İlgili talebi olan bildirimler tıklanabilir -> talep detayını açar
                if (n.getRequestId() != null && onOpenRequest != null) {
                    satir.getStyle().set("cursor", "pointer");
                    satir.addClickListener(e -> {
                        dialog.close();
                        onOpenRequest.accept(n.getRequestId());
                    });
                }

                liste.add(satir);
            }
        };
        listeyiDoldur.run();

        dialog.add(liste);

        Button temizleBtn = new Button("Bildirimleri Temizle", e -> {
            service.clearAll(userId);
            listeyiDoldur.run();
            refreshBadge();
        });
        temizleBtn.getStyle().set("color", "#c0392b");

        dialog.getFooter().add(temizleBtn, new Button("Kapat", e -> dialog.close()));
        dialog.open();

        // Görüldü -> okundu işaretle ve rozeti güncelle
        service.markAllRead(userId);
        refreshBadge();
    }
}
