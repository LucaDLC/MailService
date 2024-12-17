package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;//importo la classe WebView, che visualizza contenuti web
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;
import mailservice.clientside.Network.NetworkManager;

import java.util.Timer;
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
    private TextFlow dangerAlert; //serve a visualizzare un messaggio di errore
    @FXML
    private TextFlow successAlert; //serve a visualizzare un messaggio di successo
    @FXML
    public Button ComposeButton;
    @FXML
    public Button ForwardButton;
    @FXML
    public Button DeleteButton;
    @FXML
    public MenuButton Reply;

    @FXML
    public void initialize() {
        ConfigManager configManager = ConfigManager.getInstance();
        MailLabel.setText(configManager.readProperty("Client.Mail"));
        System.out.println("[DEBUG] MainController initialized.");
        System.out.println("[DEBUG] MailList reference: " + MailList);

        if (MailList != null) {
            MailList.getItems().add("No emails found.");
        } else {
            System.err.println("[ERROR] MailList è null.");
        }
    }

    void refreshEmails() {
        Platform.runLater(() -> {
            if (MailList == null) {
                System.err.println("[ERROR] MailList è ancora null. Ritenterò...");
                return; // Esci se MailList non è pronto
            }

            int retryCount = 3; // Numero massimo di tentativi
            while (retryCount > 0) {
                String[] emails = ClientModel.getInstance().fetchEmails();
                if (emails == null || emails.length == 0) {
                    System.err.println("[WARNING] Nessuna email trovata o connessione fallita. Tentativi rimasti: " + retryCount);
                    retryCount--;
                } else {
                    updateEmailList(emails);
                    System.out.println("[DEBUG] Email list aggiornata con successo.");
                    return; // Esci se ha avuto successo
                }
            }
            System.err.println("[ERROR] Impossibile aggiornare la lista delle email.");
            MailList.getItems().add("No emails found.");
        });
    }

    //metodo per aggiornare la ListView con le email ricevute
    public void updateEmailList(String[] emails) {
        Platform.runLater(() -> {
            if (MailList == null) {
                System.err.println("[ERROR] MailList non è inizializzata.");
                return;
            }

            MailList.getItems().clear();
            if (emails != null && emails.length > 0) {
                for (String email : emails) {
                    // Escludi risposte non valide come "SUCCESS|..."
                    if (email != null && !email.trim().isEmpty() && !email.startsWith("SUCCESS|")) {
                        MailList.getItems().add(email);
                    }
                }
            }

            if (MailList.getItems().isEmpty()) {
                MailList.getItems().add("No emails found.");
            }
        });
    }

    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load();
            Scene composeScene = new Scene(composeView);
            Stage composeStage = new Stage();
            composeStage.setScene(composeScene);
            composeStage.setTitle("ClientSide - Mail Compose");
            composeStage.initModality(Modality.APPLICATION_MODAL);
            composeStage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if (!selectedMails.isEmpty()) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Confirmation");
            confirmDialog.setHeaderText(null);
            confirmDialog.setContentText("Are you sure you want to delete the selected emails?");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    String username = ConfigManager.getInstance().readProperty("Client.Mail").split("@")[0];
                    String emailData = username + "|" + String.join(",", selectedMails);

                    NetworkManager networkManager = NetworkManager.getInstance();
                    if (networkManager.sendMessage(CommandRequest.DELETE_EMAIL, emailData)) {
                        String serverResponse = networkManager.receiveMessage();
                        if ("SUCCESS".equals(serverResponse)) {
                            refreshEmails();
                            MailList.getItems().removeAll(selectedMails);
                            showSuccessAlert("Emails deleted successfully.");
                        } else {
                            showDangerAlert("Failed to delete emails.");
                        }
                    } else {
                        showDangerAlert("Failed to connect to the server.");
                    }
                }
            });
        } else {
            showDangerAlert("No email selected for deletion.");
        }
    }

    @FXML
    protected void onForwardButtonClick() {
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Forward");
        }else{
            System.out.println("No email selected for forward");
        }
    }

    @FXML
    protected void onReplyButtonClick() {
        System.out.println("Replying Email...");
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Reply");
        }
    }

    @FXML
    protected void onReplyAllButtonAction() {
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Reply All");
        }else{
            System.out.println("No email selected for reply all ");
        }
    }
    private void showComposeWindow(String action) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load();
            Scene composeScene = new Scene(composeView);
            Stage composeStage = new Stage();
            composeStage.setScene(composeScene);
            composeStage.setTitle("ClientSide - Mail Compose");
            composeStage.initModality(Modality.APPLICATION_MODAL);
            composeStage.show();
        }catch(IOException e){
            System.err.println("Error loadind FXML file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void showDangerAlert(String message) {
        if (dangerAlert != null) {
            dangerAlert.getChildren().clear();
            Text dangerText = new Text(message);
            dangerText.setFill(Color.RED);
            dangerAlert.getChildren().add(dangerText);
            dangerAlert.setVisible(true);

            hideAlerts(); // Nascondi gli alert dopo 3 secondi
        }
    }

    @FXML
    private void showSuccessAlert(String message) {
        if(successAlert != null){
            successAlert.getChildren().clear();
            Text successText = new Text(message);
            successText.setFill(Color.GREEN);
            successAlert.getChildren().add(successText);
            successAlert.setVisible(true);

            hideAlerts(); // Nascondi gli alert dopo 3 secondi
        }
    }

    @FXML
    private void hideAlerts() {
        // Nascondere gli alert dopo 3 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
        });
        pause.play();
    }
}
