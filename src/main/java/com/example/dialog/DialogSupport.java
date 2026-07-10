package com.example.dialog;

import com.example.enums.WorkflowStatus;
import com.example.request.RequestFile;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Base64;
import java.util.List;

/**
 * Dialoglar arasında paylaşılan küçük UI yardımcıları.
 * <p>
 * Daha önce {@code formatFileSize}, {@code workflowBadge} ve ekli dosya linkleri
 * her view içinde ayrı ayrı kopyalanmıştı; tek yerden gelmesi için buraya alındı.
 */
public final class DialogSupport {

    private DialogSupport() {}

    /** Bayt cinsinden boyutu okunabilir metne çevirir (B / KB / MB). */
    public static String formatFileSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    /** İş akışı durumunu renkli bir rozet (badge) olarak döndürür. */
    public static Span workflowBadge(WorkflowStatus status) {
        String label = switch (status) {
            case BACKLOG     -> "Sırada";
            case IN_PROGRESS -> "Devam Ediyor";
            case TESTING     -> "Test Aşamasında";
            case DONE        -> "✓ Tamamlandı";
        };
        Span badge = new Span(label);
        badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px")
            .set("font-size", "12px").set("font-weight", "bold");
        switch (status) {
            case BACKLOG     -> badge.getStyle().set("background", "#fff3cd").set("color", "#856404");
            case IN_PROGRESS -> badge.getStyle().set("background", "#d1ecf1").set("color", "#0c5460");
            case TESTING     -> badge.getStyle().set("background", "#ffe8cc").set("color", "#7d3c00");
            case DONE        -> badge.getStyle().set("background", "#d4edda").set("color", "#155724");
        }
        return badge;
    }

    /**
     * Talebe ekli dosyaların indirilebilir link listesini üretir.
     *
     * @param dosyalar      talebe ekli dosyalar
     * @param bosMesajGoster liste boşsa "Ekli dosya yok." metni gösterilsin mi
     */
    public static VerticalLayout dosyaEkleri(List<RequestFile> dosyalar, boolean bosMesajGoster) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        if (dosyalar == null || dosyalar.isEmpty()) {
            if (bosMesajGoster) {
                Span yok = new Span("Ekli dosya yok.");
                yok.getStyle().set("color", "#888").set("font-size", "12px");
                layout.add(yok);
            }
            return layout;
        }

        H4 dosyaBaslik = new H4("Ekli Dosyalar");
        dosyaBaslik.getStyle().set("margin-bottom", "4px");
        layout.add(dosyaBaslik);

        for (RequestFile dosya : dosyalar) {
            Anchor link = new Anchor(
                "data:application/octet-stream;base64," +
                Base64.getEncoder().encodeToString(dosya.getFileData()),
                "📎 " + dosya.getFileName() + " (" + formatFileSize(dosya.getFileSize()) + ")"
            );
            link.getElement().setAttribute("download", dosya.getFileName());
            link.getStyle().set("display", "block").set("margin-bottom", "4px");
            layout.add(link);
        }
        return layout;
    }
}
