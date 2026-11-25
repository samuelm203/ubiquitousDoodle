package com.example.orders.javafx;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.OrderLineDto;
import com.example.orders.controller.dto.Dtos.ProductDto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.function.Function;

class OrdersTab extends Tab implements Refreshable {

    private final Backend backend;
    private final ObservableList<OrderDto> orders = FXCollections.observableArrayList();
    private final TableView<OrderDto> table = new TableView<>(orders);

    private final ComboBox<CustomerDto> customer = new ComboBox<>();

    private final VBox detail = new VBox(8);
    private final Label title = new Label();
    private final Label info = new Label();
    private final TableView<OrderLineDto> lines = new TableView<>();
    private final ComboBox<ProductDto> product = new ComboBox<>();
    private final Spinner<Integer> quantity = new Spinner<>(1, 1000, 1);
    private final HBox addLineBar;
    private final Button place = new Button("Aufgeben");
    private final Button ship = new Button("Versenden");
    private final Button cancel = new Button("Stornieren");

    private OrderDto current;
    private boolean updating;

    OrdersTab(Backend backend) {
        super("Bestellungen");
        this.backend = backend;
        setClosable(false);

        // --- Liste ---
        table.getColumns().add(Ui.<OrderDto>column("Nr.", 90, OrderDto::number));
        table.getColumns().add(Ui.<OrderDto>column("Kunde", 200, OrderDto::customerName));
        table.getColumns().add(Ui.<OrderDto>column("Status", 100, o -> Ui.statusLabel(o.status())));
        table.getColumns().add(Ui.<OrderDto>column("Erfasst", 130, o -> Ui.dateTime(o.createdAt())));
        TableColumn<OrderDto, String> total = Ui.column("Total", 120, o -> Ui.money(o.total(), o.currency()));
        total.setStyle("-fx-alignment: CENTER-RIGHT;");
        table.getColumns().add(total);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, o) -> {
            if (!updating) {
                show(o);
            }
        });

        customer.setPromptText("Kunde wählen ...");
        customer.setConverter(converter(CustomerDto::name));
        customer.setPrefWidth(260);
        Button create = new Button("Neue Bestellung");
        create.setOnAction(e -> {
            if (customer.getValue() == null) {
                Ui.run(() -> {
                    throw new com.example.orders.model.DomainException("Bitte zuerst einen Kunden wählen");
                });
                return;
            }
            Ui.call(() -> backend.orders.create(new CreateOrderRequest(customer.getValue().id())))
                    .ifPresent(o -> reload(o.id()));
        });

        VBox left = new VBox(8, new HBox(8, customer, create), table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // --- Detail ---
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        lines.getColumns().add(Ui.<OrderLineDto>column("Produkt", 190, OrderLineDto::productName));
        lines.getColumns().add(Ui.<OrderLineDto>column("Menge", 60, l -> String.valueOf(l.quantity())));
        lines.getColumns().add(Ui.<OrderLineDto>column("Preis", 90, l -> Ui.money(l.unitPrice(), current.currency())));
        lines.getColumns().add(Ui.<OrderLineDto>column("Total", 100, l -> Ui.money(l.lineTotal(), current.currency())));
        lines.setPrefHeight(220);

        Button remove = new Button("Position entfernen");
        remove.setOnAction(e -> {
            OrderLineDto l = lines.getSelectionModel().getSelectedItem();
            if (l != null) {
                update(id -> backend.orders.removeLine(id, l.productId()));
            }
        });
        remove.disableProperty().bind(lines.getSelectionModel().selectedItemProperty().isNull());

        product.setPromptText("Produkt wählen ...");
        product.setConverter(converter(p -> p.name() + " (" + Ui.money(p.price(), p.currency()) + ")"));
        product.setPrefWidth(260);
        quantity.setEditable(true);
        quantity.setPrefWidth(90);
        Button addLine = new Button("Hinzufügen");
        addLine.setOnAction(e -> {
            if (product.getValue() == null) {
                return;
            }
            update(id -> backend.orders.addLine(id, new AddLineRequest(product.getValue().id(), quantity.getValue())));
            product.setValue(null);
            quantity.getValueFactory().setValue(1);
        });
        addLineBar = new HBox(8, product, quantity, addLine, remove);

        place.setDefaultButton(true);
        place.setOnAction(e -> update(backend.orders::place));
        ship.setOnAction(e -> update(backend.orders::ship));
        cancel.setOnAction(e -> {
            if (Ui.confirm("Bestellung stornieren?")) {
                update(backend.orders::cancel);
            }
        });

        detail.getChildren().addAll(title, info, lines, addLineBar, new HBox(8, place, ship, cancel));
        detail.setPadding(new Insets(0, 0, 0, 16));
        detail.setPrefWidth(540);
        detail.setVisible(false);

        HBox root = new HBox(left, detail);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setPadding(new Insets(12));
        setContent(root);
    }

    @Override
    public void refresh() {
        customer.getItems().setAll(backend.customers.list().stream().filter(CustomerDto::active).toList());
        product.getItems().setAll(backend.products.list().stream().filter(ProductDto::active).toList());
        reload(current == null ? null : current.id());
    }

    /** Liste neu laden und, falls möglich, die Bestellung mit dieser ID wieder auswählen. */
    private void reload(String selectId) {
        updating = true;
        try {
            orders.setAll(backend.orders.list());
            OrderDto sel = selectId == null ? null
                    : orders.stream().filter(o -> o.id().equals(selectId)).findFirst().orElse(null);
            table.getSelectionModel().select(sel);
            show(sel);
        } finally {
            updating = false;
        }
    }

    private void update(Function<String, OrderDto> action) {
        Ui.call(() -> action.apply(current.id())).ifPresent(updated -> reload(updated.id()));
    }

    private void show(OrderDto o) {
        current = o;
        detail.setVisible(o != null);
        if (o == null) {
            return;
        }
        boolean draft = "DRAFT".equals(o.status());
        title.setText("Bestellung " + o.number() + " · " + o.customerName());
        info.setText("Status: " + Ui.statusLabel(o.status())
                + (o.placedAt() != null ? " · aufgegeben " + Ui.dateTime(o.placedAt()) : "")
                + " · Total " + Ui.money(o.total(), o.currency()));
        lines.getItems().setAll(o.lines());
        addLineBar.setVisible(draft);
        addLineBar.setManaged(draft);
        place.setVisible(draft);
        place.setManaged(draft);
        boolean placed = "PLACED".equals(o.status());
        ship.setVisible(placed);
        ship.setManaged(placed);
        cancel.setVisible(draft || placed);
        cancel.setManaged(draft || placed);
    }

    private static <T> StringConverter<T> converter(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : label.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
