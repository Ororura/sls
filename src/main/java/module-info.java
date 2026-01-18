module com.ororura.slseleven {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.sql;

    opens com.ororura.slseleven to javafx.fxml;
    opens com.ororura.slseleven.controller to javafx.fxml;
    opens com.ororura.slseleven.domain.model to javafx.base;
    
    exports com.ororura.slseleven;
    exports com.ororura.slseleven.controller;
    exports com.ororura.slseleven.domain.model;
    exports com.ororura.slseleven.domain.repository;
    exports com.ororura.slseleven.usecase;
}