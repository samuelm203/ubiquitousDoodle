package com.example.orders.swing;

import com.example.orders.controller.PropertiesController;
import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyRowDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import swingtree.UI;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Firmenkonfiguration (nur ADMIN): Tabelle mit einer Zeile pro Schlüssel und einer Spalte pro Umgebung.
 * Filter, «nur Unterschiede» und Paging laufen im Backend; die UI hält immer nur eine Seite. Änderungen sammeln
 * sich (auch über Seiten und Filter hinweg) und gehen mit «Speichern» in einem Aufruf (alles oder nichts) ans Backend.
 */
public class PropertiesPanel extends JPanel implements Refreshable {

    static final int PAGE_SIZE = 100;

    private record Cell(String column, String key) {}

    /** Eine Seite der Tabelle samt noch nicht gespeicherter Änderungen (Wert null = Schlüssel löschen). */
    static final class PropertiesModel extends AbstractTableModel {
        private List<String> columns = List.of();
        private List<PropertyRowDto> rows = List.of();
        final Map<Cell, String> pending = new LinkedHashMap<>();

        void setPage(PropertyTableDto page) {
            columns = page.columns();
            rows = page.rows();
            fireTableStructureChanged();
        }

        void clear() {
            columns = List.of();
            rows = List.of();
            pending.clear();
            fireTableStructureChanged();
        }

        boolean isPending(int row, int col) {
            return col > 0 && pending.containsKey(cell(row, col));
        }

