package com.example.orders.app.ui;

import com.example.orders.controller.PropertiesController;
import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyRowDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Firmenkonfiguration als Tabelle (Zeile = Schlüssel, Spalte = Datei). Nur ADMIN.
 * Geladen wird immer nur eine Seite (serverseitiges Filtern/Paging im Service); Änderungen werden
 * über Seiten und Filter hinweg gesammelt und mit einem einzigen Aufruf (alles oder nichts) gespeichert.
 */
@Route("properties")
@PageTitle("Konfiguration | Orders")
@RolesAllowed("ADMIN")
public class PropertiesView extends VerticalLayout {

    private static final String CHANGED_BG = "var(--lumo-primary-color-10pct)";

    private final PropertiesController controller;
    private final ComboBox<CompanyDto> company = new ComboBox<>("Firma");
    private final TextField filter = new TextField();
    private final Checkbox onlyDifferences = new Checkbox("Nur Unterschiede");
    private final Select<Integer> pageSize = new Select<>();
    private final Grid<PropertyRowDto> grid = new Grid<>();
    private final Button prev = new Button(VaadinIcon.ANGLE_LEFT.create());
    private final Button next = new Button(VaadinIcon.ANGLE_RIGHT.create());
    private final Span pageInfo = new Span();
    private final Button save = new Button("Speichern");
    private final Button discard = new Button("Verwerfen");

    /** Ausstehende Änderungen: Spalte + Schlüssel -> Änderung (value null = löschen). */
    private final Map<String, PropertyChangeDto> pending = new LinkedHashMap<>();
    private List<String> columns = List.of();
    private int page;
    private int total;
    private CompanyDto current;

    public PropertiesView(PropertiesController controller) {
        this.controller = controller;
        setSizeFull();

        company.setItems(controller.companies());
        company.setItemLabelGenerator(c -> c.code() + " (" + c.keyCount() + ")");
        company.setAllowCustomValue(false);
        company.addValueChangeListener(e -> {
            if (e.getValue() == null || e.getValue().equals(current)) {
                return;
            }
            if (!pending.isEmpty()) {
                UiSupport.error("Ungespeicherte Änderungen: bitte zuerst speichern oder verwerfen");
                company.setValue(current); // zurück zur bisherigen Auswahl
                return;
            }
            current = e.getValue();
            page = 0;
            load();
        });

        filter.setPlaceholder("Schlüssel filtern ...");
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> firstPage());
        onlyDifferences.addValueChangeListener(e -> firstPage());

        pageSize.setLabel("Zeilen");
        pageSize.setItems(50, 100, 200, 500);
        pageSize.setValue(100);
        pageSize.setWidth("100px");
        pageSize.addValueChangeListener(e -> firstPage());

        prev.addClickListener(e -> {
            page--;
            load();
        });
        next.addClickListener(e -> {
            page++;
            load();
        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save());
        discard.addClickListener(e -> {
            pending.clear();
            load();
        });

        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addColumn(PropertyRowDto::key).setHeader("Schlüssel").setFlexGrow(2).setResizable(true);

        HorizontalLayout bar = new HorizontalLayout(company, filter, onlyDifferences, pageSize);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        HorizontalLayout footer = new HorizontalLayout(prev, pageInfo, next, save, discard);
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(bar, grid, footer);
        updateButtons();
    }

    private void firstPage() {
        page = 0;
        load();
    }

    private void load() {
        if (current == null) {
            return;
        }
        var result = UiSupport.call(() -> controller.table(current.code(), filter.getValue(),
                onlyDifferences.getValue(), page, pageSize.getValue()));
        if (result.isEmpty()) {
            return;
        }
        PropertyTableDto t = result.get();
        page = t.page();
        total = t.total();
        if (!t.columns().equals(columns)) {
            columns = t.columns();
            while (grid.getColumns().size() > 1) {
                grid.removeColumn(grid.getColumns().get(1));
            }
            for (int i = 0; i < columns.size(); i++) {
                int idx = i;
                grid.addComponentColumn(row -> cell(row, idx)).setHeader(columns.get(i))
                        .setFlexGrow(1).setResizable(true).setWidth("14em");
            }
        }
        grid.setItems(t.rows());
        updateButtons();
    }

    private TextField cell(PropertyRowDto row, int idx) {
        String column = columns.get(idx);
        String key = row.key();
        String id = column + '\u0000' + key;
        String original = row.values().get(idx);
        PropertyChangeDto change = pending.get(id);
        String shown = change == null ? original : change.value();

        TextField field = new TextField();
        field.setWidthFull();
        field.addThemeName("small");
        field.setPlaceholder("— fehlt —");
        field.setValue(shown == null ? "" : shown);
        field.setSuffixComponent(deleteButton(id, column, key, original, field));
        mark(field, change != null);
        field.addValueChangeListener(e -> {
            if (!e.isFromClient()) {
                return;
            }
            String value = e.getValue();
            if (value.equals(original == null ? "" : original)) {
                pending.remove(id); // unverändert (fehlender Schlüssel bleibt fehlend)
            } else {
                pending.put(id, new PropertyChangeDto(column, key, value));
            }
            mark(field, pending.containsKey(id));
            updateButtons();
        });
        return field;
    }

    private Button deleteButton(String id, String column, String key, String original, TextField field) {
        Button b = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> {
            if (original == null) {
                pending.remove(id);
            } else {
                pending.put(id, new PropertyChangeDto(column, key, null));
            }
            field.setValue("");
            mark(field, pending.containsKey(id));
            updateButtons();
        });
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        b.setTooltipText("Schlüssel in dieser Datei löschen");
        return b;
    }

    private static void mark(TextField field, boolean changed) {
        field.getStyle().set("background", changed ? CHANGED_BG : "");
    }

    private void save() {
        var req = new PropertyChangeRequest(List.copyOf(pending.values()));
        UiSupport.call(() -> controller.change(current.code(), req)).ifPresent(r -> {
            pending.clear();
            UiSupport.success(r.applied() + " Änderung(en) gespeichert");
            load();
        });
    }

    private void updateButtons() {
        int size = pageSize.getValue() == null ? 100 : pageSize.getValue();
        int pages = Math.max(1, (total + size - 1) / size);
        prev.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);
        pageInfo.setText("Seite " + (page + 1) + " / " + pages + " (" + total + " Zeilen)");
        save.setText("Speichern (" + pending.size() + ")");
        save.setEnabled(!pending.isEmpty());
        discard.setEnabled(!pending.isEmpty());
    }
}
