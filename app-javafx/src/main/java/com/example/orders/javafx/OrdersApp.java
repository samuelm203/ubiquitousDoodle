package com.example.orders.javafx;

import com.example.orders.Backend;
import com.example.orders.DemoData;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * JavaFX-Einstieg. Das Backend ist dasselbe wie bei Spring/Vaadin; hier wird es von Hand
 * verdrahtet (InMemoryBackend) und die Tabs rufen nur dessen Controller auf.
 */
public class OrdersApp extends Application {

    @Override
    public void start(Stage stage) {
        Backend backend = InMemoryBackend.create();
        DemoData.seed(backend);

        List<Refreshable> tabs = List.of(
                new DashboardTab(backend),
                new OrdersTab(backend),
                new CustomersTab(backend),
                new ProductsTab(backend));

        TabPane pane = new TabPane();
        for (Refreshable t : tabs) {
            pane.getTabs().add((Tab) t);
        }
        // Beim Wechsel neu laden, damit z. B. neue Kunden in der Bestell-Auswahl erscheinen
        pane.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> ((Refreshable) tab).refresh());
        ((Refreshable) pane.getSelectionModel().getSelectedItem()).refresh();

        stage.setTitle("Orders (JavaFX)");
        stage.setScene(new Scene(pane, 1100, 650));
        stage.show();

        // Nur für automatische Smoke-Tests: -Dorders.exitAfterMs=3000
        String exitAfter = System.getProperty("orders.exitAfterMs");
        if (exitAfter != null) {
            PauseTransition pause = new PauseTransition(Duration.millis(Long.parseLong(exitAfter)));
            pause.setOnFinished(e -> Platform.exit());
            pause.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
