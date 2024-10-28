package mailservice.clientside.Controller;

import javafx.fxml.FXML; //importo la classe FXML
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.web.WebView;    //importo la classe WebView, che visualizza contenuti web

public class MainController {
    //collegamento con la GUI tramite l'annotazione @FXML
    @FXML   // questa annotazione indica che la variabile o il metodo è associato a un file .fxml
    private Label fromLbl;  //serve a visualizzare il mittente dell'email
    @FXML
    private Label subjectLabel; //serve a visualizzare l'oggetto dell'email
    @FXML
    private Label toLbl; //serve a visualizzare il destinatario dell'email
    @FXML
    private Label subjectLbl; //serve a visualizzare l'oggetto dell'email
    @FXML
    private Label DateLbl; //serve a visualizzare la data dell'email
    @FXML
    private WebView emailContentTxt; //serve a visualizzare il contenuto dell'email

    //implementazione delle azioni da eseguire quando si preme il bottone
    @FXML
    //metodo che viene chiamato quando si preme il bottone
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

}
