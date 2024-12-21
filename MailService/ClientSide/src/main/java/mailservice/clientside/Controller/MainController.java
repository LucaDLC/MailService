package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.shared.Email;
import mailservice.clientside.Model.ClientModel;

import java.util.Collections;
import java.io.IOException;
import java.util.stream.Collectors;

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
    private ListView<Email> MailList; //serve a visualizzare la lista delle email
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

    private ClientModel clientModel;

    @FXML
    public void initialize() {
        System.out.println("[INFO] Initializing MainController...");
        clientModel = ClientModel.getInstance();

        if (clientModel == null) {
            System.err.println("[ERROR] Failed to initialize ClientModel.");
            showDangerAlert("Initialization error.");
            ComposeButton.setDisable(true);
            ForwardButton.setDisable(true);
            DeleteButton.setDisable(true);
            Platform.exit();
            return;
        }

        System.out.println("[INFO] ClientModel initialized successfully.");
        MailLabel.setText(clientModel.getUserEmail());
        refreshEmails();

        // Set up the cell factory to display only the email subject
        MailList.setCellFactory(lv -> new ListCell<Email>() {
            @Override
            protected void updateItem(Email email, boolean empty) {
                super.updateItem(email, empty);
                setText(empty || email == null ? null : email.getSubject());
            }
        });

        MailList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayEmailDetails(newSelection);
            }
        });
    }

    public void refreshEmails() {
        if (clientModel == null) {
            System.err.println("[ERROR] ClientModel is not initialized. Cannot fetch emails.");
            showDangerAlert("Unable to refresh emails: ClientModel is not initialized.");
            return;
        }

        Task<ObservableList<Email>> fetchEmailsTask = new Task<>() {
            @Override
            protected ObservableList<Email> call() {
                return FXCollections.observableArrayList(clientModel.fetchEmails());
            }
        };

        fetchEmailsTask.setOnSucceeded(event -> {
            ObservableList<Email> emails = fetchEmailsTask.getValue();
            if (emails.isEmpty()) {
                System.out.println("[INFO] No emails found.");
            }
            MailList.setItems(emails);
        });

        fetchEmailsTask.setOnFailed(event -> {
            System.err.println("[ERROR] Failed to fetch emails: " + fetchEmailsTask.getException().getMessage());
            showDangerAlert("Failed to fetch emails.");
        });

        new Thread(fetchEmailsTask).start();
    }

    private void displayEmailDetails(Email email) {
        if (email != null) {
            SenderLabel.setText(email.getSender());
            ReceiverLabel.setText(String.join(", ", email.getReceivers()));
            ObjectLabel.setText(email.getSubject());
            DateLabel.setText(email.getDate().toString());
            MailContent.getEngine().loadContent(email.getText());
        }
    }

    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {
        System.out.println("[INFO] Composing Mail.");
        Platform.runLater(() -> {
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
                System.err.println("[ERROR] Failed to load MailCompose.fxml: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    protected void onDeleteButtonClick() {
        ObservableList<Email> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if (!selectedMails.isEmpty()) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Confirmation");
            confirmDialog.setHeaderText(null);
            confirmDialog.setContentText("Are you sure you want to delete the selected emails?");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    String username = ConfigManager.getInstance().readProperty("Client.Mail").split("@")[0];
                    String emailData = username + "|" + selectedMails.stream()
                            .flatMap(email -> email.getReceivers().stream())
                            .collect(Collectors.joining(","));

                    ClientModel clientModel = ClientModel.getInstance();
                    if (clientModel.wrapDeleteEmail(null)) {   //da rivedere
                        refreshEmails();
                        MailList.getItems().removeAll(selectedMails);
                        showSuccessAlert("Emails deleted successfully.");

                    } else {
                    showDangerAlert("Failed to delete emails.");
                    }
                }
            });
        } else {
            showDangerAlert("No email selected for deletion.");
        }
    }

    @FXML
    protected void onForwardButtonClick() {
        ObservableList<Email> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Forward");
        }else{
            System.out.println("[INFO] No email selected for forward");
        }
    }

    @FXML
    protected void onReplyButtonClick() {
        System.out.println("[INFO] Replying Email...");
        ObservableList<Email> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Reply");
        }
    }

    @FXML
    protected void onReplyAllButtonAction() {
        System.out.println("[INFO] Replying Email...");
        ObservableList<Email> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Reply All");
        }else{
            System.out.println("[INFO] No email selected for reply all ");
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
            System.err.println("[ERROR] Error loadind FXML file: " + e.getMessage());
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
