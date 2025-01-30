package mailservice.serverside.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Cursor;
import mailservice.serverside.Log.LogType;
import mailservice.serverside.Model.ServerModel;

import java.io.IOException;


public class ServerController {
    @FXML
    private ListView<String> ServerLog; //serve a visualizzare il log del server
    @FXML
    private Button startButton; //serve a avviare il server
    @FXML
    private Button stopButton; //serve a fermare il server
    @FXML
    private ImageView folderCreation;

    private ServerModel serverModel;
    private boolean isServerRunning = false; //stato per monitorare se il server è avviato


    @FXML
    protected void initialize() {
        // Inizializza i pulsanti e l'interfaccia
        startButton.setCursor(Cursor.HAND);
        stopButton.setCursor(Cursor.HAND);
        folderCreation.setCursor(Cursor.HAND); // Funziona dopo aver aggiunto l'import
        startButton.setDisable(false);
        stopButton.setDisable(true);
        if (ServerLog != null) {
            ServerLog.getItems().clear(); // Pulisce il log
        }
    }


    @FXML
    protected void startServer() {
        if (isServerRunning) {  // controllo se il server è già avviato
            log(LogType.INFO,"Server is already running.");
            return;
        }

        serverModel = ServerModel.getInstance(this); //passiamo il ServerController al ServerModel
        serverModel.startServer();
        isServerRunning = true;

        //aggiorna lo stato dei bottoni
        startButton.setDisable(true);
        stopButton.setDisable(false);

        //aggiungiamo un messaggio di log per segnalare che il server è stato avviato
        log(LogType.INFO,"Server started on port " + serverModel.getPort());
    }


    @FXML
    protected void onStart() {
        ServerLog.getItems().clear();
        startServer();
    }


    @FXML
    protected void stopServer() {
        if(serverModel != null && isServerRunning) {
            serverModel.stopServer();
            isServerRunning = false; //aggiorna lo stato del server

            //aggiorna lo stato dei bottoni
            startButton.setDisable(false);
            stopButton.setDisable(true);


            //aggiungiamo un messaggio di log per segnalare che il server è stato fermato
            log(LogType.INFO, "Server stopped");
        }
        else {
            log(LogType.ERROR,"Server is not running");
        }
    }


    @FXML
    public void onStop() {
        ServerLog.getItems().clear();
        stopServer();
    }


    @FXML
    protected void onServerCreateFolder() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/serverside/Folder.fxml"));
        Parent mainView = loader.load(); //carico il file FXML
        Scene mainScene = new Scene(mainView); //creo una nuova scena
        Stage mainStage = new Stage(); //creo una nuova finestra

        mainStage.setScene(mainScene); //imposto la scena nella finestra
        mainStage.setTitle("Serverside - Folder Management");
        mainStage.initModality(Modality.APPLICATION_MODAL); //consente di interagire con entrambe le finestre
        mainStage.show();
    }


    public void log(LogType type, String message) {
        Platform.runLater(() -> {
            String formattedMessage = String.format("[%s] %s", type.name(), message);
            ServerLog.getItems().add(formattedMessage);
            ServerLog.scrollTo(ServerLog.getItems().size() - 1);
            while (ServerLog.getItems().size() > 50) {
                ServerLog.getItems().remove(0); // Rimuove la prima voce (la più vecchia)
            }
        });
    }


    protected void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
