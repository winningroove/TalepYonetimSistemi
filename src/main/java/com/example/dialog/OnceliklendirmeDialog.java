package com.example.dialog;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.IsTipi;
import com.example.enums.YoneticiTakdiri;
import com.example.message.MesajPaneli;
import com.example.message.RequestMessageService;
import com.example.prioritization.Prioritization;
import com.example.prioritization.PrioritizationService;
import com.example.request.Request;
import com.example.request.RequestFileService;
import com.example.request.RequestService;
import com.example.user.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Ürün sorumlusunun talep önceliklendirme penceresi.
 * <p>
 * İş etkisi / aciliyet / iş tipi / yönetici takdiri seçilir; skor ve çarpan
 * etkilerinin dağılımı canlı gösterilir. Çaba tahmini (geliştirici müdahalesi)
 * bu aşamada girilmez — onu Scrum Master girer.
 */
public final class OnceliklendirmeDialog {

    private OnceliklendirmeDialog() {}

    public static void open(Request request, Runnable onSuccess, Long currentUserId,
                            PrioritizationService prioritizationService,
                            RequestService requestService,
                            RequestFileService requestFileService,
                            UserService userService,
                            RequestMessageService requestMessageService) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Talep Önceliklendirme — #" + request.getRequestId());
        dialog.setWidth("550px");

        int musteriDegeriPuan = userService.getMusteriDegeriPuan(request.getCustomerId());
        String musteriDegeriLabel = switch (musteriDegeriPuan) {
            case 5 -> "VIP (5)";
            case 4 -> "Büyük (4)";
            case 3 -> "Orta (3)";
            case 2 -> "Küçük (2)";
            default -> "İç Kullanıcı (1)";
        };

        Span talepBaslikSpan = new Span("Talep: " + request.getTitle());
        talepBaslikSpan.getStyle().set("font-weight", "bold");

