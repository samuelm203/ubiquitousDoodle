package com.example.orders.swing;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.ProductController;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.OrderLineDto;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.swing.ListTableModel.Column;
import swingtree.UI;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.util.function.Function;

/** Bestellungen anlegen, Positionen pflegen und den Status weiterschalten (Controller: OrderController). */
public class OrdersPanel extends JPanel implements Refreshable {

    private final OrderController orders;
    private final CustomerController customers;
    private final ProductController products;

    private final ListTableModel<OrderDto> orderModel = new ListTableModel<>(
            new Column<OrderDto>("Nr.", String.class, OrderDto::number),
            new Column<OrderDto>("Kunde", String.class, OrderDto::customerName),
            new Column<OrderDto>("Status", String.class, o -> UiSupport.statusLabel(o.status())),
            new Column<OrderDto>("Erfasst", java.time.Instant.class, OrderDto::createdAt),
            new Column<OrderDto>("Total", UiSupport.Money.class, o -> new UiSupport.Money(o.total(), o.currency())));
    private final JTable orderTable = ListTableModel.newTable(orderModel);
    private final JComboBox<CustomerDto> customer = new JComboBox<>();

    private final JLabel detailTitle = new JLabel();
    private final JLabel detailInfo = new JLabel();
    private final ListTableModel<OrderLineDto> lineModel = new ListTableModel<>(
            new Column<OrderLineDto>("Produkt", String.class, OrderLineDto::productName),
            new Column<OrderLineDto>("Menge", Integer.class, OrderLineDto::quantity),
            new Column<OrderLineDto>("Preis", UiSupport.Money.class, l -> new UiSupport.Money(l.unitPrice(), currency())),
            new Column<OrderLineDto>("Total", UiSupport.Money.class, l -> new UiSupport.Money(l.lineTotal(), currency())));
    private final JTable lineTable = ListTableModel.newTable(lineModel);
    private final JComboBox<ProductDto> product = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100_000, 1));
    private final JButton removeLine = new JButton("Position entfernen");
    private final JButton addLine = new JButton("Hinzufügen");
    private final JButton place = new JButton("Aufgeben");
    private final JButton ship = new JButton("Versenden");
    private final JButton cancel = new JButton("Stornieren");
    private final JPanel addLineBar;
    private final JPanel detail;

    private OrderDto current;
    private boolean adjusting;

    public OrdersPanel(OrderController orders, CustomerController customers, ProductController products) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;

        customer.setRenderer(OrdersPanel.<CustomerDto>renderer(CustomerDto::name));
        product.setRenderer(OrdersPanel.<ProductDto>renderer(p -> p.name() + " (" + UiSupport.money(p.price(), p.currency()) + ")"));
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !adjusting) {
                show(ListTableModel.selected(orderTable, orderModel));
            }
        });
        lineTable.getSelectionModel().addListSelectionListener(e ->
                removeLine.setEnabled(current != null && "DRAFT".equals(current.status())
                        && lineTable.getSelectedRow() >= 0));

        addLineBar = UI.panel("insets 0, fillx", "[grow, fill][][]")
                .add(UI.of(product))
                .add(UI.of(quantity).withPrefSize(80, 28))
                .add(UI.of(addLine).onClick(it -> addLine()))
                .get(JPanel.class);

        detail = UI.panel("insets 0 12 0 0, wrap 1, fillx", "[grow, fill]", "[][][grow, fill][][]")
                .add(UI.of(detailTitle).withFontSize(16))
                .add(UI.of(detailInfo))
                .add("grow", UI.of(new JScrollPane(lineTable)))
                .add(UI.of(addLineBar))
                .add("split 4", UI.of(removeLine).onClick(it -> removeSelectedLine()))
                .add(UI.of(place).onClick(it -> update(orders::place)))
                .add(UI.of(ship).onClick(it -> update(orders::ship)))
                .add(UI.of(cancel).onClick(it -> update(orders::cancel)))
                .get(JPanel.class);
        detail.setVisible(false);

        UI.of(this)
                .withLayout("fill, insets 16", "[grow, fill][520!]", "[][grow, fill]")
                .add("split 2", UI.of(customer).withPrefSize(280, 28))
                .add("wrap", UI.button("Neue Bestellung").onClick(it -> createOrder()))
                .add("grow, spany 2", UI.of(detail))
                .add("grow, newline", UI.of(new JScrollPane(orderTable)));
        refresh();
    }

    @Override
    public void refresh() {
        // Auswahllisten neu laden: Kunden/Produkte können sich in anderen Tabs geändert haben
        customer.removeAllItems();
        customers.list().stream().filter(CustomerDto::active).forEach(customer::addItem);
        customer.setSelectedIndex(-1);
        product.removeAllItems();
        products.list().stream().filter(ProductDto::active).forEach(product::addItem);
        product.setSelectedIndex(-1);
        reload(current == null ? null : current.id());
    }

    /** Liste neu laden und, falls vorhanden, die Bestellung mit dieser ID wieder auswählen. */
    private void reload(String selectId) {
        adjusting = true;
        try {
            orderModel.setRows(orders.list());
            orderTable.clearSelection();
            OrderDto match = null;
            for (int i = 0; i < orderModel.getRowCount(); i++) {
                if (orderModel.rowAt(i).id().equals(selectId)) {
                    match = orderModel.rowAt(i);
                    int view = orderTable.convertRowIndexToView(i);
                    orderTable.getSelectionModel().setSelectionInterval(view, view);
                }
            }
            show(match);
        } finally {
            adjusting = false;
        }
    }

    void createOrder() {
        CustomerDto selected = (CustomerDto) customer.getSelectedItem();
        if (selected == null) {
            UiSupport.error("Bitte zuerst einen Kunden wählen");
            return;
        }
        UiSupport.call(() -> orders.create(new CreateOrderRequest(selected.id())))
                .ifPresent(o -> reload(o.id()));
    }

    void addLine() {
        ProductDto selected = (ProductDto) product.getSelectedItem();
        if (current == null) {
            return;
        }
        if (selected == null) {
            UiSupport.error("Produkt und Menge angeben");
            return;
        }
        int qty = ((Number) quantity.getValue()).intValue();
        update(id -> orders.addLine(id, new AddLineRequest(selected.id(), qty)));
        product.setSelectedIndex(-1);
        quantity.setValue(1);
    }

    void removeSelectedLine() {
        int row = lineTable.getSelectedRow();
        if (current == null || row < 0) {
            return;
        }
        OrderLineDto line = lineModel.rowAt(lineTable.convertRowIndexToModel(row));
        update(id -> orders.removeLine(id, line.productId()));
    }

    /** Aktion auf der aktuellen Bestellung ausführen, danach Liste und Detail aktualisieren. */
    void update(Function<String, OrderDto> action) {
        if (current == null) {
            return;
        }
        String id = current.id();
        UiSupport.call(() -> action.apply(id)).ifPresent(updated -> reload(updated.id()));
    }

    private String currency() {
        return current == null ? "CHF" : current.currency();
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
        lineModel.setRows(o.lines());
        removeLine.setVisible(draft);
        removeLine.setEnabled(false);
        addLineBar.setVisible(draft);
        place.setVisible(draft);
        ship.setVisible("PLACED".equals(o.status()));
        cancel.setVisible(draft || "PLACED".equals(o.status()));
    }

    private static <E> DefaultListCellRenderer renderer(Function<E, String> label) {
        return new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value == null ? "" : label.apply((E) value));
                return this;
            }
        };
    }

    // Für Tests
    JComboBox<CustomerDto> customerCombo() { return customer; }
    JComboBox<ProductDto> productCombo() { return product; }
    JSpinner quantitySpinner() { return quantity; }
    JTable orderTable() { return orderTable; }
    ListTableModel<OrderDto> orderModel() { return orderModel; }
    ListTableModel<OrderLineDto> lineModel() { return lineModel; }
    JTable lineTable() { return lineTable; }
    JButton placeButton() { return place; }
    JButton shipButton() { return ship; }
    JButton cancelButton() { return cancel; }
    JButton addLineButton() { return addLine; }
    JButton removeLineButton() { return removeLine; }
    JPanel detailPanel() { return detail; }
}
