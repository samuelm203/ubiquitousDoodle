package com.example.orders.javafx;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

class ProductsTab extends Tab implements Refreshable {

    private final Backend backend;
    private final ObservableList<ProductDto> items = FXCollections.observableArrayList();
    private final TableView<ProductDto> table = new TableView<>(items);

    private final Label formTitle = new Label();
    private final TextField sku = Ui.field("SKU");
    private final TextField name = Ui.field("Name");
    private final TextField price = Ui.field("Preis (CHF)");
    private final CheckBox active = new CheckBox("Lieferbar");
    private final VBox form = new VBox(8);

    private ProductDto editing;

    ProductsTab(Backend backend) {
        super("Produkte");
        this.backend = backend;
        setClosable(false);

        TableColumn<ProductDto, String> priceCol =
                Ui.column("Preis", 120, p -> Ui.money(p.price(), p.currency()));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        table.getColumns().add(Ui.<ProductDto>column("SKU", 110, ProductDto::sku));
        table.getColumns().add(Ui.<ProductDto>column("Name", 260, ProductDto::name));
        table.getColumns().add(priceCol);
        table.getColumns().add(Ui.<ProductDto>column("Lieferbar", 80, p -> p.active() ? "Ja" : "Nein"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, p) -> {
            if (p != null) {
                edit(p);
            }
        });

        Button add = new Button("Neues Produkt");
        add.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            edit(new ProductDto(null, "", "", BigDecimal.ZERO, "CHF", true));
        });
        Button save = new Button("Speichern");
        save.setDefaultButton(true);
        save.setOnAction(e -> save());
        Button cancel = new Button("Abbrechen");
        cancel.setOnAction(e -> form.setVisible(false));

        form.getChildren().addAll(formTitle, sku, name, price, active, new HBox(8, save, cancel));
        form.setPadding(new Insets(0, 0, 0, 16));
        form.setPrefWidth(300);
        form.setVisible(false);

        VBox left = new VBox(8, add, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox root = new HBox(left, form);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setPadding(new Insets(12));
        root.setAlignment(Pos.TOP_LEFT);
        setContent(root);
    }

    @Override
    public void refresh() {
        items.setAll(backend.products.list());
    }

    private void edit(ProductDto p) {
        editing = p;
        formTitle.setText(p.id() == null ? "Neues Produkt" : "Produkt bearbeiten");
        sku.setText(p.sku());
        sku.setEditable(p.id() == null); // SKU ist ein fachlicher Schlüssel
        name.setText(p.name());
        price.setText(p.price().toPlainString());
        active.setSelected(p.active());
        form.setVisible(true);
        name.requestFocus();
    }

    private void save() {
        BigDecimal amount;
        try {
            amount = new BigDecimal(price.getText().strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            Ui.run(() -> {
                throw new com.example.orders.model.DomainException("Ungültiger Preis: " + price.getText());
            });
            return;
        }
        ProductForm input = new ProductForm(sku.getText(), name.getText(), amount, active.isSelected());
        Ui.call(() -> editing.id() == null ? backend.products.create(input) : backend.products.update(editing.id(), input))
                .ifPresent(saved -> {
                    form.setVisible(false);
                    refresh();
                });
    }
}
