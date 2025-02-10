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
import static mailservice.shared.enums.LogType.*;

import java.io.IOException;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField LoginFieldID; //serve a inserire l'email dell'utente
    @FXML
    private Button LoginButton; //serve ad effettuare il login
    @FXML
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore


    @FXML
    protected void onLoginButtonClick() {
        String login = LoginFieldID.getText()+ "@rama.it"; //aggiungo il dominio
        ClientModel.log(INFO,"Email: " + login);
        ConfigManager configManager = ConfigManager.getInstance();

        if (configManager.validateEmail(login)) {
            ClientModel.log(INFO,"Email is valid");
            Map.Entry<Boolean, Boolean> loginResult = ClientModel.getInstance().wrapLoginCheck(login);
            if (loginResult.getKey()) {
                if (loginResult.getValue()) {
                    ClientModel.log(INFO,"The Email is a server user");
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
                        ClientModel.log(ERROR,"Unable to load Main.fxml: " + e.getMessage());
                        showDangerAlert("Unable to load Main.fxml");
                    }
                } else {
                    ClientModel.log(ERROR,"The Email is not a server user");
                    showDangerAlert("The Email user is not registered yet");
                }
            } else {
                ClientModel.log(ERROR,"Server is unreachable");
                showDangerAlert("Server is unreachable");
            }

        } else {
            ClientModel.log(ERROR,"Email is not Valid");
            showDangerAlert("Invalid Email");
        }
    }


    @FXML
    protected void onLoginButtonAction() {
        ClientModel.log(INFO, "Attempting Login...");
    }


    @FXML
    private void showDangerAlert(String message) {
        dangerAlert.getChildren().clear();
        Text dangerText = new Text(message);
        dangerText.setFill(Color.RED);
        dangerAlert.getChildren().add(dangerText);
        dangerAlert.setVisible(true);
        hideAlerts();
    }



    @FXML
    private void hideAlerts() {
        // Nascondere gli alert dopo 3 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
        });
        pause.play();
    }


}
