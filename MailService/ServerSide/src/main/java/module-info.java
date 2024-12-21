module mailservice.serverside {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires Shared;

    opens mailservice.serverside.Controller to javafx.fxml;
    exports mailservice.serverside;
}