package com.example.orders.app.ui;

import com.example.orders.controller.ProductController;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route("products")
@PageTitle("Produkte | Orders")
@RolesAllowed("ADMIN")
public class ProductsView extends HorizontalLayout {

    private final ProductController controller;
    private final Grid<ProductDto> grid = new Grid<>(ProductDto.class, false);

    private final H3 formTitle = new H3();
    private final TextField sku = new TextField("SKU");
    private final TextField name = new TextField("Name");
    private final BigDecimalField price = new BigDecimalField("Preis (CHF)");
    private final Checkbox active = new Checkbox("Lieferbar");
    private final VerticalLayout form;

    private ProductDto editing;

    public ProductsView(ProductController controller) {
        this.controller = controller;
        setSizeFull();

        grid.addColumn(ProductDto::sku).setHeader("SKU").setSortable(true).setAutoWidth(true);
        grid.addColumn(ProductDto::name).setHeader("Name").setSortable(true).setFlexGrow(1);
        grid.addColumn(p -> UiSupport.money(p.price(), p.currency())).setHeader("Preis")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.addColumn(p -> p.active() ? "Ja" : "Nein").setHeader("Lieferbar");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        Button add = new Button("Neues Produkt", VaadinIcon.PLUS.create(), e -> {
            grid.asSingleSelect().clear();
            edit(new ProductDto(null, "", "", BigDecimal.ZERO, "CHF", true));
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout list = new VerticalLayout(add, grid);
        list.setSizeFull();
        list.setPadding(false);

        Button save = new Button("Speichern", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Abbrechen", e -> grid.asSingleSelect().clear());

        form = new VerticalLayout(formTitle, new FormLayout(sku, name, price, active), new HorizontalLayout(save, cancel));
        form.setWidth("380px");
        form.setVisible(false);

        add(list, form);
        refresh();
    }

    private void refresh() {
        grid.setItems(controller.list());
    }

    private void edit(ProductDto p) {
        editing = p;
        form.setVisible(p != null);
        if (p == null) {
            return;
        }
        formTitle.setText(p.id() == null ? "Neues Produkt" : "Produkt bearbeiten");
        sku.setValue(p.sku());
        sku.setReadOnly(p.id() != null);
        name.setValue(p.name());
        price.setValue(p.price());
        active.setValue(p.active());
    }

    private void save() {
        ProductForm input = new ProductForm(sku.getValue(), name.getValue(), price.getValue(), active.getValue());
        UiSupport.call(() -> editing.id() == null ? controller.create(input) : controller.update(editing.id(), input))
                .ifPresent(saved -> {
                    UiSupport.success("Produkt gespeichert");
                    refresh();
                    grid.asSingleSelect().clear();
                });
    }
}
