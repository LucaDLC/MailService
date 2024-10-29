package mailservice.clientside.Controller;

import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;    //importo la classe WebView, che visualizza contenuti web
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
    @FXML
    private Button ComposeButton;

    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load();
            Scene composeScene = new Scene(composeView);
            Stage composeStage = new Stage();

            composeStage.setScene(composeScene);
            composeStage.setTitle("Compose your Email");
            composeStage.initModality(Modality.APPLICATION_MODAL); //serve per permettere all'utente di interagire con questa finestra prima di tornare alla finestra principale
            composeStage.show();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    //handler per l'azione del bottone Compose
    protected void onComposeButtonAction() {
        System.out.println("Composing a new Email...");
        onComposeButtonClick();
    }
    @FXML
    protected void onDeleteButtonClick() {
        int selectedIndex =MailList.getSelectionModel().getSelectedIndex(); //
        if(selectedIndex>=0){//se >=0 vuol dire che la mail è stata selezionata
            MailList.getItems().remove(selectedIndex);
            System.out.println("Email deleted from the list");
        }else{
            System.out.println("No Email selected to delete");
        }
    }
    //handler per l'azione del bottone Delete
    @FXML
    protected void onDeleteButtonAction() {
        System.out.println("Delete button action triggered.");
        onDeleteButtonClick();
    }

}
