package com.example.dialog;

import com.example.request.Request;
import com.example.request.RequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

/** Talep reddetme penceresi — ret gerekçesi alır ve talebi reddeder. */
public final class ReddetDialog {

    private ReddetDialog() {}

    public static void open(Request request, Runnable onSuccess, RequestService requestService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talebi Reddet — #" + request.getRequestId());

        TextArea gerekcaField = new TextArea("Ret Gerekçesi");
        gerekcaField.setWidthFull();
        gerekcaField.setMinHeight("120px");

        Button reddetBtn = new Button("Reddet", e -> {
            try {
                requestService.rejectRequest(request.getRequestId(), gerekcaField.getValue());
                Notification.show("Talep reddedildi.", 3000, Notification.Position.TOP_CENTER);
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        dialog.add(new VerticalLayout(gerekcaField));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), reddetBtn);
        dialog.open();
    }
}
