package com.example.orders.javafx;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;

class CustomersTab extends Tab implements Refreshable {

    private final Backend backend;
    private final ObservableList<CustomerDto> items = FXCollections.observableArrayList();
    private final FilteredList<CustomerDto> filtered = new FilteredList<>(items);
    private final TableView<CustomerDto> table = new TableView<>(filtered);

    private final TextField search = Ui.field("Suchen ...");
    private final Label formTitle = new Label();
    private final TextField name = Ui.field("Name");
    private final TextField email = Ui.field("E-Mail");
    private final TextField city = Ui.field("Ort");
    private final CheckBox active = new CheckBox("Aktiv");
    private final Button delete = new Button("Löschen");
    private final VBox form = new VBox(8);

    private CustomerDto editing;

    CustomersTab(Backend backend) {
        super("Kunden");
        this.backend = backend;
        setClosable(false);

        table.getColumns().add(Ui.<CustomerDto>column("Name", 220, CustomerDto::name));
        table.getColumns().add(Ui.<CustomerDto>column("E-Mail", 260, CustomerDto::email));
        table.getColumns().add(Ui.<CustomerDto>column("Ort", 130, CustomerDto::city));
        table.getColumns().add(Ui.<CustomerDto>column("Aktiv", 60, c -> c.active() ? "Ja" : "Nein"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, c) -> {
            if (c != null) {
                edit(c);
            }
        });

        search.textProperty().addListener((obs, old, text) -> {
            String q = text.strip().toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> q.isEmpty()
                    || (c.name() + " " + c.email() + " " + c.city()).toLowerCase(Locale.ROOT).contains(q));
        });
        Button add = new Button("Neuer Kunde");
        add.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            edit(new CustomerDto(null, "", "", "", true));
        });

        Button save = new Button("Speichern");
        save.setDefaultButton(true);
        save.setOnAction(e -> save());
        Button cancel = new Button("Abbrechen");
        cancel.setOnAction(e -> form.setVisible(false));
        delete.setOnAction(e -> delete());

        form.getChildren().addAll(formTitle, name, email, city, active, new HBox(8, save, cancel, delete));
        form.setPadding(new Insets(0, 0, 0, 16));
        form.setPrefWidth(300);
        form.setVisible(false);

        VBox left = new VBox(8, new HBox(8, search, add), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox root = new HBox(left, form);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setPadding(new Insets(12));
        setContent(root);
    }

    @Override
    public void refresh() {
        items.setAll(backend.customers.list());
    }

    private void edit(CustomerDto c) {
        editing = c;
        formTitle.setText(c.id() == null ? "Neuer Kunde" : "Kunde bearbeiten");
        name.setText(c.name());
        email.setText(c.email());
        city.setText(c.city() == null ? "" : c.city());
        active.setSelected(c.active());
        delete.setDisable(c.id() == null);
        form.setVisible(true);
        name.requestFocus();
    }

    private void save() {
        CustomerForm input = new CustomerForm(name.getText(), email.getText(), city.getText(), active.isSelected());
        Ui.call(() -> editing.id() == null ? backend.customers.create(input) : backend.customers.update(editing.id(), input))
                .ifPresent(saved -> {
                    form.setVisible(false);
                    refresh();
                });
    }

    private void delete() {
        if (Ui.confirm("Kunde „" + editing.name() + "“ wirklich löschen?")
                && Ui.run(() -> backend.customers.delete(editing.id()))) {
            form.setVisible(false);
            refresh();
        }
    }
}
