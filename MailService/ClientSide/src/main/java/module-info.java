module mailservice.clientside {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens mailservice.clientside to javafx.fxml;
    exports mailservice.clientside;
}