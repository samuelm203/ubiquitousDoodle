package com.example.orders.swing;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.example.orders.swing.ListTableModel.Column;
import swingtree.UI;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.util.List;
import java.util.Locale;

/** Kunden suchen, anlegen, bearbeiten und löschen; leitet alles an den CustomerController weiter. */
public class CustomersPanel extends JPanel implements Refreshable {

    private final CustomerController controller;
    private final ListTableModel<CustomerDto> model = new ListTableModel<>(
            new Column<CustomerDto>("Name", String.class, CustomerDto::name),
            new Column<CustomerDto>("E-Mail", String.class, CustomerDto::email),
            new Column<CustomerDto>("Ort", String.class, CustomerDto::city),
            new Column<CustomerDto>("Aktiv", String.class, c -> c.active() ? "Ja" : "Nein"));
    private final JTable table = ListTableModel.newTable(model);
    private final JTextField filter = new JTextField();

    private final JLabel formTitle = new JLabel();
    private final JTextField name = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField city = new JTextField();
    private final JCheckBox active = new JCheckBox("Aktiv");
    private final JButton delete = new JButton("Löschen");
    private final JPanel form;

    private List<CustomerDto> all = List.of();
    private CustomerDto editing;
    private boolean adjusting;

    public CustomersPanel(CustomerController controller) {
        this.controller = controller;

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !adjusting) {
                edit(ListTableModel.selected(table, model));
            }
        });

        form = UI.panel("insets 0 12 0 0, wrap 2", "[][grow, fill]")
                .add("span 2", UI.of(formTitle).withFontSize(16))
                .add(UI.label("Name")).add("growx", UI.of(name))
                .add(UI.label("E-Mail")).add("growx", UI.of(email))
                .add(UI.label("Ort")).add("growx", UI.of(city))
                .add("skip 1", UI.of(active))
                .add("span 2, split 3", UI.button("Speichern").onClick(it -> save()))
                .add(UI.button("Abbrechen").onClick(it -> table.clearSelection()))
                .add(UI.of(delete).onClick(it -> delete()))
                .get(JPanel.class);
        form.setVisible(false);

        UI.of(this)
                .withLayout("fill, insets 16", "[grow, fill][320!]", "[][grow, fill]")
                .add("growx, split 2", UI.of(filter).onTextChange(it -> applyFilter()))
                .add("wrap", UI.button("Neuer Kunde").onClick(it -> {
                    table.clearSelection();
                    edit(new CustomerDto(null, "", "", "", true));
                }))
                .add("grow, spany 2", UI.of(form))
                .add("grow, newline", UI.of(new JScrollPane(table)));
        filter.putClientProperty("JTextField.placeholderText", "Suchen ...");
        refresh();
    }

    @Override
    public void refresh() {
        all = controller.list();
        applyFilter();
    }

    private void applyFilter() {
        String q = filter.getText().strip().toLowerCase(Locale.ROOT);
        adjusting = true;
        try {
            model.setRows(all.stream()
                    .filter(c -> q.isEmpty()
                            || (c.name() + " " + c.email() + " " + c.city()).toLowerCase(Locale.ROOT).contains(q))
                    .toList());
        } finally {
            adjusting = false;
        }
        // Selektion ist nach dem Neuladen weg: Formular nur schliessen, wenn es keinen neuen Kunden zeigt
        if (editing == null || editing.id() != null) {
            edit(ListTableModel.selected(table, model));
        }
    }

    void edit(CustomerDto c) {
        editing = c;
        form.setVisible(c != null);
        if (c == null) {
            return;
        }
        formTitle.setText(c.id() == null ? "Neuer Kunde" : "Kunde bearbeiten");
        name.setText(c.name());
        email.setText(c.email());
        city.setText(c.city() == null ? "" : c.city());
        active.setSelected(c.active());
        delete.setEnabled(c.id() != null);
        name.requestFocusInWindow();
    }

    void save() {
        if (editing == null) {
            return;
        }
        CustomerForm input = new CustomerForm(name.getText(), email.getText(), city.getText(), active.isSelected());
        UiSupport.call(() -> editing.id() == null ? controller.create(input) : controller.update(editing.id(), input))
                .ifPresent(saved -> {
                    editing = null;
                    table.clearSelection();
                    refresh();
                    edit(null);
                });
    }

    void delete() {
        if (editing == null || editing.id() == null || !UiSupport.confirm("Kunde «" + editing.name() + "» löschen?")) {
            return;
        }
        if (UiSupport.run(() -> controller.delete(editing.id()))) {
            editing = null;
            table.clearSelection();
            refresh();
            edit(null);
        }
    }

    // Für Tests
    JTextField nameField() { return name; }
    JTextField emailField() { return email; }
    JTextField cityField() { return city; }
    JTable table() { return table; }
    ListTableModel<CustomerDto> model() { return model; }
}
