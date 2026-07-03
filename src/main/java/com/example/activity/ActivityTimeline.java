package com.example.activity;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Bir talebin aktivite geçmişini dikey zaman çizelgesi olarak gösterir.
 * Kullanıcı adını çözmek için dışarıdan bir çözümleyici (id -> ad) alır.
 */
public class ActivityTimeline extends Composite<Div> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public ActivityTimeline(List<ActivityLog> entries, Function<Long, String> nameResolver) {
        Div root = getContent();
        root.setWidthFull();

        Span baslik = new Span("Aktivite Geçmişi");
        baslik.getStyle()
            .set("font-weight", "600").set("display", "block")
            .set("margin", "14px 0 8px 0").set("color", "#036baa");
        root.add(baslik);

        if (entries == null || entries.isEmpty()) {
            Span yok = new Span("Henüz kayıt yok.");
            yok.getStyle().set("color", "#888").set("font-size", "12px");
            root.add(yok);
            return;
        }

        Div liste = new Div();
        liste.getStyle()
            .set("border-left", "2px solid #d7e3ef")
            .set("margin-left", "6px").set("padding-left", "14px");

        for (ActivityLog a : entries) {
            liste.add(satir(a, nameResolver));
        }
        root.add(liste);
    }

    private Div satir(ActivityLog a, Function<Long, String> nameResolver) {
        Div row = new Div();
        row.getStyle().set("position", "relative").set("margin-bottom", "12px");

        Div nokta = new Div();
        nokta.getStyle()
            .set("position", "absolute").set("left", "-21px").set("top", "3px")
            .set("width", "10px").set("height", "10px").set("border-radius", "50%")
            .set("background", "#036baa").set("box-shadow", "0 0 0 3px rgba(3,107,170,0.15)");
        row.add(nokta);

        Span action = new Span(a.getAction());
        action.getStyle().set("font-weight", "600").set("font-size", "13px").set("display", "block");
        row.add(action);

        if (a.getDetail() != null && !a.getDetail().isBlank()) {
            Span detail = new Span(a.getDetail());
            detail.getStyle().set("font-size", "12px").set("color", "#555").set("display", "block");
            row.add(detail);
        }

        String kim = a.getUserId() != null ? nameResolver.apply(a.getUserId()) : "Sistem";
        Span meta = new Span(kim + " · " + a.getCreatedAt().format(FMT));
        meta.getStyle()
            .set("font-size", "11px").set("color", "#8a97a5")
            .set("display", "block").set("margin-top", "2px");
        row.add(meta);

        return row;
    }
}
