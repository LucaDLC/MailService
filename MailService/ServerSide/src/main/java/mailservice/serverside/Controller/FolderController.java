package mailservice.serverside.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import mailservice.serverside.Model.ServerModel;
import mailservice.shared.Email;

import static mailservice.shared.enums.CommandResponse.*;


public class FolderController {

    @FXML
    private TextField FolderFieldID; //serve a inserire l'email dell'utente
    @FXML
    private Button FolderButton; //serve ad effettuare la creazione della cartella
    @FXML
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore
    @FXML
    private TextFlow successAlert; //serve a visualizzare un messaggio di successo
    @FXML
    private Button DeleteFolderButton; //serve a eliminare la cartella
    @FXML
    private ListView<String> folderListId; //serve a visualizzare la lista delle email presenti nella cartella

    private ServerModel serverModel;

    @FXML
    protected void initialize() {
        serverModel = ServerModel.getInstance();
        folderListId.getItems().clear();
        folderListId.setItems(serverModel.getFolderList());

        // Set up il cell factory per visualizzare i soggetti delle email
        folderListId.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String folderName, boolean empty) {
                super.updateItem(folderName, empty);
                setText(empty || folderName == null ? null : folderName);
            }
        });

        folderListId.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                FolderFieldID.setText(newSelection.replace("@rama.it", ""));
            }
        });

        folderListId.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) { // Se la ListView perde il focus
                folderListId.getSelectionModel().clearSelection();
            }
        });


    }


    @FXML
    protected void onFolderButtonClick() {
        String folder = FolderFieldID.getText()+ "@rama.it"; //aggiungo il dominio
        if(serverModel.FolderManagement(folder,true).equals(SUCCESS)){
            showSuccessAlert("Folder created successfully");
        } else if(serverModel.FolderManagement(folder,true).equals(ILLEGAL_PARAMS)){
            showDangerAlert("Folder name is not valid");
        }
        else {
            showDangerAlert("Folder already exists");
        }
    }


    @FXML
    protected void onDeleteFolderButtonClick() {
        String folder = FolderFieldID.getText()+ "@rama.it"; //aggiungo il dominio
        if(serverModel.FolderManagement(folder,false).equals(SUCCESS)){
            folderListId.getItems().remove(folder);
            folderListId.getSelectionModel().clearSelection();
            FolderFieldID.clear();
            showSuccessAlert("Folder deleted successfully");
        } else if(serverModel.FolderManagement(folder,false).equals(ILLEGAL_PARAMS)){
            showDangerAlert("Folder name is not valid");
        }
        else {
            showDangerAlert("Folder does not exist");
        }
    }


    @FXML
    private void showDangerAlert(String message) {
        successAlert.setVisible(false); //nascondo il messaggio di successo
        getDangerAlert();
        Text dangerText = new Text(message);
        dangerText.setFill(Color.RED);
        dangerAlert.getChildren().add(dangerText);
        dangerAlert.setVisible(true);
        hideAlerts();
    }


    @FXML
    private void showSuccessAlert(String message) {
        dangerAlert.setVisible(false); //nascondo il messaggio di errore
        getSuccessAlert();
        Text successText = new Text(message);
        successText.setFill(Color.GREEN);
        successAlert.getChildren().add(successText);
        successAlert.setVisible(true);
        hideAlerts();
    }


    @FXML
    private void hideAlerts() {
        // Nascondere gli alert dopo 3 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
            successAlert.setVisible(false);
        });
        pause.play();
    }


    private void getSuccessAlert() {
        successAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di successo nel caso in cui ci sia già un messaggio
    }


    private void getDangerAlert() {
        dangerAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di errore nel caso in cui ci sia già un messaggio
    }

}
