package com.example.orders.swing;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/** Schreibgeschütztes Tabellenmodell über eine Liste von DTOs. */
public final class ListTableModel<T> extends AbstractTableModel {

    public record Column<T>(String title, Class<?> type, Function<T, Object> value) {}

    private final List<Column<T>> columns;
    private List<T> rows = List.of();

    @SafeVarargs
    public ListTableModel(Column<T>... columns) {
        this.columns = List.of(columns);
    }

    public void setRows(List<T> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    public T rowAt(int modelRow) {
        return rows.get(modelRow);
    }

    public List<T> rows() {
        return rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).title();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns.get(column).type();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return columns.get(column).value().apply(rows.get(row));
    }

    /** Konfiguriert Sortierung und Darstellung (Beträge rechtsbündig, Zeitstempel formatiert). */
    public static JTable newTable(ListTableModel<?> model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setDefaultRenderer(UiSupport.Money.class, new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        table.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        table.setDefaultRenderer(Instant.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(UiSupport.dateTime((Instant) value));
            }
        });
        return table;
    }

    /** Selektiertes DTO oder null; berücksichtigt die Sortierung der View. */
    public static <T> T selected(JTable table, ListTableModel<T> model) {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : model.rowAt(table.convertRowIndexToModel(viewRow));
    }
}
