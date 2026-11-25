package com.example.orders.app.ui;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.ProductController;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.OrderLineDto;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.function.Function;

@Route("orders")
@PageTitle("Bestellungen | Orders")
@PermitAll
public class OrdersView extends HorizontalLayout {

    private final OrderController orders;
    private final Grid<OrderDto> grid = new Grid<>(OrderDto.class, false);

    // Detailbereich
    private final VerticalLayout detail = new VerticalLayout();
    private final H3 detailTitle = new H3();
    private final Span detailInfo = new Span();
    private final Grid<OrderLineDto> lines = new Grid<>(OrderLineDto.class, false);
    private final ComboBox<ProductDto> product = new ComboBox<>("Produkt");
    private final IntegerField quantity = new IntegerField("Menge");
    private final HorizontalLayout addLineBar;
    private final Button place = new Button("Aufgeben", VaadinIcon.CHECK.create());
    private final Button ship = new Button("Versenden", VaadinIcon.TRUCK.create());
    private final Button cancel = new Button("Stornieren", VaadinIcon.CLOSE.create());
    private final Grid.Column<OrderLineDto> removeColumn;

    private OrderDto current;

    public OrdersView(OrderController orders, CustomerController customers, ProductController products) {
        this.orders = orders;
        setSizeFull();

        // --- Liste ---
        grid.addColumn(OrderDto::number).setHeader("Nr.").setAutoWidth(true);
        grid.addColumn(OrderDto::customerName).setHeader("Kunde").setSortable(true).setFlexGrow(1);
        grid.addColumn(o -> UiSupport.statusLabel(o.status())).setHeader("Status").setSortable(true);
        grid.addColumn(o -> UiSupport.dateTime(o.createdAt())).setHeader("Erfasst").setAutoWidth(true);
        grid.addColumn(o -> UiSupport.money(o.total(), o.currency())).setHeader("Total")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.asSingleSelect().addValueChangeListener(e -> show(e.getValue()));

        ComboBox<CustomerDto> customer = new ComboBox<>();
        customer.setPlaceholder("Kunde wählen ...");
        customer.setItems(customers.list().stream().filter(CustomerDto::active).toList());
        customer.setItemLabelGenerator(CustomerDto::name);
        customer.setWidth("280px");
        Button create = new Button("Neue Bestellung", VaadinIcon.PLUS.create(), e -> {
            if (customer.isEmpty()) {
                UiSupport.error("Bitte zuerst einen Kunden wählen");
                return;
            }
            UiSupport.call(() -> orders.create(new CreateOrderRequest(customer.getValue().id())))
                    .ifPresent(o -> {
                        refresh();
                        grid.select(findInGrid(o));
                    });
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout list = new VerticalLayout(new HorizontalLayout(customer, create), grid);
        list.setSizeFull();
        list.setPadding(false);

        // --- Detail ---
        lines.addColumn(OrderLineDto::productName).setHeader("Produkt").setFlexGrow(1);
        lines.addColumn(OrderLineDto::quantity).setHeader("Menge").setTextAlign(ColumnTextAlign.END);
        lines.addColumn(l -> UiSupport.money(l.unitPrice(), current.currency())).setHeader("Preis")
                .setTextAlign(ColumnTextAlign.END);
        lines.addColumn(l -> UiSupport.money(l.lineTotal(), current.currency())).setHeader("Total")
                .setTextAlign(ColumnTextAlign.END);
        removeColumn = lines.addComponentColumn(l -> {
            Button remove = new Button(VaadinIcon.TRASH.create(),
                    e -> update(id -> orders.removeLine(id, l.productId())));
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setAriaLabel("Position entfernen");
            return remove;
        }).setAutoWidth(true).setFlexGrow(0);
        lines.setAllRowsVisible(true);

        product.setItems(products.list().stream().filter(ProductDto::active).toList());
        product.setItemLabelGenerator(p -> p.name() + " (" + UiSupport.money(p.price(), p.currency()) + ")");
        product.setWidth("320px");
        quantity.setValue(1);
        quantity.setMin(1);
        quantity.setStepButtonsVisible(true);
        quantity.setWidth("130px");
        Button addLine = new Button("Hinzufügen", VaadinIcon.PLUS.create(), e -> {
            if (product.isEmpty() || quantity.isEmpty()) {
                UiSupport.error("Produkt und Menge angeben");
                return;
            }
            update(id -> orders.addLine(id, new AddLineRequest(product.getValue().id(), quantity.getValue())));
            product.clear();
            quantity.setValue(1);
        });
        addLineBar = new HorizontalLayout(product, quantity, addLine);
        addLineBar.setAlignItems(FlexComponent.Alignment.BASELINE);

        place.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        place.addClickListener(e -> update(orders::place));
        ship.addClickListener(e -> update(orders::ship));
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.addClickListener(e -> update(orders::cancel));

        detail.add(detailTitle, detailInfo, lines, addLineBar, new HorizontalLayout(place, ship, cancel));
        detail.setWidth("620px");
        detail.setVisible(false);

        add(list, detail);
        refresh();
    }

    private void refresh() {
        grid.setItems(orders.list());
    }

    private OrderDto findInGrid(OrderDto order) {
        return grid.getListDataView().getItems()
                .filter(o -> o.id().equals(order.id())).findFirst().orElse(null);
    }

    /** Aktion auf der aktuellen Bestellung ausführen, danach Liste und Detail aktualisieren. */
    private void update(Function<String, OrderDto> action) {
        UiSupport.call(() -> action.apply(current.id())).ifPresent(updated -> {
            refresh();
            OrderDto inGrid = findInGrid(updated);
            grid.select(inGrid);
            show(inGrid);
        });
    }

    private void show(OrderDto o) {
        current = o;
        detail.setVisible(o != null);
        if (o == null) {
            return;
        }
        boolean draft = "DRAFT".equals(o.status());
        detailTitle.setText("Bestellung " + o.number() + " · " + o.customerName());
        detailInfo.setText("Status: " + UiSupport.statusLabel(o.status())
                + (o.placedAt() != null ? " · aufgegeben " + UiSupport.dateTime(o.placedAt()) : "")
                + " · Total " + UiSupport.money(o.total(), o.currency()));
        lines.setItems(o.lines());
        removeColumn.setVisible(draft);
        addLineBar.setVisible(draft);
        place.setVisible(draft);
        ship.setVisible("PLACED".equals(o.status()));
        cancel.setVisible(draft || "PLACED".equals(o.status()));
    }
}
