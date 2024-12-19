package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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

    MainController mainController = new MainController();

    @FXML
    protected void onSendMailButtonClick() {
        String recipient = RecipientFieldID.getText().trim();
        String subject = ObjectFieldID.getText().trim();
        String mailBody = MailBodyID.getHtmlText().trim();

        if (recipient.isEmpty() || subject.isEmpty() || mailBody.isEmpty()) {
            showDangerAlert("All fields (recipient, subject, body) must be filled.");
            return;
        }

        // Invio asincrono dell'email
        Task<Boolean> sendEmailTask = new Task<>() {
            @Override
            protected Boolean call() {
                String sender = ConfigManager.getInstance().readProperty("Client.Mail");
                List<String> recipients = Arrays.asList(recipient.split(","));
                NetworkManager networkManager = NetworkManager.getInstance();

                return networkManager.sendEmail(sender, recipients, subject, mailBody);
            }
        };

        sendEmailTask.setOnSucceeded(event -> {
            if (sendEmailTask.getValue()) {
                showSuccessAlert("Email sent successfully.");
                mainController.refreshEmails(); // Aggiorna la lista delle email
                Stage stage = (Stage) SendMailButton.getScene().getWindow();
                stage.close();
            } else {
                showDangerAlert("Failed to send email. Please try again.");
            }
        });

        sendEmailTask.setOnFailed(event -> {
            showDangerAlert("An error occurred while sending the email. Please try again.");
        });

        new Thread(sendEmailTask).start(); // Esegue il task in un thread separato
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
}