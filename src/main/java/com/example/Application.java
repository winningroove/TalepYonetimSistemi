package com.example;

import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

// Aura teması: özel CSS ve inline stiller bu tabana göre ayarlandı.
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css?v=5") // Özel stiller (v ile önbellek kırma)
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
