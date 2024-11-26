package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.ClientApp;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;
import mailservice.clientside.Network.NetworkManager;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField LoginFieldID; //serve a inserire l'email dell'utente
    @FXML
    private Button LoginButton; //serve a effettuare il login
    @FXML
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore
    @FXML
    private TextFlow successAlert; //serve a visualizzare un messaggio di successo

    @FXML
    protected void onLoginButtonClick() {
        String login = LoginFieldID.getText()+ "@Rama.it"; //aggiungo il dominio

        NetworkManager networkManager = NetworkManager.getInstance();
        ClientModel clientModel = ClientModel.getInstance();

        if(clientModel.validateEmail(login))
        {
            if(networkManager.connectToServer()){
                showSuccessAlert("Login successful");

                //carica la scena principale
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/Main.fxml"));
                    Parent mainView = loader.load(); //carico il file FXML
                    Scene mainScene = new Scene(mainView); //creo una nuova scena
                    Stage mainStage = new Stage(); //creo una nuova finestra

                    mainStage.setScene(mainScene); //imposto la scena nella finestra
                    mainStage.setTitle("ClientSide - Main");
                    mainStage.initModality(Modality.APPLICATION_MODAL); //consente di interagire con entrambe le finestre
                    mainStage.show();

                    //chiudo la finestra di login
                    Stage stage = (Stage) LoginButton.getScene().getWindow();
                    stage.close();
                }catch(IOException e){
                    System.err.println("Errore nel caricamento del file FXML: " + e.getMessage());
                    showDangerAlert("Unable to load Main.fxml");
                }
            }else{
                showDangerAlert("Unable to connect to the server");
            }
        }else{
            showDangerAlert("Invalid email");
        }
    }

    @FXML
    protected void onLoginButtonAction() {
        System.out.println("Attempting Login...");
    }

    @FXML
    private void showDangerAlert(String message) {
        successAlert.setVisible(false); //nascondo il messaggio di successo
        getDangerAlert();
        Text dangerText = new Text(message);
        dangerText.setFill(Color.RED);
        dangerAlert.getChildren().add(dangerText);
        dangerAlert.setVisible(true);
        hideAlerts();
        //aggiungo il messaggio di errore al campo dangerAlert
    }

    @FXML
    private void showSuccessAlert(String message) {
        dangerAlert.setVisible(false); //nascondo il messaggio di errore
        getSuccessAlert();
        Text successText = new Text(message);
        successText.setFill(Color.GREEN);
        successAlert.getChildren().add(successText);
        successAlert.setVisible(true);
        hideAlerts();
        //aggiungo il messaggio di successo al campo successAlert
    }

    @FXML
    private void hideAlerts() {
        // Nascondere gli alert dopo 3 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
            successAlert.setVisible(false);
        });
        pause.play();
    }

    public TextFlow getSuccessAlert() {
        successAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di successo nel caso in cui ci sia già un messaggio
        return successAlert;
    }

    public TextFlow getDangerAlert() {
        dangerAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di errore nel caso in cui ci sia già un messaggio
        return dangerAlert;
    }
}
