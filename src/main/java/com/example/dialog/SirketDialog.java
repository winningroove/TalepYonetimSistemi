package com.example.dialog;

import com.example.company.Company;
import com.example.company.CompanyService;
import com.example.enums.MusteriDegeri;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Function;

/** Şirket ekleme / düzenleme penceresi (Admin paneli). */
public final class SirketDialog {

    private SirketDialog() {}

    public static void yeni(CompanyService companyService, Runnable onSuccess,
                            Function<MusteriDegeri, String> degerLabel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yeni Şirket Ekle");
        dialog.setWidth("420px");

        TextField adField = new TextField("Şirket Adı");
        adField.setWidthFull();

        ComboBox<MusteriDegeri> degerBox = degerBox(degerLabel);

        Button kaydetBtn = kaydetButonu("Kaydet", () -> {
            companyService.createCompany(adField.getValue(), degerBox.getValue());
            Notification.show("Şirket oluşturuldu.", 3000, Notification.Position.TOP_CENTER);
        }, dialog, onSuccess);

        dialog.add(new VerticalLayout(adField, degerBox));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), kaydetBtn);
        dialog.open();
    }

    public static void duzenle(Company company, CompanyService companyService, Runnable onSuccess,
                               Function<MusteriDegeri, String> degerLabel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Şirket Düzenle — " + company.getName());
        dialog.setWidth("420px");

        TextField adField = new TextField("Şirket Adı");
        adField.setValue(company.getName());
        adField.setWidthFull();

        ComboBox<MusteriDegeri> degerBox = degerBox(degerLabel);
        if (company.getMusteriDegeri() != null) {
            degerBox.setValue(company.getMusteriDegeri());
        }

        Button kaydetBtn = kaydetButonu("Kaydet", () -> {
            companyService.updateCompany(company.getCompanyId(), adField.getValue(), degerBox.getValue());
            Notification.show("Şirket güncellendi.", 3000, Notification.Position.TOP_CENTER);
        }, dialog, onSuccess);

        dialog.add(new VerticalLayout(adField, degerBox));
        dialog.getFooter().add(new Button("İptal", e -> dialog.close()), kaydetBtn);
        dialog.open();
    }

    private static ComboBox<MusteriDegeri> degerBox(Function<MusteriDegeri, String> degerLabel) {
        ComboBox<MusteriDegeri> degerBox = new ComboBox<>("Şirket Değeri");
        degerBox.setItems(MusteriDegeri.values());
        degerBox.setItemLabelGenerator(degerLabel::apply);
        degerBox.setWidthFull();
        return degerBox;
    }

    private static Button kaydetButonu(String metin, Runnable islem, Dialog dialog, Runnable onSuccess) {
        Button btn = new Button(metin, e -> {
            try {
                islem.run();
                dialog.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        btn.getStyle().set("background-color", "#1B2A3B").set("color", "white");
        return btn;
    }
}
