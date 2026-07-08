package com.example.prioritization;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Admin ekranı: önceliklendirme skoru katsayı ve eşiklerini tarayıcıdan düzenleme.
 * Değerler DB'ye yazılır ({@link PrioritizationConfigService}) ve uygulamayı
 * yeniden başlatmadan anında geçerli olur.
 */
public class SkorAyarlariForm extends Composite<VerticalLayout> {

    /** Tek bir ayar alanının bileşeni ve oku/yaz kancaları. */
    private record Alan(String key, com.vaadin.flow.component.Component comp,
                        Supplier<Double> oku, Consumer<Double> yaz) {}

    private final PrioritizationConfigService configService;
    private final List<Alan> alanlar = new ArrayList<>();

    public SkorAyarlariForm(PrioritizationConfigService configService) {
        this.configService = configService;

        VerticalLayout kok = getContent();
        kok.setPadding(false);

        kok.add(new H2("Skor Ayarları"));
        Paragraph aciklama = new Paragraph(
            "Önceliklendirme skorunun katsayıları ve eşikleri. Değişiklikler kaydedilince "
            + "hesaplama anında yeni değerlerle yapılır (yeniden başlatma gerekmez). "
            + "Not: Ağırlıkların toplamı bölene bölününce skorun 0–100 aralığını korumasına dikkat edin.");
        aciklama.getStyle().set("color", "#666").set("font-size", "13px").set("max-width", "620px");
        kok.add(aciklama);

        kok.add(bolum("Baz Skor Ağırlıkları", List.of(
            intAlan("agirlik.isEtkisi",      "İş Etkisi ağırlığı"),
            intAlan("agirlik.aciliyet",      "Aciliyet ağırlığı"),
            intAlan("agirlik.musteriDegeri", "Şirket Değeri ağırlığı"),
            intAlan("agirlik.isTipi",        "İş Tipi ağırlığı"),
            intAlan("agirlik.beklemeSuresi", "Bekleme Süresi ağırlığı"),
            dblAlan("agirlik.bolen",         "Bölen"))));

        kok.add(bolum("Etiket Eşikleri (üst sınır)", List.of(
            intAlan("etiket.cokDusuk", "ÇOK DÜŞÜK ≤"),
            intAlan("etiket.dusuk",    "DÜŞÜK ≤"),
            intAlan("etiket.orta",     "ORTA ≤"),
            intAlan("etiket.yuksek",   "YÜKSEK ≤"))));

        kok.add(bolum("Güvenilirlik (talep sahibi geçmişi)", List.of(
            intAlan("guvenilirlik.minTalep",      "Min. talep sayısı"),
            dblAlan("guvenilirlik.redOraniEsik",  "Red oranı eşiği (0–1)"),
            dblAlan("guvenilirlik.onayOraniEsik", "Onay oranı eşiği (0–1)"),
            intAlan("guvenilirlik.ceza",          "Ceza puanı"),
            intAlan("guvenilirlik.odul",          "Ödül puanı"))));

        kok.add(bolum("Bekleme Süresi Puanları (1–5)", List.of(
            intAlan("bekleme.cokUzunPuan",    "≥ 30 gün"),
            intAlan("bekleme.uzunPuan",       "≥ 14 gün"),
            intAlan("bekleme.ortaPuan",       "≥ 7 gün"),
            intAlan("bekleme.kisaPuan",       "≥ 3 gün"),
            intAlan("bekleme.varsayilanPuan", "< 3 gün"))));

        Button kaydetBtn = new Button("Kaydet", e -> kaydet());
        kaydetBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        kaydetBtn.getStyle().set("background-color", "#036baa");

        Button varsayilanBtn = new Button("Varsayılana Dön", e -> {
            try {
                configService.resetToDefaults();
                alanlariYenile();
                Notification.show("Varsayılan değerlere dönüldü.", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                hataGoster(ex);
            }
        });
        varsayilanBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout butonlar = new HorizontalLayout(kaydetBtn, varsayilanBtn);
        butonlar.getStyle().set("margin-top", "12px");
        kok.add(butonlar);
    }

    private VerticalLayout bolum(String baslik, List<Alan> bolumAlanlari) {
        H4 h = new H4(baslik);
        h.getStyle().set("margin-bottom", "4px").set("color", "#036baa");

        FormLayout form = new FormLayout();
        form.getStyle().set("max-width", "560px");
        for (Alan a : bolumAlanlari) {
            form.add(a.comp());
            alanlar.add(a);
        }

        VerticalLayout box = new VerticalLayout(h, form);
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("margin-top", "10px");
        return box;
    }

    private Alan intAlan(String key, String etiket) {
        IntegerField f = new IntegerField(etiket);
        f.setValue((int) Math.round(configService.get(key)));
        f.setStepButtonsVisible(true);
        f.setWidthFull();
        return new Alan(key, f,
            () -> f.getValue() == null ? null : f.getValue().doubleValue(),
            v -> f.setValue((int) Math.round(v)));
    }

    private Alan dblAlan(String key, String etiket) {
        NumberField f = new NumberField(etiket);
        f.setValue(configService.get(key));
        f.setStep(0.05);
        f.setWidthFull();
        return new Alan(key, f,
            f::getValue,
            f::setValue);
    }

    private void kaydet() {
        Map<String, Number> degerler = new LinkedHashMap<>();
        for (Alan a : alanlar) {
            Double v = a.oku().get();
            if (v == null) {
                Notification.show("Tüm alanları doldurun.", 3000, Notification.Position.MIDDLE);
                return;
            }
            degerler.put(a.key(), v);
        }
        try {
            configService.save(degerler);
            Notification.show("Skor ayarları kaydedildi ve uygulandı.", 3000, Notification.Position.TOP_CENTER);
        } catch (Exception ex) {
            hataGoster(ex);
        }
    }

    private void alanlariYenile() {
        for (Alan a : alanlar) {
            a.yaz().accept(configService.get(a.key()));
        }
    }

    /** DB hatasını (ör. tablo yok) kullanıcıya anlaşılır biçimde gösterir. */
    private void hataGoster(Exception ex) {
        String mesaj = "Kaydedilemedi: " + ex.getMessage();
        if (ex.getMessage() != null && ex.getMessage().contains("ORA-00942")) {
            mesaj = "Config tablosu bulunamadı. Önce migration_prioritization_config.sql "
                + "dosyasındaki tabloyu Oracle'da oluşturun.";
        }
        Notification n = Notification.show(mesaj, 6000, Notification.Position.MIDDLE);
        n.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
    }
}
