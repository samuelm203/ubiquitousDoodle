package com.example.orders.javafx;

import com.example.orders.controller.PropertiesController;
import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyRowDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Firmenkonfiguration (nur ADMIN): Zeilen = Schlüssel, Spalten = Umgebungen, {@code null} = fehlt.
 * Filter, "nur Unterschiede" und Paging laufen im Backend; die Tabelle zeigt nur die aktuelle Seite
 * (TableView virtualisiert die Zeilen). Änderungen werden über Seiten und Filter hinweg gesammelt
 * und mit einem einzigen Aufruf gespeichert (alles oder nichts).
 */
class PropertiesTab extends Tab implements Refreshable {

    private static final String CHANGED = "-fx-background-color: #fff3c4;";

    private record Cell(String key, String column) {}

    private final PropertiesController controller;
    /** Offene Änderungen; Wert {@code null} bedeutet "Schlüssel löschen". */
    private final Map<Cell, String> pending = new HashMap<>();

    private final ChoiceBox<CompanyDto> companies = new ChoiceBox<>();
    private final TextField filter = Ui.field("Schlüssel oder Wert filtern ...");
    private final CheckBox onlyDifferences = new CheckBox("Nur Unterschiede");
    private final ChoiceBox<Integer> pageSize = new ChoiceBox<>(FXCollections.observableArrayList(50, 100, 500, 2000));
    private final Button prev = new Button("<");
    private final Button next = new Button(">");
    private final Label pageInfo = new Label();
    private final TableView<PropertyRowDto> table = new TableView<>();
    private final Button save = new Button("Speichern");
    private final Button discard = new Button("Verwerfen");
    private final Button remove = new Button("Wert löschen");
    private final PauseTransition filterDelay = new PauseTransition(Duration.millis(300));

    private List<String> shownColumns = List.of();
    private String shownCompany;
    private int page;
    private boolean updating;

    PropertiesTab(PropertiesController controller) {
        super("Konfiguration");
        this.controller = controller;
        setClosable(false);

        companies.setConverter(new StringConverter<>() {
            @Override
            public String toString(CompanyDto c) {
                return c == null ? "" : c.code() + " (" + c.keyCount() + ")";
            }

            @Override
            public CompanyDto fromString(String s) {
                return null;
            }
        });
        pageSize.setValue(100);

        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setPlaceholder(new Label("Keine Zeilen"));

        companies.valueProperty().addListener((obs, old, c) -> onCompanyChosen(old));
        filterDelay.setOnFinished(e -> reload(0));
        filter.textProperty().addListener((obs, old, t) -> filterDelay.playFromStart());
        onlyDifferences.selectedProperty().addListener((obs, old, v) -> reload(0));
        pageSize.valueProperty().addListener((obs, old, v) -> reload(0));
        prev.setOnAction(e -> reload(page - 1));
        next.setOnAction(e -> reload(page + 1));
        save.setOnAction(e -> save());
        discard.setOnAction(e -> {
            pending.clear();
            reload(page);
        });
        remove.setOnAction(e -> deleteSelected());
        updatePendingButtons();

        HBox top = new HBox(8, new Label("Firma"), companies, filter, onlyDifferences);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);
        Label hint = new Label("Zelle doppelklicken zum Bearbeiten, \"fehlt\" = Schlüssel nicht vorhanden.");
        hint.setStyle("-fx-text-fill: #666;");
        HBox bottom = new HBox(8, prev, pageInfo, next, new Label("Zeilen/Seite"), pageSize, hint);
        bottom.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(8, remove, discard, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(8, top, table, bottom, actions);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setPadding(new Insets(12));
        setContent(root);
    }

    @Override
    public void refresh() {
        Ui.call(controller::companies).ifPresent(list -> {
            CompanyDto selected = companies.getValue();
            updating = true;
            companies.getItems().setAll(list);
            CompanyDto keep = selected == null ? null
                    : list.stream().filter(c -> c.code().equals(selected.code())).findFirst().orElse(null);
            companies.setValue(keep != null ? keep : list.isEmpty() ? null : list.get(0));
            updating = false;
            CompanyDto now = companies.getValue();
            if (now == null || !now.code().equals(shownCompany)) {
                reload(0);
            } else if (pending.isEmpty()) {
                reload(page);
            }
        });
    }

    private void onCompanyChosen(CompanyDto old) {
        if (updating) {
            return;
        }
        if (!pending.isEmpty() && !Ui.confirm("Nicht gespeicherte Änderungen verwerfen?")) {
            updating = true;
            companies.setValue(old);
            updating = false;
            return;
        }
        pending.clear();
        reload(0);
    }

