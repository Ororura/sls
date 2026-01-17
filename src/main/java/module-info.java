module com.ororura.slseleven {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;

    opens com.ororura.slseleven to javafx.fxml;
    opens com.ororura.slseleven.controller to javafx.fxml;
    opens com.ororura.slseleven.domain to javafx.base;
    
    exports com.ororura.slseleven;
    exports com.ororura.slseleven.controller;
    exports com.ororura.slseleven.domain;
    exports com.ororura.slseleven.service;
}