        Span beklemeSuresi = new Span("Bekleme Süresi Puanı: " +
            prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt()));

        Span musteriDegeriSpan = new Span("Şirket Değeri: " + musteriDegeriLabel);
        musteriDegeriSpan.getStyle().set("color", "#036baa").set("font-weight", "bold");

        // Requester Credibility (güvenilirlik) — talep sahibinin geçmiş performansı
        var credStats = requestService.getCredibilityStats(request.getCustomerId());
        int credibilityScore = prioritizationService.calculateCredibilityScore(request.getCustomerId());
        String credText;
        if (credStats.total() < 5) {
            credText = String.format(
                "Güvenilirlik Skoru: 0 (yetersiz geçmiş — toplam %d talep, en az 5 gerekir)",
                credStats.total());
        } else {
            credText = String.format(
                "Güvenilirlik Skoru: %+d  (Toplam %d Onaylanan %d Reddedilen %d)",
                credibilityScore, credStats.total(), credStats.approved(), credStats.rejected());
        }
        Span credibilitySpan = new Span(credText);
        credibilitySpan.getStyle().set("font-weight", "bold");
        if (credibilityScore > 0)      credibilitySpan.getStyle().set("color", "#1e7e34"); // ödül
        else if (credibilityScore < 0) credibilitySpan.getStyle().set("color", "#c0392b"); // ceza
        else                           credibilitySpan.getStyle().set("color", "#666");

        Span smNotu = new Span("Not: Geliştirici çaba tahmini Scrum Master tarafından girilecektir.");
        smNotu.getStyle().set("color", "#888").set("font-size", "12px").set("font-style", "italic");

        ComboBox<Integer> isEtkisiBox = new ComboBox<>("İş Etkisi");
        isEtkisiBox.setItems(1, 2, 3, 4, 5);
        isEtkisiBox.setItemLabelGenerator(v -> switch (v) {
            case 5 -> "5 - Sistem tamamen çalışmıyor";
            case 4 -> "4 - Kritik iş süreci etkileniyor";
            case 3 -> "3 - Kısmi etki, geçici çözüm var";
            case 2 -> "2 - Küçük etki, işler yavaşlıyor";
            default -> "1 - Kozmetik / Görsel";
        });
        isEtkisiBox.setWidthFull();

        ComboBox<Integer> acilyetBox = new ComboBox<>("Aciliyet");
        acilyetBox.setItems(1, 2, 3, 4, 5);
        acilyetBox.setItemLabelGenerator(v -> switch (v) {
            case 5 -> "5 - Bugün çözülmeli";
            case 4 -> "4 - Bu hafta içinde";
            case 3 -> "3 - Bu ay içinde";
            case 2 -> "2 - Önümüzdeki sprint'e planlanabilir";
            default -> "1 - Esnek, zaman bağımsız";
        });
        acilyetBox.setWidthFull();

        ComboBox<IsTipi> isTipiBox = new ComboBox<>("İş Tipi");
        isTipiBox.setItems(IsTipi.values());
        isTipiBox.setItemLabelGenerator(v -> switch (v) {
            case GUVENLIK_ACIGI  -> "Güvenlik Açığı";
            case KRITIK_BUG      -> "Kritik Bug";
            case BUG             -> "Bug";
            case PERFORMANS      -> "Performans Sorunu";
            case ENTEGRASYON     -> "Entegrasyon";
            case FEATURE_REQUEST -> "Yeni Özellik";
            case ENHANCEMENT     -> "İyileştirme";
            case DOKUMANTASYON   -> "Dokümantasyon";
        });
        isTipiBox.setWidthFull();

        ComboBox<YoneticiTakdiri> takdirBox = new ComboBox<>("Yönetici Takdiri");
        takdirBox.setItems(YoneticiTakdiri.values());
        takdirBox.setItemLabelGenerator(v -> switch (v) {
            case YOK       -> "Yok / Normal (0)";
            case ONEMLI    -> "Önemli — Birim Hedefi (+5)";
            case STRATEJIK -> "Stratejik — Şirket Hedefi (+10)";
            case KRITIK    -> "Kritik — Acil Müdahale (+15)";
        });
        takdirBox.setValue(YoneticiTakdiri.YOK);
        takdirBox.setWidthFull();

        Span skorSpan  = new Span("Tahmini Skor: —");
        Span labelSpan = new Span("");
        skorSpan.getStyle().set("font-weight", "bold").set("font-size", "16px");

        // Skor dağılımı (çarpan etkileri) — her faktörün skora katkısı canlı gösterilir
        Div dagilimBox = new Div();
        dagilimBox.setVisible(false);
        dagilimBox.getStyle()
            .set("background", "rgba(3,107,170,0.05)")
            .set("border", "1px solid rgba(3,107,170,0.20)")
            .set("border-radius", "10px")
            .set("padding", "12px 14px")
            .set("margin-top", "4px");
        Span dagilimBaslik = new Span("Skor Dağılımı (çarpan etkileri)");
        dagilimBaslik.getStyle().set("font-weight", "700").set("display", "block")
            .set("margin-bottom", "8px").set("color", "#036baa").set("font-size", "13px");
        Div dagilimIcerik = new Div();
        dagilimBox.add(dagilimBaslik, dagilimIcerik);

        Runnable skorGuncelle = () -> {
            if (isEtkisiBox.getValue() != null && acilyetBox.getValue() != null
                    && isTipiBox.getValue() != null && takdirBox.getValue() != null) {

                Prioritization temp = new Prioritization();
                temp.setIsEtkisi(isEtkisiBox.getValue());
                temp.setAciliyet(acilyetBox.getValue());
                temp.setMusteriDegeriPuan(musteriDegeriPuan);
                temp.setIsTipi(isTipiBox.getValue());
                temp.setIsTipiPuan(isTipiBox.getValue().getPuan());
                int beklemePuan = prioritizationService.calculateBeklemeSuresiPuan(request.getCreatedAt());
                temp.setBeklemeSuresiPuan(beklemePuan);
                temp.setGelistiriciMudahalesi(GelistiriciMudahalesi.ORTA);

                int credibility = prioritizationService.calculateCredibilityScore(request.getCustomerId());
                YoneticiTakdiri takdir = takdirBox.getValue();
                int takdirPuan = takdir != null ? takdir.getPuan() : 0;
                int skor = prioritizationService.calculateFinalScore(temp, takdir, credibility);
                skorSpan.setText("Tahmini Skor: " + skor);
                labelSpan.setText(" — " + prioritizationService.getLabel(skor));

                // Ağırlıklar ÷ bölen baz skoru verir. Katsayılar koda gömülü değildir;
                // PrioritizationProperties'ten (application.properties ile ayarlanabilir) okunur.
                var a = prioritizationService.getProperties().getAgirlik();
                String bolenStr = fmt(a.getBolen());
                double isEtkisiKatki = isEtkisiBox.getValue() * a.getIsEtkisi() / a.getBolen();
                double aciliyetKatki = acilyetBox.getValue() * a.getAciliyet() / a.getBolen();
                double sirketKatki   = musteriDegeriPuan * a.getMusteriDegeri() / a.getBolen();
                double isTipiKatki   = isTipiBox.getValue().getPuan() * a.getIsTipi() / a.getBolen();
                double beklemeKatki  = beklemePuan * a.getBeklemeSuresi() / a.getBolen();
                double bazSkor = isEtkisiKatki + aciliyetKatki + sirketKatki + isTipiKatki + beklemeKatki;

                dagilimIcerik.removeAll();
                dagilimIcerik.add(
                    dagilimSatiri("İş Etkisi",       isEtkisiBox.getValue() + " × " + a.getIsEtkisi() + " ÷ " + bolenStr, "+" + fmt(isEtkisiKatki), false),
                    dagilimSatiri("Aciliyet",        acilyetBox.getValue() + " × " + a.getAciliyet() + " ÷ " + bolenStr,  "+" + fmt(aciliyetKatki), false),
                    dagilimSatiri("Şirket Değeri",   musteriDegeriPuan + " × " + a.getMusteriDegeri() + " ÷ " + bolenStr, "+" + fmt(sirketKatki),   false),
                    dagilimSatiri("İş Tipi",         isTipiBox.getValue().getPuan() + " × " + a.getIsTipi() + " ÷ " + bolenStr, "+" + fmt(isTipiKatki), false),
                    dagilimSatiri("Bekleme Süresi",  beklemePuan + " × " + a.getBeklemeSuresi() + " ÷ " + bolenStr,        "+" + fmt(beklemeKatki),  false),
                    dagilimAyrac(),
                    dagilimSatiri("Baz Skor",        "", fmt(bazSkor), true),
                    dagilimSatiri("Yönetici Takdiri", takdir != null ? takdirKisa(takdir) : "", isaretli(takdirPuan), false),
                    dagilimSatiri("Güvenilirlik",    "", isaretli(credibility), false),
                    dagilimSatiri("Geliştirici Müdahalesi", "SM girecek", "0", false),
                    dagilimAyrac(),
                    dagilimSatiri("Toplam (0–100)",  "", String.valueOf(skor), true)
                );
                dagilimBox.setVisible(true);
            } else {
                dagilimIcerik.removeAll();
                dagilimBox.setVisible(false);
                skorSpan.setText("Tahmini Skor: —");
                labelSpan.setText("");
            }
        };

        isEtkisiBox.addValueChangeListener(e -> skorGuncelle.run());
        acilyetBox.addValueChangeListener(e -> skorGuncelle.run());
        isTipiBox.addValueChangeListener(e -> skorGuncelle.run());
        takdirBox.addValueChangeListener(e -> skorGuncelle.run());

        Button kaydetBtn = new Button("Değerleri Kaydet", e -> {
            if (isEtkisiBox.getValue() == null || acilyetBox.getValue() == null
                    || isTipiBox.getValue() == null || takdirBox.getValue() == null) {
                Notification.show("Tüm alanları seçiniz.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                Prioritization p = new Prioritization();
                p.setRequestId(request.getRequestId());
                p.setIsEtkisi(isEtkisiBox.getValue());
                p.setAciliyet(acilyetBox.getValue());
                p.setIsTipi(isTipiBox.getValue());

                requestService.updateYoneticiTakdiri(request.getRequestId(), takdirBox.getValue());

                prioritizationService.savePrioritizationByPO(
                    p, request.getCustomerId(), request.getCreatedAt());

                Notification.show("Önceliklendirme kaydedildi. Scrum Master çaba tahminini girecektir.",
                    4000, Notification.Position.TOP_CENTER);
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        kaydetBtn.getStyle().set("background-color", "#1B2A3B").set("color", "white");

        Button iptalBtn = new Button("İptal", e -> dialog.close());

        // Detayları/dosyaları gördükten sonra doğrudan reddetme seçeneği
        Button reddetBtn = new Button("Reddet", e -> {
            dialog.close();
            ReddetDialog.open(request, onSuccess, requestService);
        });
        reddetBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout skorLayout = new HorizontalLayout(skorSpan, labelSpan);
        skorLayout.setAlignItems(Alignment.BASELINE);

        VerticalLayout icerik = new VerticalLayout(
            talepBaslikSpan, beklemeSuresi, musteriDegeriSpan, credibilitySpan, smNotu,
            DialogSupport.dosyaEkleri(requestFileService.getFilesByRequestId(request.getRequestId()), true),
            isEtkisiBox, acilyetBox, isTipiBox,
            takdirBox, skorLayout, dagilimBox
        );
        icerik.setPadding(false);
        // Ekip notları (SM'in geri gönderme gerekçesi dahil) burada da görünür
        icerik.add(MesajPaneli.ekipKanali(request.getRequestId(), currentUserId, requestMessageService, userService));

        dialog.add(icerik);
        dialog.getFooter().add(iptalBtn, reddetBtn, kaydetBtn);
        dialog.open();
    }

    /** Skor dağılımı tablosunda tek satır: faktör adı — hesap — katkı. */
    private static Div dagilimSatiri(String etiket, String hesap, String katki, boolean vurgu) {
        Span e = new Span(etiket);
        e.getStyle().set("flex", "1").set("font-size", "13px");
        if (vurgu) e.getStyle().set("font-weight", "700");

        Span h = new Span(hesap);
        h.getStyle().set("color", "#7a8794").set("font-size", "11px").set("margin", "0 10px");

        Span k = new Span(katki);
        k.getStyle().set("min-width", "44px").set("text-align", "right")
            .set("font-weight", vurgu ? "700" : "600").set("font-size", "13px");
        if (vurgu) k.getStyle().set("color", "#036baa");

        Div row = new Div(e, h, k);
        row.getStyle().set("display", "flex").set("align-items", "baseline").set("padding", "3px 0");
        return row;
    }

    /** Skor dağılımı içinde ince ayraç çizgisi. */
    private static Div dagilimAyrac() {
        Div d = new Div();
        d.getStyle().set("border-top", "1px solid rgba(3,107,170,0.20)").set("margin", "5px 0");
        return d;
    }

    /** Ondalık değeri temiz gösterir: tam sayıysa ondalık kısmı gizler (24.0 → "24"). */
    private static String fmt(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 10) / 10.0);
    }

    /** Eklemeli düzeltici puanı işaretli metne çevirir (+5 / 0 / -10). */
    private static String isaretli(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    /** Yönetici takdirinin dağılım satırında görünecek kısa etiketi. */
    private static String takdirKisa(YoneticiTakdiri t) {
        return switch (t) {
            case YOK       -> "Normal";
            case ONEMLI    -> "Önemli";
            case STRATEJIK -> "Stratejik";
            case KRITIK    -> "Kritik";
        };
    }
}
