package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.HTMLEditor;
import javafx.util.Duration;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;
import mailservice.clientside.Model.Email;
import mailservice.clientside.Utility.Utils;

import java.util.ArrayList;
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


    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonClick() {
        String sender = ConfigManager.getInstance().readProperty("Client.Mail"); //prendo il mittente //prendo il mittente
        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        if(recipient.isEmpty() || object.isEmpty() || mailBody.isEmpty()){
            //se i campi non sono validi mostro un messaggio di errore
            showDangerAlert("Please fill all the fields");
            return;
        }

        //validazione dei destinatari separati da virgola
        String[] recipientsArray = recipient.split("\\s*,\\s*");
        if(!Arrays.stream(recipientsArray).allMatch(Utils::validateEmail)){
            //se i destinatari non sono validi mostro un messaggio di errore
            showDangerAlert("Invalid recipients. Please check the email addresses ");
            return;

        }

        //creo un oggetto email
        Email email = new Email(sender, new ArrayList<>(List.of(recipientsArray)), object, mailBody);

        //invio l'email
        send(email);

        //pulizia dei campi dell'interfaccia utente
        RecipientFieldID.clear(); //pulisco il campo destinatario
        ObjectFieldID.clear(); //pulisco il campo oggetto
        MailBodyID.setHtmlText(""); //pulisco il campo corpo dell'email

        //messaggio di conferma invio email
        showSuccessAlert("Email sent successfully");

    }

    private void send(Object email){
        ClientModel clientModel = ClientModel.getInstance(); //otteniamo l'istanza del singleton

        //se l'oggetto email è una email valisa, inviamo l'email al server
        if(email instanceof Email){
            Email emailObject = (Email) email;
            boolean success = clientModel.sendEmail(emailObject.getSender(), String.join(",", emailObject.getReceivers()), emailObject.getSubject(), emailObject.getText());

            if(success){
                System.out.println("Email sent successfully to the server");
                showSuccessAlert("Email sent successfully to the server");
            } else {
                System.out.println("Error sending email to the server");
                showDangerAlert("Error sending email to the server");
            }
        } else {
            System.out.println("The object is not an email");
            showDangerAlert("The object is not an email");
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
    protected void onSendMailButtonAction() {
        System.out.println("Sending Email...");
    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonClick() {
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

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonAction() {
        System.out.println("Deleting fields...");
    }

}