module com.ororura.slseleven {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.sql;
    requires java.desktop;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    opens com.ororura.slseleven to javafx.fxml;
    opens com.ororura.slseleven.adapters.controller to javafx.fxml;
    opens com.ororura.slseleven.domain.model to javafx.base;

    exports com.ororura.slseleven;
    exports com.ororura.slseleven.adapters.controller;
    exports com.ororura.slseleven.domain.model;
    exports com.ororura.slseleven.domain.repository;
    exports com.ororura.slseleven.application.usecase;
}
