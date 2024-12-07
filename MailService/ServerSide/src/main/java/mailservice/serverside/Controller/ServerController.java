package mailservice.serverside.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import mailservice.serverside.Model.ServerModel;

public class ServerController {
    @FXML
    private ListView<String> ServerLog; //serve a visualizzare il log del server
    @FXML
    private Button startButton; //serve a avviare il server
    @FXML
    private Button stopButton; //serve a fermare il server

    private ServerModel serverModel;
    private boolean isServerRunning = false; //stato per monitorare se il server è avviato

    @FXML
    public void initialize() {
        // Inizializza i pulsanti e l'interfaccia
        startButton.setDisable(false);
        stopButton.setDisable(true);

        if (ServerLog != null) {
            ServerLog.getItems().clear(); // Pulisce il log
        }
    }

    //metodo per avviare il server
    @FXML
    public void startServer() {
        if (isServerRunning) {  // controllo se il server è già avviato
            log("Server is already running.");
            return;
        }

        serverModel = new ServerModel(this); //passiamo il ServerController al ServerModel
        serverModel.startServer();
        isServerRunning = true;

        //aggiorna lo stato dei bottoni
        startButton.setDisable(true);
        stopButton.setDisable(false);

        //aggiungiamo un messaggio di log per segnalare che il server è stato avviato
        log("Server started on port " + serverModel.getPort());
    }
    @FXML
    public void onStart() {
        startServer();
    }

    //metodo per fermare il server
    @FXML
    public void stopServer() {
        if(serverModel != null && isServerRunning) {
            serverModel.stopServer();
            isServerRunning = false; //aggiorna lo stato del server

            //aggiorna lo stato dei bottoni
            startButton.setDisable(false);
            stopButton.setDisable(true);

            //aggiungiamo un messaggio di log per segnalare che il server è stato fermato
            log("Server stopped");
        }
        else {
            log("Server is not running");
        }
    }
    @FXML
    public void onStop() {
        stopServer();
    }

    //metodo per aggiungere un messaggio al log del server
    public void log(String message) {
        ServerLog.getItems().add(message);
    }

    //metodo per mostrare un alert in caso di errore
    public void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
