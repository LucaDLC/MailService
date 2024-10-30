package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.HTMLEditor;
import javafx.util.Duration;

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
            //altrimenti mostro un messaggio di successo
            showSuccessAlert("Email sent successfully");
        }
    }

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

    private void hideAlerts() {
        // Nascondere gli alert dopo 3 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
            successAlert.setVisible(false);
        });
        pause.play();
    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonAction() {
        System.out.println("Sending Email...");
        onSendMailButtonClick();
    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonClick() {
        String sender = SenderFieldID.getText(); //prendo il mittente
        String recipient = RecipientFieldID.getText(); //prendo il destinatario
        String object = ObjectFieldID.getText(); //prendo l'oggetto
        String mailBody = MailBodyID.getHtmlText(); //prendo il corpo dell'email

        if(sender.isEmpty() && recipient.isEmpty()&&object.isEmpty()&&mailBody.isEmpty()){
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
        onCancelFieldButtonClick();
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