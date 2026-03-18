module com.photo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.swing;

    exports com.photo;
    exports com.photo.controller;
    exports com.photo.model;
    exports com.photo.utils;

    opens com.photo.controller to javafx.fxml;
    opens com.photo to javafx.fxml;
}