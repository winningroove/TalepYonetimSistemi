package com.example.user;

import com.example.activity.ActivityLog;
import com.example.activity.ActivityLogService;
import com.example.company.CompanyService;
import com.example.enums.Role;
import com.example.request.Request;
import com.example.request.RequestService;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Paylaşımlı profil bileşeni: sidebar'daki tıklanabilir kullanıcı satırını ve
 * "Profilim" penceresini (bilgiler + son aktiviteler + şifre değiştirme) üretir.
 * Servisleri doğrudan alır; kullanmayan view'lar için ilgili servis null geçilebilir.
 */
public final class ProfileDialog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ProfileDialog() {}

    /** Sidebar altındaki tıklanabilir avatar + ad + rol satırı. */
    public static HorizontalLayout sidebarProfileRow(String name, String roleLabel, Runnable onClick) {
        Avatar avatar = new Avatar();
        avatar.getStyle().set("--vaadin-avatar-size", "38px").set("flex-shrink", "0");

        Span ad = new Span(name != null ? name : "Kullanıcı");
        ad.getStyle().set("color", "white").set("font-size", "13px")
            .set("font-weight", "600").set("display", "block");
        Span rol = new Span(roleLabel);
        rol.getStyle().set("color", "#c7d3e0").set("font-size", "11px").set("display", "block");
        Div bilgi = new Div(ad, rol);

        HorizontalLayout satir = new HorizontalLayout(avatar, bilgi);
        satir.setAlignItems(FlexComponent.Alignment.CENTER);
        satir.setWidthFull();
        satir.getStyle()
            .set("cursor", "pointer").set("padding", "8px 6px")
            .set("border-radius", "8px").set("transition", "background .15s ease");
        satir.getElement().addEventListener("mouseover", e ->
            satir.getStyle().set("background", "rgba(255,255,255,0.10)"));
        satir.getElement().addEventListener("mouseout", e ->
            satir.getStyle().set("background", "transparent"));
        satir.getElement().addEventListener("click", e -> onClick.run());
        return satir;
    }

    public static void open(User user, CompanyService companyService,
                            ActivityLogService activityLogService,
                            RequestService requestService,
                            UserService userService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Profilim");
        dialog.setWidth("480px");

        VerticalLayout icerik = new VerticalLayout();
        icerik.setPadding(false);
        icerik.setSpacing(false);

        // --- Üst: avatar (siluet) + ad + rol ---
        Avatar avatar = new Avatar();
        avatar.getStyle().set("--vaadin-avatar-size", "64px");

        H3 ad = new H3(user.getNameSurname());
        ad.getStyle().set("margin", "0");
        Span rol = new Span(rolTr(user.getRole()));
        rol.getStyle().set("color", "#036baa").set("font-weight", "600").set("font-size", "13px");
        Div adRol = new Div(ad, rol);

        HorizontalLayout baslik = new HorizontalLayout(avatar, adRol);
        baslik.setAlignItems(FlexComponent.Alignment.CENTER);
        baslik.getStyle().set("margin-bottom", "10px");
        icerik.add(baslik);

        icerik.add(bilgiSatiri("E-posta", user.getEmail()));
        if (companyService != null && user.getCompanyId() != null) {
            icerik.add(bilgiSatiri("Şirket", companyService.getName(user.getCompanyId())));
        }
        if (user.getCreatedAt() != null) {
            icerik.add(bilgiSatiri("Üyelik", user.getCreatedAt().format(FMT_DATE)));
        }

        icerik.add(new Hr());

        // --- Son aktivitelerim ---
        Span aktBaslik = new Span("Son Aktivitelerim");
        aktBaslik.getStyle().set("font-weight", "600").set("display", "block").set("margin-bottom", "6px");
        icerik.add(aktBaslik);

        List<ActivityLog> aktiviteler = activityLogService != null
            ? activityLogService.getRecentByUser(user.getUserId()) : List.of();
        if (aktiviteler.isEmpty()) {
            Span yok = new Span("Henüz aktivite yok.");
            yok.getStyle().set("color", "#888").set("font-size", "12px");
            icerik.add(yok);
        } else {
            VerticalLayout liste = new VerticalLayout();
            liste.setPadding(false);
            liste.setSpacing(false);
            liste.setWidthFull();
            liste.getStyle().set("max-height", "200px").set("overflow-y", "auto");
            for (ActivityLog a : aktiviteler) {
                liste.add(aktiviteSatiri(a, requestService));
            }
            icerik.add(liste);
        }

        icerik.add(new Hr());

        // --- Şifre değiştir ---
        Span sifreBaslik = new Span("Şifre Değiştir");
        sifreBaslik.getStyle().set("font-weight", "600").set("display", "block").set("margin-bottom", "6px");
        icerik.add(sifreBaslik);

        PasswordField mevcut = new PasswordField("Mevcut şifre");
        mevcut.setWidthFull();
        mevcut.getElement().setAttribute("autocomplete", "current-password");
        PasswordField yeni = new PasswordField("Yeni şifre");
        yeni.setWidthFull();
        yeni.getElement().setAttribute("autocomplete", "new-password");
        PasswordField yeniTekrar = new PasswordField("Yeni şifre (tekrar)");
        yeniTekrar.setWidthFull();
        yeniTekrar.getElement().setAttribute("autocomplete", "new-password");

        Button degistirBtn = new Button("Şifreyi Değiştir", e -> {
            if (!yeni.getValue().equals(yeniTekrar.getValue())) {
                Notification.show("Yeni şifreler eşleşmiyor.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                userService.changeOwnPassword(user.getUserId(), mevcut.getValue(), yeni.getValue());
                Notification.show("Şifreniz güncellendi.", 3000, Notification.Position.TOP_CENTER);
                mevcut.clear();
                yeni.clear();
                yeniTekrar.clear();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        degistirBtn.getStyle().set("background-color", "#036baa").set("color", "white").set("margin-top", "6px");

        icerik.add(mevcut, yeni, yeniTekrar, degistirBtn);

        dialog.add(icerik);
        dialog.getFooter().add(new Button("Kapat", e -> dialog.close()));
        dialog.open();
    }

    private static HorizontalLayout bilgiSatiri(String etiket, String deger) {
        Span e = new Span(etiket + ":");
        e.getStyle().set("color", "#888").set("font-size", "13px");
        Span d = new Span(deger);
        d.getStyle().set("font-size", "13px").set("font-weight", "500");
        HorizontalLayout h = new HorizontalLayout(e, d);
        h.setSpacing(false);
        h.getStyle().set("gap", "4px").set("margin", "2px 0");
        return h;
    }

    private static Div aktiviteSatiri(ActivityLog a, RequestService requestService) {
        Div row = new Div();
        row.getStyle().set("padding", "6px 0").set("border-bottom", "1px solid #eee");

        String talep = requestService != null
            ? requestService.findById(a.getRequestId()).map(Request::getTitle).orElse("#" + a.getRequestId())
            : "#" + a.getRequestId();

        Span action = new Span(a.getAction() + " — " + talep);
        action.getStyle().set("font-size", "13px").set("display", "block");
        row.add(action);

        Span meta = new Span(a.getCreatedAt().format(FMT));
        meta.getStyle().set("font-size", "11px").set("color", "#8a97a5").set("display", "block");
        row.add(meta);
        return row;
    }

    private static String rolTr(Role role) {
        return switch (role) {
            case CUSTOMER      -> "Müşteri";
            case PRODUCT_OWNER -> "Ürün Sorumlusu";
            case DEVELOPER     -> "Geliştirici";
            case SCRUM_MASTER  -> "Scrum Master";
            case ADMIN         -> "Admin";
        };
    }
}
