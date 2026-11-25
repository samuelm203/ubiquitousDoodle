package com.example.orders.swing;

import com.example.orders.DemoData;
import com.example.orders.PropertiesModule;
import com.example.orders.controller.PropertiesController;
import com.example.orders.persistence.files.DemoProperties;
import com.example.orders.persistence.files.FilePropertiesRepository;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import swingtree.SwingTree;
import swingtree.threading.EventProcessor;

import javax.swing.JTabbedPane;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Headless-Tests für den Konfigurations-Tab (ADMIN). */
class PropertiesPanelTest {

    @BeforeAll
    static void headless() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED);
    }

    private PropertiesController controller(Path root) {
        DemoProperties.generateIfEmpty(root);
        return new PropertiesModule(new FilePropertiesRepository(root)).controller;
    }

    @Test
    void pagesFiltersAndSavesAllOrNothing(@TempDir Path root) {
        List<String> errors = new ArrayList<>();
        UiSupport.setDialogs(new UiSupport.Dialogs() {
            public void error(String m) { errors.add(m); }
            public boolean confirm(String m) { return true; }
        });
        PropertiesController c = controller(root);
        PropertiesPanel panel = new PropertiesPanel(c);
        var model = panel.model();

        assertThat(model.getRowCount()).isEqualTo(PropertiesPanel.PAGE_SIZE);
        assertThat(model.getColumnCount()).isEqualTo(5);

        panel.nextButton().doClick();
        String keyOnPage2 = (String) model.getValueAt(0, 0);

        // Zelle ändern und eine andere löschen -> beides gesammelt, erst Speichern schreibt
        model.setValueAt("changed", 0, 1);
        model.setCell(1, 2, null);
        assertThat(model.pending).hasSize(2);
        assertThat(panel.saveButton().getText()).contains("2");
        String col = model.getColumnName(1);
        String key1 = (String) model.getValueAt(1, 0);
        assertThat(c.table(panel.companyBox().getSelectedItem().toString(), keyOnPage2, false, 0, 10)
                .rows().get(0).values()).doesNotContain("changed");

        panel.saveButton().doClick();
        assertThat(errors).isEmpty();
        assertThat(model.pending).isEmpty();
        assertThat(model.getValueAt(0, 1)).isEqualTo("changed");
        assertThat(model.getValueAt(1, 2)).isNull();
        assertThat(col).isNotBlank();
        assertThat(key1).isNotBlank();

        // Filter und nur Unterschiede
        panel.filterField().setText("zzz-gibt-es-nicht");
        panel.reload(0);
        assertThat(model.getRowCount()).isZero();
        panel.filterField().setText("");
        panel.onlyDifferencesBox().doClick();
        assertThat(model.getRowCount()).isPositive();
    }

    @Test
    void companySwitchDiscardsPending(@TempDir Path root) {
        List<String> errors = new ArrayList<>();
        UiSupport.setDialogs(new UiSupport.Dialogs() {
            public void error(String m) { errors.add(m); }
            public boolean confirm(String m) { return true; }
        });
        PropertiesPanel panel = new PropertiesPanel(controller(root));
        panel.model().setValueAt("x", 0, 1);
        panel.companyBox().setSelectedIndex(1);
        assertThat(panel.model().pending).isEmpty();
    }

    @Test
    void configTabOnlyForAdmin(@TempDir Path root) {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        var backend = InMemoryBackend.create();
        DemoData.seed(backend);
        assertThat(SwingApp.propertiesFor("USER")).isNull();
        var userTabs = (JTabbedPane) MainFrame.create(backend, null).getContentPane().getComponent(0);
        assertThat(userTabs.getTabCount()).isEqualTo(4);
        var adminTabs = (JTabbedPane) MainFrame.create(backend, controller(root)).getContentPane().getComponent(0);
        assertThat(adminTabs.getTabCount()).isEqualTo(5);
        assertThat(adminTabs.getTitleAt(4)).isEqualTo("Konfiguration");
    }
}
