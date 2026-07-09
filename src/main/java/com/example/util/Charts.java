package com.example.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Map;

/**
 * Bağımlılıksız hafif grafik bileşenleri (CSS conic-gradient + div'ler).
 * Vaadin Charts ücretli olduğu için gösterge panelinde bunlar kullanılır.
 */
public final class Charts {

    private Charts() {}

    /**
     * Halka (donut) grafik + açıklama listesi.
     * @param veriler etiket → değer (sıra korunur; LinkedHashMap önerilir)
     * @param renkler etiket → renk (hex)
     */
    public static Component donut(Map<String, Integer> veriler, Map<String, String> renkler) {
        int toplam = veriler.values().stream().mapToInt(Integer::intValue).sum();

        Div ringWrap = new Div();
        ringWrap.getStyle().set("position", "relative")
            .set("width", "150px").set("height", "150px").set("flex-shrink", "0");

        Div ring = new Div();
        ring.getStyle().set("width", "150px").set("height", "150px").set("border-radius", "50%");
        if (toplam == 0) {
            ring.getStyle().set("background", "#e0e0e0");
        } else {
            StringBuilder g = new StringBuilder("conic-gradient(");
            double acc = 0;
            boolean first = true;
            for (Map.Entry<String, Integer> e : veriler.entrySet()) {
                double pct = e.getValue() / (double) toplam;
                String c = renkler.getOrDefault(e.getKey(), "#cccccc");
                if (!first) g.append(", ");
                g.append(c).append(" ").append(turn(acc)).append(" ").append(turn(acc + pct));
                acc += pct;
                first = false;
            }
            g.append(")");
            ring.getStyle().set("background", g.toString());
        }

        Div hole = new Div();
        hole.addClassName("donut-hole");
        hole.getStyle().set("position", "absolute").set("width", "92px").set("height", "92px")
            .set("border-radius", "50%").set("top", "29px").set("left", "29px")
            .set("display", "flex").set("flex-direction", "column")
            .set("align-items", "center").set("justify-content", "center");
        Span sayi = new Span(String.valueOf(toplam));
        sayi.getStyle().set("font-size", "24px").set("font-weight", "700");
        Span alt = new Span("Toplam");
        alt.getStyle().set("font-size", "11px").set("color", "#888");
        hole.add(sayi, alt);

        ringWrap.add(ring, hole);

        VerticalLayout legend = new VerticalLayout();
        legend.setPadding(false);
        legend.setSpacing(false);
        for (Map.Entry<String, Integer> e : veriler.entrySet()) {
            Div sw = new Div();
            sw.getStyle().set("width", "12px").set("height", "12px").set("border-radius", "3px")
                .set("background", renkler.getOrDefault(e.getKey(), "#cccccc")).set("flex-shrink", "0");
            Span lbl = new Span(e.getKey() + " (" + e.getValue() + ")");
            lbl.getStyle().set("font-size", "13px");
            HorizontalLayout row = new HorizontalLayout(sw, lbl);
            row.setAlignItems(Alignment.CENTER);
            row.getStyle().set("gap", "8px").set("padding", "3px 0");
            legend.add(row);
        }

        HorizontalLayout box = new HorizontalLayout(ringWrap, legend);
        box.setAlignItems(Alignment.CENTER);
        box.getStyle().set("gap", "20px").set("flex-wrap", "wrap");
        return box;
    }

    /** Dikey çubuk grafik (ör. haftalık trend). Değer üstte, etiket altta. */
    public static Component dikeyCubuklar(List<String> etiketler, List<Integer> degerler, String renk) {
        int max = Math.max(1, degerler.stream().mapToInt(Integer::intValue).max().orElse(1));

        HorizontalLayout chart = new HorizontalLayout();
        chart.setWidthFull();
        chart.setAlignItems(Alignment.END);
        chart.getStyle().set("gap", "10px").set("height", "170px").set("padding-top", "8px");

        for (int i = 0; i < etiketler.size(); i++) {
            int v = degerler.get(i);
            double h = v / (double) max * 120.0; // en yüksek çubuk 120px

            Span deger = new Span(String.valueOf(v));
            deger.getStyle().set("font-size", "12px").set("font-weight", "600");

            Div bar = new Div();
            bar.getStyle().set("width", "100%").set("min-width", "14px")
                .set("height", (v == 0 ? 2 : h) + "px")
                .set("background", "linear-gradient(180deg, #0a86cf 0%, " + renk + " 100%)")
                .set("border-radius", "6px 6px 0 0").set("transition", "height .3s ease");

            Span et = new Span(etiketler.get(i));
            et.getStyle().set("font-size", "11px").set("color", "#888").set("margin-top", "4px");

            Div col = new Div(deger, bar, et);
            col.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("align-items", "center").set("justify-content", "flex-end")
                .set("flex", "1").set("height", "100%");
            chart.add(col);
        }
        return chart;
    }

    private static String turn(double frac) {
        return (Math.round(frac * 10000) / 10000.0) + "turn";
    }
}
