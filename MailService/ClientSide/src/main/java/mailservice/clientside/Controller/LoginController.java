package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField LoginFieldID; //serve a inserire l'email dell'utente
    @FXML
    private Button LoginButton; //serve ad effettuare il login
    @FXML
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore
    @FXML
    private TextFlow successAlert; //serve a visualizzare un messaggio di successo


    @FXML
    protected void onLoginButtonClick() {
        String login = LoginFieldID.getText()+ "@rama.it"; //aggiungo il dominio
        System.out.println("Email: " + login);
        ConfigManager configManager = ConfigManager.getInstance();

        if(configManager.validateEmail(login))
        {
            System.out.println("[INFO] Email is valid");
            if(ClientModel.getInstance().wrapLoginCheck(login)){
                System.out.println("[INFO]The Email is a server user");
                showSuccessAlert();
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/Main.fxml"));
                    Parent mainView = loader.load(); //carico il file FXML
                    Scene mainScene = new Scene(mainView); //creo una nuova scena
                    Stage mainStage = new Stage(); //creo una nuova finestra

                    mainStage.setScene(mainScene); //imposto la scena nella finestra
                    mainStage.setTitle("ClientSide - Main");
                    mainStage.setResizable(false);
                    mainStage.initModality(Modality.APPLICATION_MODAL); //consente di interagire con entrambe le finestre
                    mainStage.show();

                    //chiudo la finestra di login
                    Stage stage = (Stage) LoginButton.getScene().getWindow();
                    stage.setResizable(false);
                    stage.close();
                } catch (IOException e) {
                    System.err.println("Unable to load Main.fxml: " + e.getMessage());
                    showDangerAlert("Unable to load Main.fxml");
                }
            } else {
                System.err.println("[ERROR] The Email is not a server user");
                showDangerAlert("Server is offline or user not registered yet");
            }

        } else {
            System.err.println("[ERROR] Email is not Valid");
            showDangerAlert("Invalid Email");
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
    }


    @FXML
    private void showSuccessAlert() {
        dangerAlert.setVisible(false); //nascondo il messaggio di errore
        getSuccessAlert();
        Text successText = new Text("Login successful");
        successText.setFill(Color.GREEN);
        successAlert.getChildren().add(successText);
        successAlert.setVisible(true);
        hideAlerts();
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


    private void getSuccessAlert() {
        successAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di successo nel caso in cui ci sia già un messaggio
    }


    private void getDangerAlert() {
        dangerAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di errore nel caso in cui ci sia già un messaggio
    }
}
