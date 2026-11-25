package com.example.orders.jsf;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.ProductDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Named
@ViewScoped
public class OrdersBean implements Serializable {

    @Inject
    transient Backend backend;

    /** Filter: leer = alle, sonst ein Status-Name. */
    private String statusFilter = "";
    private String selectedId;

    private String newCustomerId;
    private String lineProductId;
    private int lineQuantity = 1;

    public List<OrderDto> getVisible() {
        return backend.orders.list().stream()
                .filter(o -> statusFilter == null || statusFilter.isEmpty() || o.status().equals(statusFilter))
                .sorted(Comparator.comparing(OrderDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Die gewählte Bestellung, jedes Mal frisch aus dem Backend (nie veraltet). */
    public OrderDto getCurrent() {
        if (selectedId == null) {
            return null;
        }
        try {
            return backend.orders.get(selectedId);
        } catch (RuntimeException e) {
            selectedId = null;
            return null;
        }
    }

    public boolean isDraft() {
        OrderDto o = getCurrent();
        return o != null && "DRAFT".equals(o.status());
    }

    public boolean isPlaced() {
        OrderDto o = getCurrent();
        return o != null && "PLACED".equals(o.status());
    }

    public List<CustomerDto> getActiveCustomers() {
        return backend.customers.list().stream()
                .filter(CustomerDto::active)
                .sorted(Comparator.comparing(CustomerDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ProductDto> getActiveProducts() {
        return backend.products.list().stream()
                .filter(ProductDto::active)
                .sorted(Comparator.comparing(ProductDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void select(String id) {
        selectedId = id.equals(selectedId) ? null : id;
        lineProductId = null;
        lineQuantity = 1;
    }

    public void close() {
        selectedId = null;
    }

    public void createOrder() {
        if (newCustomerId == null || newCustomerId.isEmpty()) {
            Messages.error("Bitte zuerst einen Kunden wählen");
            return;
        }
        OrderDto created = Messages.attempt(
                () -> backend.orders.create(new CreateOrderRequest(newCustomerId)), "Bestellung angelegt");
        if (created != null) {
            newCustomerId = null;
            statusFilter = "";
            selectedId = created.id();
        }
    }

    public void addLine() {
        if (lineProductId == null || lineProductId.isEmpty() || lineQuantity < 1) {
            Messages.error("Produkt und Menge (≥ 1) angeben");
            return;
        }
        OrderDto updated = Messages.attempt(
                () -> backend.orders.addLine(selectedId, new AddLineRequest(lineProductId, lineQuantity)), null);
        if (updated != null) {
            lineProductId = null;
            lineQuantity = 1;
        }
    }

    public void removeLine(String productId) {
        Messages.attempt(() -> backend.orders.removeLine(selectedId, productId), null);
    }

    public void place() {
        Messages.attempt(() -> backend.orders.place(selectedId), "Bestellung aufgegeben");
    }

    public void ship() {
        Messages.attempt(() -> backend.orders.ship(selectedId), "Bestellung versendet");
    }

    public void cancel() {
        Messages.attempt(() -> backend.orders.cancel(selectedId), "Bestellung storniert");
    }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }
    public String getSelectedId() { return selectedId; }
    public String getNewCustomerId() { return newCustomerId; }
    public void setNewCustomerId(String newCustomerId) { this.newCustomerId = newCustomerId; }
    public String getLineProductId() { return lineProductId; }
    public void setLineProductId(String lineProductId) { this.lineProductId = lineProductId; }
    public int getLineQuantity() { return lineQuantity; }
    public void setLineQuantity(int lineQuantity) { this.lineQuantity = lineQuantity; }
}
