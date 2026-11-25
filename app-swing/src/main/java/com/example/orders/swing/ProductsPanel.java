package com.example.orders.swing;

import com.example.orders.controller.ProductController;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import com.example.orders.swing.ListTableModel.Column;
import swingtree.UI;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.math.BigDecimal;

/** Produkte anlegen und bearbeiten (kein Löschen, wie in der Vaadin-View). */
public class ProductsPanel extends JPanel implements Refreshable {

    private final ProductController controller;
    private final ListTableModel<ProductDto> model = new ListTableModel<>(
            new Column<ProductDto>("SKU", String.class, ProductDto::sku),
            new Column<ProductDto>("Name", String.class, ProductDto::name),
            new Column<ProductDto>("Preis", UiSupport.Money.class, p -> new UiSupport.Money(p.price(), p.currency())),
            new Column<ProductDto>("Lieferbar", String.class, p -> p.active() ? "Ja" : "Nein"));
    private final JTable table = ListTableModel.newTable(model);

    private final JLabel formTitle = new JLabel();
    private final JTextField sku = new JTextField();
    private final JTextField name = new JTextField();
    private final JTextField price = new JTextField();
    private final JCheckBox active = new JCheckBox("Lieferbar");
    private final JPanel form;

    private ProductDto editing;
    private boolean adjusting;

    public ProductsPanel(ProductController controller) {
        this.controller = controller;

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !adjusting) {
                edit(ListTableModel.selected(table, model));
            }
        });

        form = UI.panel("insets 0 12 0 0, wrap 2", "[][grow, fill]")
                .add("span 2", UI.of(formTitle).withFontSize(16))
                .add(UI.label("SKU")).add("growx", UI.of(sku))
                .add(UI.label("Name")).add("growx", UI.of(name))
                .add(UI.label("Preis (CHF)")).add("growx", UI.of(price))
                .add("skip 1", UI.of(active))
                .add("span 2, split 2", UI.button("Speichern").onClick(it -> save()))
                .add(UI.button("Abbrechen").onClick(it -> table.clearSelection()))
                .get(JPanel.class);
        form.setVisible(false);

        UI.of(this)
                .withLayout("fill, insets 16", "[grow, fill][320!]", "[][grow, fill]")
                .add("wrap", UI.button("Neues Produkt").onClick(it -> {
                    table.clearSelection();
                    edit(new ProductDto(null, "", "", BigDecimal.ZERO, "CHF", true));
                }))
                .add("grow, spany 2", UI.of(form))
                .add("grow, newline", UI.of(new JScrollPane(table)));
        refresh();
    }

    @Override
    public void refresh() {
        adjusting = true;
        try {
            model.setRows(controller.list());
        } finally {
            adjusting = false;
        }
        if (editing == null || editing.id() != null) {
            edit(ListTableModel.selected(table, model));
        }
    }

    void edit(ProductDto p) {
        editing = p;
        form.setVisible(p != null);
        if (p == null) {
            return;
        }
        formTitle.setText(p.id() == null ? "Neues Produkt" : "Produkt bearbeiten");
        sku.setText(p.sku());
        sku.setEditable(p.id() == null);
        name.setText(p.name());
        price.setText(p.price().toPlainString());
        active.setSelected(p.active());
    }

    void save() {
        if (editing == null) {
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(price.getText().strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            UiSupport.error("Ungültiger Preis: " + price.getText());
            return;
        }
        ProductForm input = new ProductForm(sku.getText(), name.getText(), amount, active.isSelected());
        UiSupport.call(() -> editing.id() == null ? controller.create(input) : controller.update(editing.id(), input))
                .ifPresent(saved -> {
                    editing = null;
                    table.clearSelection();
                    refresh();
                    edit(null);
                });
    }

    // Für Tests
    JTextField skuField() { return sku; }
    JTextField nameField() { return name; }
    JTextField priceField() { return price; }
    ListTableModel<ProductDto> model() { return model; }
}
