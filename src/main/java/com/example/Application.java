package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

// Tema belirtilmediğinde Vaadin varsayılan olarak LUMO temasını kullanır.
// (Aura kaldırıldı -> Lumo. styles.css'teki --lumo-* değişkenleri artık etkin.)
@SpringBootApplication
@StyleSheet("styles.css?v=4") // Özel stiller (Lumo tabanı üzerine)
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
