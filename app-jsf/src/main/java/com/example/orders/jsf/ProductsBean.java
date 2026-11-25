package com.example.orders.jsf;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class ProductsBean implements Serializable {

    @Inject
    transient Backend backend;

    private String filter = "";

    // Formularzustand des Dialogs; id == null bedeutet "neues Produkt"
    private String id;
    private String sku;
    private String name;
    private String price;
    private boolean active = true;

    public List<ProductDto> getVisible() {
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return backend.products.list().stream()
                .sorted(Comparator.comparing(ProductDto::name, String.CASE_INSENSITIVE_ORDER))
                .filter(p -> q.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(q)
                        || p.sku().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public void newProduct() {
        id = null;
        sku = "";
        name = "";
        price = "";
        active = true;
    }

    public void edit(ProductDto p) {
        id = p.id();
        sku = p.sku();
        name = p.name();
        price = p.price().toPlainString();
        active = p.active();
    }

    public void save() {
        BigDecimal amount;
        try {
            // Komma als Dezimaltrenner erlauben; als BigDecimal bleibt der Preis exakt (z. B. 12.50)
            amount = new BigDecimal(price.trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException e) {
            Messages.error("Ungültiger Preis: " + price);
            return;
        }
        ProductForm form = new ProductForm(sku, name, amount, active);
        boolean creating = id == null;
        Object result = Messages.attempt(
                () -> creating ? backend.products.create(form) : backend.products.update(id, form),
                creating ? "Produkt angelegt" : "Produkt gespeichert");
        if (result != null) {
            PrimeFaces.current().executeScript("PF('productDialog').hide()");
        }
    }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public String getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
