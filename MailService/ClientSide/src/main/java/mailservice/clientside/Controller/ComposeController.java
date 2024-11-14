package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.HTMLEditor;
import javafx.util.Duration;
import mailservice.clientside.Model.ClientModel;

public class ComposeController{
    //collegamento con la GUI tramite l'annotazione @FXML
    @FXML
    private TextField SenderFieldID; //serve a visualizzare il mittente dell'email
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
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore
    @FXML
    private TextFlow successAlert; //serve a visualizzare un messaggio di successo

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonClick() {
        String sender = SenderFieldID.getText(); //prendo il mittente
        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        if(sender.isEmpty() || recipient.isEmpty()||object.isEmpty()||mailBody.isEmpty()){
            //se uno dei campi è vuoto mostro un messaggio di errore
            showDangerAlert("Please fill all the fields");
        } else {
            ClientModel clientModel = ClientModel.getInstance();
            boolean success = clientModel.sendEmail(sender, recipient, object, mailBody); //invio l'email
            if(success){
                //se l'email è stata inviata con successo mostro un messaggio di successo
                showSuccessAlert("Email sent successfully");
            } else {
                //altrimenti mostro un messaggio di errore
                showDangerAlert("Error sending email");
            }
        }
    }

    private void showDangerAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);  // Tipo di alert per errore
        alert.setTitle("Errore");
        alert.setHeaderText(null);  // Nessun testo di intestazione
        alert.setContentText(message);  // Messaggio di errore
        alert.showAndWait();  // Mostra l'alert e aspetta che l'utente lo chiuda
        //aggiungo il messaggio di errore al campo dangerAlert
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // Tipo di alert per successo
        alert.setTitle("Successo");
        alert.setHeaderText(null);  // Nessun testo di intestazione
        alert.setContentText(message);  // Messaggio di successo
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
        String sender = SenderFieldID.getText(); //prendo il mittente
        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        if(sender.isEmpty() && recipient.isEmpty() && object.isEmpty() && mailBody.equals("<html dir=\"ltr\"><head></head><body contenteditable=\"true\"></body></html>")){
            //se tutti i campi sono vuoti mostro un messaggio di errore
            showDangerAlert("Fields are already empty");
        } else {
            //altrimenti pulisco i campi e mostro un messaggio di successo
            SenderFieldID.clear(); //pulisco il campo mittente
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

    public TextFlow getSuccessAlert() {
        successAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di successo nel caso in cui ci sia già un messaggio
        return successAlert;
    }

    public TextFlow getDangerAlert() {
        dangerAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di errore nel caso in cui ci sia già un messaggio
        return dangerAlert;
    }
}