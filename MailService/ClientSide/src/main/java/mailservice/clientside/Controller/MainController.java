package mailservice.clientside.Controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.control.ListView;
import javafx.scene.web.WebView;//importo la classe WebView, che visualizza contenuti web
import javafx.stage.Modality;
import javafx.stage.Stage;
import mailservice.clientside.ClientApp;
import java.io.IOException;

public class MainController {
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
    private ListView<String> MailList; //serve a visualizzare la lista delle email
    @FXML
    private Label MailLabel; //serve a visualizzare la mail email


    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load(); //carico il file FXML
            Scene composeScene = new Scene(composeView); //creo una nuova scena
            Stage composeStage = new Stage(); //creo una nuova finestra

            composeStage.setScene(composeScene); //imposto la scena nella finestra
            composeStage.setTitle("ClientSide - Mail Compose");
            composeStage.initModality(Modality.APPLICATION_MODAL); //consente di interagire con entrambe le finestre
            composeStage.show();

        }catch (IOException e) {
            System.err.println("Errore nel caricamento del file FXML: " + e.getMessage());
            e.printStackTrace();
        }

    }
    @FXML
    //handler per l'azione del bottone Compose
    protected void onComposeButtonAction() {
        System.out.println("Composing a new Email...");
    }

    @FXML
    protected void onDeleteButtonClick() {
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        //An ObservableList is a special type of list that allows listeners to track changes to the list, such as additions, removals, or updates to its elements
        if(!selectedMails.isEmpty()) {
            MailList.getItems().removeAll(selectedMails);
            System.out.println("Emails deleted successfully");
        }
        else{
            System.out.println("No email selected");
        }
    }
    @FXML
    //handler per l'azione del bottone Delete
    protected void onDeleteButtonAction() {
        System.out.println("Deleting Email...");
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
