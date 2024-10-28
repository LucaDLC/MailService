module mailservice.clientside { //modulo che contiene le classi per il client
    requires javafx.controls; //moduli richiesti per l'applicazione JavaFX
    requires javafx.fxml; //moduli richiesti per l'applicazione JavaFX
    requires javafx.web;

    requires org.kordamp.bootstrapfx.core; //modulo richiesto per l'interfaccia utente

    opens mailservice.clientside to javafx.fxml; //apre il package per l'utilizzo da parte di JavaFX
    exports mailservice.clientside; //esporta il package per l'utilizzo da parte di altri moduli
}