module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires java.sql;
    requires com.fazecast.jSerialComm;

    opens org.sncf.gui to javafx.fxml;
    exports org.sncf.gui;
}