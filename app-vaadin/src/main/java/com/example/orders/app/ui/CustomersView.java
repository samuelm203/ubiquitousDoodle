package com.example.orders.app.ui;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Locale;

/** View: zeigt Daten an und leitet Benutzeraktionen an den CustomerController weiter. */
@Route("customers")
@PageTitle("Kunden | Orders")
@PermitAll
public class CustomersView extends HorizontalLayout {

    private final CustomerController controller;
    private final Grid<CustomerDto> grid = new Grid<>(CustomerDto.class, false);
    private final TextField filter = new TextField();

    private final H3 formTitle = new H3();
    private final TextField name = new TextField("Name");
    private final EmailField email = new EmailField("E-Mail");
    private final TextField city = new TextField("Ort");
    private final Checkbox active = new Checkbox("Aktiv");
    private final Button delete = new Button("Löschen", VaadinIcon.TRASH.create());
    private final VerticalLayout form;

    private List<CustomerDto> all = List.of();
    private CustomerDto editing;

    public CustomersView(CustomerController controller, AuthenticationContext auth) {
        this.controller = controller;
        setSizeFull();

        grid.addColumn(CustomerDto::name).setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.addColumn(CustomerDto::email).setHeader("E-Mail").setSortable(true).setAutoWidth(true);
        grid.addColumn(CustomerDto::city).setHeader("Ort").setSortable(true);
        grid.addColumn(c -> c.active() ? "Ja" : "Nein").setHeader("Aktiv");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        filter.setPlaceholder("Suchen ...");
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> applyFilter());

        Button add = new Button("Neuer Kunde", VaadinIcon.PLUS.create(), e -> {
            grid.asSingleSelect().clear();
            edit(new CustomerDto(null, "", "", "", true));
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout list = new VerticalLayout(new HorizontalLayout(filter, add), grid);
        list.setSizeFull();
        list.setPadding(false);

        Button save = new Button("Speichern", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Abbrechen", e -> grid.asSingleSelect().clear());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(e -> delete());
        delete.setVisible(auth.hasRole("ADMIN"));

        form = new VerticalLayout(formTitle, new FormLayout(name, email, city, active),
                new HorizontalLayout(save, cancel, delete));
        form.setWidth("380px");
        form.setVisible(false);

        add(list, form);
        refresh();
    }

    private void refresh() {
        all = controller.list();
        applyFilter();
    }

    private void applyFilter() {
        String q = filter.getValue().strip().toLowerCase(Locale.ROOT);
        grid.setItems(all.stream()
                .filter(c -> q.isEmpty() || (c.name() + " " + c.email() + " " + c.city()).toLowerCase(Locale.ROOT).contains(q))
                .toList());
    }

    private void edit(CustomerDto c) {
        editing = c;
        form.setVisible(c != null);
        if (c == null) {
            return;
        }
        formTitle.setText(c.id() == null ? "Neuer Kunde" : "Kunde bearbeiten");
        name.setValue(c.name());
        email.setValue(c.email());
        city.setValue(c.city() == null ? "" : c.city());
        active.setValue(c.active());
        delete.setEnabled(c.id() != null);
        name.focus();
    }

    private void save() {
        CustomerForm input = new CustomerForm(name.getValue(), email.getValue(), city.getValue(), active.getValue());
        UiSupport.call(() -> editing.id() == null ? controller.create(input) : controller.update(editing.id(), input))
                .ifPresent(saved -> {
                    UiSupport.success("Kunde gespeichert");
                    refresh();
                    grid.asSingleSelect().clear();
                });
    }

    private void delete() {
        if (UiSupport.run(() -> controller.delete(editing.id()))) {
            UiSupport.success("Kunde gelöscht");
            refresh();
            edit(null);
        }
    }
}
