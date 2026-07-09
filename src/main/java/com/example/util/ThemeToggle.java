package com.example.util;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;

/**
 * Açık/Koyu tema geçiş düğmesi. Kök öğeye {@code data-theme="dark"} ekler/kaldırır
 * ve tercihi {@code localStorage}'a yazar; her sayfa açılışında kayıtlı tercihi uygular.
 * Karartma stilleri styles.css içindeki {@code html[data-theme="dark"]} bloğundadır.
 */
public class ThemeToggle extends Composite<Button> {

    public ThemeToggle() {
        Button b = getContent();
        b.setText("🌓 Tema");
        b.getElement().setAttribute("title", "Açık / Koyu tema");
        b.getStyle()
            .set("color", "white")
            .set("background", "rgba(255,255,255,0.07)")
            .set("border", "none")
            .set("border-radius", "6px")
            .set("width", "100%")
            .set("cursor", "pointer")
            .set("padding", "8px 14px")
            .set("font-size", "13px")
            .set("margin-bottom", "4px");

        b.addClickListener(e -> b.getElement().executeJs(
            "const r=document.documentElement;"
            + "const dark=r.getAttribute('data-theme')==='dark';"
            + "if(dark){r.removeAttribute('data-theme');}else{r.setAttribute('data-theme','dark');}"
            + "try{localStorage.setItem('tys-theme', dark ? 'light' : 'dark');}catch(err){}"
        ));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Sayfa açılışında kayıtlı tercihi uygula.
        getContent().getElement().executeJs(
            "try{if(localStorage.getItem('tys-theme')==='dark'){"
            + "document.documentElement.setAttribute('data-theme','dark');}}catch(err){}"
        );
    }
}