    private void reload(int requestedPage) {
        CompanyDto company = companies.getValue();
        if (updating || company == null) {
            return;
        }
        PropertyTableDto t = Ui.call(() -> controller.table(company.code(), filter.getText(),
                onlyDifferences.isSelected(), Math.max(requestedPage, 0), pageSize.getValue())).orElse(null);
        if (t == null) {
            return;
        }
        if (!company.code().equals(shownCompany) || !t.columns().equals(shownColumns)) {
            buildColumns(t.columns());
            shownCompany = company.code();
        }
        page = t.page();
        int pages = Math.max(1, (t.total() + t.size() - 1) / t.size());
        // Ein einziges setAll statt Zeile für Zeile: nur ein Change-Event für bis zu 2000 Zeilen
        table.getItems().setAll(t.rows());
        pageInfo.setText("Seite " + (page + 1) + " von " + pages + " (" + t.total() + " Zeilen)");
        prev.setDisable(page <= 0);
        next.setDisable(page + 1 >= pages);
        updatePendingButtons();
    }

    private void buildColumns(List<String> columns) {
        shownColumns = columns;
        List<TableColumn<PropertyRowDto, ?>> cols = new ArrayList<>();
        TableColumn<PropertyRowDto, String> key = Ui.column("Schlüssel", 300, PropertyRowDto::key);
        key.setSortable(false);
        cols.add(key);
        for (int i = 0; i < columns.size(); i++) {
            cols.add(valueColumn(columns.get(i), i));
        }
        table.getColumns().setAll(cols);
    }

    private TableColumn<PropertyRowDto, String> valueColumn(String name, int index) {
        TableColumn<PropertyRowDto, String> col = new TableColumn<>(name);
        col.setPrefWidth(200);
        col.setSortable(false);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(effective(c.getValue(), name, index)));
        col.setCellFactory(c -> new ValueCell(name));
        col.setOnEditCommit((CellEditEvent<PropertyRowDto, String> e) -> {
            PropertyRowDto row = e.getRowValue();
            record(new Cell(row.key(), name), row.values().get(index), e.getNewValue());
        });
        return col;
    }

    private String effective(PropertyRowDto row, String column, int index) {
        Cell cell = new Cell(row.key(), column);
        return pending.containsKey(cell) ? pending.get(cell) : row.values().get(index);
    }

    /** Merkt die Änderung oder verwirft sie, wenn der Wert wieder dem gespeicherten entspricht. */
    private void record(Cell cell, String original, String value) {
        if (Objects.equals(original, value)) {
            pending.remove(cell);
        } else {
            pending.put(cell, value);
        }
        table.refresh();
        updatePendingButtons();
    }

    private void deleteSelected() {
        for (var pos : new ArrayList<>(table.getSelectionModel().getSelectedCells())) {
            if (pos.getColumn() < 1 || pos.getRow() < 0) {
                continue; // Schlüsselspalte nicht löschbar
            }
            PropertyRowDto row = table.getItems().get(pos.getRow());
            int index = pos.getColumn() - 1;
            Cell cell = new Cell(row.key(), shownColumns.get(index));
            if (row.values().get(index) != null || pending.containsKey(cell)) {
                record(cell, row.values().get(index), null);
            }
        }
    }

    private void save() {
        CompanyDto company = companies.getValue();
        List<PropertyChangeDto> changes = pending.entrySet().stream()
                .map(e -> new PropertyChangeDto(e.getKey().column(), e.getKey().key(), e.getValue()))
                .toList();
        if (company == null || changes.isEmpty()) {
            return;
        }
        if (Ui.call(() -> controller.change(company.code(), new PropertyChangeRequest(changes))).isPresent()) {
            pending.clear();
            refresh(); // aktualisiert auch die Schlüsselzahl in der Firmenliste
        }
    }

    private void updatePendingButtons() {
        save.setText(pending.isEmpty() ? "Speichern" : "Speichern (" + pending.size() + ")");
        save.setDisable(pending.isEmpty());
        discard.setDisable(pending.isEmpty());
    }

    /** Textzelle: fehlende Werte kursiv grau, geänderte Werte gelb hinterlegt. */
    private final class ValueCell extends TextFieldTableCell<PropertyRowDto, String> {
        private final String column;

        ValueCell(String column) {
            super(new StringConverter<>() {
                @Override
                public String toString(String s) {
                    return s == null ? "" : s;
                }

                @Override
                public String fromString(String s) {
                    return s;
                }
            });
            this.column = column;
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setStyle("");
            if (empty) {
                return;
            }
            PropertyRowDto row = getTableRow() == null ? null : getTableRow().getItem();
            boolean changed = row != null && pending.containsKey(new Cell(row.key(), column));
            if (item == null && !isEditing()) {
                setText("fehlt");
                setStyle("-fx-text-fill: #999; -fx-font-style: italic;" + (changed ? CHANGED : ""));
            } else if (changed) {
                setStyle(CHANGED);
            }
        }
    }
}