        private Cell cell(int row, int col) {
            return new Cell(columns.get(col - 1), rows.get(row).key());
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size() + 1;
        }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? "Schlüssel" : columns.get(col - 1);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col > 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return rows.get(row).key();
            }
            Cell c = cell(row, col);
            return pending.containsKey(c) ? pending.get(c) : rows.get(row).values().get(col - 1);
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col > 0) {
                setCell(row, col, value == null ? null : value.toString());
            }
        }

        void setCell(int row, int col, String value) {
            Cell c = cell(row, col);
            if (Objects.equals(value, rows.get(row).values().get(col - 1))) {
                pending.remove(c);
            } else {
                pending.put(c, value);
            }
            fireTableCellUpdated(row, col);
        }
    }

    private static final Color PENDING = new Color(255, 243, 190);

    private final PropertiesController controller;
    private final PropertiesModel model = new PropertiesModel();
    private final JTable table = new JTable(model);
    private final JComboBox<String> company = new JComboBox<>();
    private final JTextField filter = new JTextField();
    private final JCheckBox onlyDifferences = new JCheckBox("Nur Unterschiede");
    private final JButton prev = new JButton("‹");
    private final JButton next = new JButton("›");
    private final JLabel pageLabel = new JLabel();
    private final JButton setNull = new JButton("Löschen (null)");
    private final JButton save = new JButton("Speichern");
    private final JButton discard = new JButton("Verwerfen");
    private final Timer filterTimer;

    private String currentCompany;
    private int page;
    private int total;
    private boolean adjusting;

    public PropertiesPanel(PropertiesController controller) {
        this.controller = controller;

        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                           int row, int col) {
                boolean missing = col > 0 && value == null;
                Component c = super.getTableCellRendererComponent(t, missing ? "— fehlt —" : value, selected, focus, row, col);
                c.setFont(c.getFont().deriveFont(missing ? Font.ITALIC : Font.PLAIN));
                if (!selected) {
                    c.setForeground(missing ? Color.GRAY : t.getForeground());
                    c.setBackground(model.isPending(row, col) ? PENDING : t.getBackground());
                }
                return c;
            }
        });
        // Zähler/Buttons nach jeder Zelländerung aktualisieren
        model.addTableModelListener(e -> updateControls());

        filterTimer = new Timer(250, e -> reload(0));
        filterTimer.setRepeats(false);
        filter.putClientProperty("JTextField.placeholderText", "Schlüssel oder Wert filtern ...");

        UI.of(this)
                .withLayout("fill, insets 16", "[][grow, fill][][][][]", "[][grow, fill][]")
                .add(UI.label("Firma")).add("width 200!", UI.of(company))
                .add("growx", UI.of(filter).onTextChange(it -> filterTimer.restart()))
                .add(UI.of(onlyDifferences).onClick(it -> reload(0)))
                .add(UI.of(prev).onClick(it -> reload(page - 1)))
                .add(UI.of(pageLabel))
                .add("wrap", UI.of(next).onClick(it -> reload(page + 1)))
                .add("span, grow", UI.of(new JScrollPane(table)))
                .add("newline, span, split 3", UI.of(setNull).onClick(it -> deleteSelected()))
                .add(UI.of(discard).onClick(it -> discard()))
                .add(UI.of(save).onClick(it -> save()));

        company.addActionListener(e -> {
            if (!adjusting) {
                changeCompany((String) company.getSelectedItem());
            }
        });
        refresh();
    }

    @Override
    public void refresh() {
        List<CompanyDto> companies = UiSupport.call(controller::companies).orElse(List.of());
        adjusting = true;
        try {
            company.removeAllItems();
            companies.forEach(c -> company.addItem(c.code()));
            if (companies.stream().noneMatch(c -> c.code().equals(currentCompany))) {
                currentCompany = companies.isEmpty() ? null : companies.get(0).code();
                model.pending.clear();
            }
            company.setSelectedItem(currentCompany);
        } finally {
            adjusting = false;
        }
        reload(page);
    }

    private void changeCompany(String code) {
        if (code == null || code.equals(currentCompany)) {
            return;
        }
        if (!model.pending.isEmpty() && !UiSupport.confirm(model.pending.size() + " ungespeicherte Änderungen verwerfen?")) {
            adjusting = true;
            company.setSelectedItem(currentCompany);
            adjusting = false;
            return;
        }
        model.pending.clear();
        currentCompany = code;
        reload(0);
    }

    /** Lädt genau eine Seite vom Backend (Filter und Paging passieren dort). */
    void reload(int requestedPage) {
        filterTimer.stop();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        if (currentCompany == null) {
            model.clear();
            return;
        }
        var result = UiSupport.call(() -> controller.table(currentCompany, filter.getText(),
                onlyDifferences.isSelected(), Math.max(requestedPage, 0), PAGE_SIZE));
        if (result.isEmpty()) {
            return;
        }
        PropertyTableDto t = result.get();
        // Seite liegt hinter dem Ende (z. B. nach engerem Filter): auf die letzte vorhandene Seite springen
        if (t.rows().isEmpty() && t.total() > 0 && t.page() > 0) {
            reload((t.total() - 1) / PAGE_SIZE);
            return;
        }
        page = t.page();
        total = t.total();
        model.setPage(t);
        updateControls();
    }

    private void updateControls() {
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        pageLabel.setText("Seite " + (page + 1) + " / " + pages + "  (" + total + " Zeilen)");
        prev.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);
        int n = model.pending.size();
        save.setText(n == 0 ? "Speichern" : "Speichern (" + n + ")");
        save.setEnabled(n > 0);
        discard.setEnabled(n > 0);
    }

    /** Markiert alle selektierten Zellen als gelöscht (Wert null). */
    void deleteSelected() {
        for (int r : table.getSelectedRows()) {
            for (int c : table.getSelectedColumns()) {
                if (c > 0) {
                    model.setCell(r, c, null);
                }
            }
        }
    }

    private void discard() {
        model.pending.clear();
        model.fireTableDataChanged();
    }

    void save() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        if (model.pending.isEmpty()) {
            return;
        }
        List<PropertyChangeDto> changes = new ArrayList<>(model.pending.size());
        model.pending.forEach((c, v) -> changes.add(new PropertyChangeDto(c.column(), c.key(), v)));
        UiSupport.call(() -> controller.change(currentCompany, new PropertyChangeRequest(changes))).ifPresent(r -> {
            model.pending.clear();
            reload(page);
        });
    }

    // Für Tests
    JTable table() { return table; }
    PropertiesModel model() { return model; }
    JTextField filterField() { return filter; }
    JCheckBox onlyDifferencesBox() { return onlyDifferences; }
    JComboBox<String> companyBox() { return company; }
    JButton saveButton() { return save; }
    JButton nextButton() { return next; }
}
