package com.example.orders;

import com.example.orders.controller.PropertiesController;
import com.example.orders.repository.PropertiesRepository;
import com.example.orders.service.PropertiesService;

/**
 * Verdrahtung der Firmenkonfiguration von Hand, getrennt von {@link Backend}, damit bestehende Apps
 * (Vaadin, JavaFX, Swing ...) unverändert bleiben. Jedes Framework gibt nur ein Repository mit.
 */
public final class PropertiesModule {

    public final PropertiesService service;
    public final PropertiesController controller;

    public PropertiesModule(PropertiesRepository repository) {
        this.service = new PropertiesService(repository);
        this.controller = new PropertiesController(service);
    }
}
