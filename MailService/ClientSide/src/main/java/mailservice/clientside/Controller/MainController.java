package mailservice.clientside.Controller;

import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.control.ListView;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;    //importo la classe WebView, che visualizza contenuti web
import javafx.stage.Stage;
import mailservice.clientside.ClientApp;

public class MainController{
    //collegamento con la GUI tramite l'annotazione @FXML
    @FXML   // questa annotazione indica che la variabile o il metodo è associato a un file .fxml
    private Label SenderLabel;  //serve a visualizzare il mittente dell'email
    @FXML
    private Label ReceiverLabel; //serve a visualizzare il destinatario dell'email
    @FXML
    private Label ObjectLabel; //serve a visualizzare l'oggetto dell'email
    @FXML
    private Label DateLabel; //serve a visualizzare la data dell'email
    @FXML
    private WebView MailContent; //serve a visualizzare il contenuto dell'email
    @FXML
    private ListView MailList; //serve a visualizzare la lista delle email
    @FXML
    private Label MailLabel; //serve a visualizzare la mail email
    @FXML
    private TextFlow DangerAlert;
    @FXML
    private TextFlow SuccessAlert;


    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {

    }
    @FXML
    protected void onComposeButtonAction() {

    }
    @FXML
    protected void onDeleteButtonClick() {

    }
    @FXML
    protected void onDeleteButtonAction() {

    }
    @FXML
    protected void onForwardButtonClick() {

    }
    @FXML
    protected void onForwardButtonAction() {

    }
    @FXML
    protected void onReplyButtonClick() {

    }
    @FXML
    protected void onReplyButtonAction() {

    }
    @FXML
    protected void onReplyAllButtonAction() {

    }

    public TextFlow getSuccessAlert() {
        return null;
    }

    public TextFlow getDangerAlert() {
        return null;
    }
}
