package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Network.NetworkManager;

import java.util.Arrays;
import java.util.List;

public class ComposeController{
    //collegamento con la GUI tramite l'annotazione @FXML

    @FXML
    private TextField RecipientFieldID; //serve a visualizzare il destinatario dell'email
    @FXML
    private TextField ObjectFieldID; //serve a visualizzare l'oggetto dell'email
    @FXML
    private Button SendMailButton; //serve a visualizzare il bottone per inviare l'email
    @FXML
    private Button CancelFieldButton; //serve a visualizzare il bottone per cancellare i campi della email
    @FXML
    private HTMLEditor MailBodyID; //serve a visualizzare e scrivere il corpo dell'email

    private Runnable updateCallback;

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonClick() {
        System.out.println("[DEBUG] Sending Email...");

        String sender = ConfigManager.getInstance().readProperty("Client.Mail"); //prendo il mittente
        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        List<String> recipients = Arrays.asList(recipient.split(","));
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        System.out.println("[DEBUG] Email data: Sender=" + sender + ", Recipients=" + recipients + ", Subject=" + object);

        NetworkManager networkManager = NetworkManager.getInstance();
        boolean sent = networkManager.sendEmail(sender, recipients, object, mailBody);

        if (sent) {
            System.out.println("[INFO] Email sent successfully.");
            Stage stage = (Stage) SendMailButton.getScene().getWindow();
            stage.close();;

            // Chiama il callback per aggiornare la lista delle email
            if (updateCallback != null) {
                updateCallback.run();
            }

            showSuccessAlert("Email sent successfully.");
        } else {
            System.out.println("[ERROR] Failed to send email.");
            showDangerAlert("Failed to send email. Please check your connection or try again.");
        }
    }

    private void showDangerAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);  // Tipo di alert per errore
        alert.setTitle("Error");
        alert.setHeaderText(null);  // Nessun testo di intestazione
        alert.setContentText(message);  // Messaggio di errore

        //crea una transizione per chiudere l'alert dopo 2 secondi
        PauseTransition pause= new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> alert.close());
        pause.play();

        alert.showAndWait();  // Mostra l'alert e aspetta che l'utente lo chiuda
        //aggiungo il messaggio di errore al campo dangerAlert
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // Tipo di alert per successo
        alert.setTitle("Success");
        alert.setHeaderText(null);  // Nessun testo di intestazione
        alert.setContentText(message);  // Messaggio di successo

        //crea una transizione per chiudere l'alert dopo 2 secondi
        PauseTransition pause= new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> alert.close());
        pause.play();

        alert.showAndWait();  // Mostra l'alert e aspetta che l'utente lo chiuda
        //aggiungo il messaggio di successo al campo successAlert
    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonClick() {
        System.out.println("Deleting fields...");

        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        if(recipient.isEmpty() && object.isEmpty() && mailBody.trim().isEmpty()){
            //se tutti i campi sono vuoti mostro un messaggio di errore
            showDangerAlert("Fields are already empty");
        } else {
            //altrimenti pulisco i campi e mostro un messaggio di successo
            RecipientFieldID.clear(); //pulisco il campo destinatario
            ObjectFieldID.clear(); //pulisco il campo oggetto
            MailBodyID.setHtmlText(""); //pulisco il campo corpo dell'email
            showSuccessAlert("Fields cleared successfully");
        }
    }

    public void setUpdateCallback(Runnable callBack) {
        this.updateCallback = callBack;
    }
}