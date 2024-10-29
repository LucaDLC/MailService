package mailservice.clientside.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.TextFlow;
import javafx.scene.web.HTMLEditor;

public class ComposeController{
    //collegamento con la GUI tramite l'annotazione @FXML
    @FXML
    private Label SenderFieldID; //serve a visualizzare il mittente dell'email
    @FXML
    private Label RecipientFieldID; //serve a visualizzare il destinatario dell'email
    @FXML
    private Label ObjectFieldID; //serve a visualizzare l'oggetto dell'email
    @FXML
    private Button SendMailButton; //serve a visualizzare il bottone per inviare l'email
    @FXML
    private Button CancelFieldButton; //serve a visualizzare il bottone per cancellare i campi della email
    @FXML
    private HTMLEditor MailBodyID; //serve a visualizzare e scrivere il corpo dell'email
    @FXML
    private TextFlow DangerAlert;
    @FXML
    private TextFlow SuccessAlert;

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonClick() {

    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onSendMailButtonAction() {

    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonClick() {

    }

    @FXML
    //metodo che viene chiamato quando si preme il bottone
    protected void onCancelFieldButtonAction() {

    }

    public TextFlow getSuccessAlert() {
        return null;
    }

    public TextFlow getDangerAlert() {
        return null;
    }
}