package com.example.orders.jsf;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Rolle der aktuellen Sitzung. Die JSF-App hat (noch) keine Anmeldung; die Rolle wird deshalb in der
 * Navigation umgeschaltet. Das ist nur ein Demo-Ersatz: für echten Schutz müsste Quarkus Security davor.
 */
@Named
@SessionScoped
public class RoleBean implements Serializable {

    private boolean admin;

    public void toggle() {
        admin = !admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getRole() {
        return admin ? "ADMIN" : "USER";
    }
}
