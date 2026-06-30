package com.example.util;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Grid'ler için basit istemci-tarafı arama kutusu üretir.
 * <p>
 * Performans: "aranabilir metin" her satır için YALNIZCA BİR KEZ (kutu
 * oluşturulurken) hesaplanır ve bellekte saklanır. Böylece her tuş vuruşunda
 * veritabanına gidilmez; arama saf bellek-içi metin karşılaştırmasıdır.
 */
public final class GridSearch {

    private GridSearch() {}

    public static <T> TextField create(Grid<T> grid, List<T> items,
                                       String placeholder, Function<T, String> searchable) {
        // Aranabilir metinleri bir kez hesapla (DB erişimleri burada biter).
        Map<T, String> index = new IdentityHashMap<>();
        for (T item : items) {
            String text = searchable.apply(item);
            index.put(item, text == null ? "" : text.toLowerCase());
        }

        TextField field = new TextField();
        field.setPlaceholder(placeholder);
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setValueChangeTimeout(200);
        field.setWidth("340px");
        field.setPrefixComponent(VaadinIcon.SEARCH.create());

        field.addValueChangeListener(e -> {
            String q = e.getValue() == null ? "" : e.getValue().trim().toLowerCase();
            if (q.isEmpty()) {
                grid.setItems(items);
                return;
            }
            grid.setItems(items.stream()
                    .filter(x -> index.getOrDefault(x, "").contains(q))
                    .toList());
        });

        return field;
    }
}
