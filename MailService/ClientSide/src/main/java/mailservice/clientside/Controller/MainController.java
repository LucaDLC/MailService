package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import mailservice.clientside.ClientApp;
import mailservice.shared.Email;
import mailservice.clientside.Model.ClientModel;

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
    public Button ReplyButton;
    @FXML
    public Button ReplyAllButton;
    @FXML
    public Button LogoutButton;

    private ClientModel clientModel = null;


    @FXML
    protected void initialize() {
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

        // Collega la ListView alla lista osservabile
        MailList.setItems(clientModel.getEmailList());

        // Set up il cell factory per visualizzare i soggetti delle email
        MailList.setCellFactory(lv -> new ListCell<>() {
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

        ClientApp.startPeriodicFetch();
    }


    private void displayEmailDetails(Email email) {
        if (email != null) {
            Platform.runLater(() -> {
                SenderLabel.setText(email.getSender());
                ReceiverLabel.setText(String.join(", ", email.getReceivers()));
                ObjectLabel.setText(email.getSubject());
                DateLabel.setText(email.getDate());
                MailContent.getEngine().loadContent(email.getText());
            });
        }
    }


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
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            showDangerAlert("Please select an email to delete.");
            return;
        }

        // Conferma l'azione di eliminazione
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete Confirmation");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("Are you sure you want to delete the selected email?");

        // Se l'utente conferma, procedi con l'eliminazione
        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Chiamata al metodo wrapDeleteEmail nel client
                if (ClientModel.getInstance().wrapDeleteEmail(selectedEmail)) {
                    // Se l'eliminazione va a buon fine, rimuovi l'email dalla lista
                    MailList.getItems().remove(selectedEmail);
                    showSuccessAlert("Email deleted successfully.");
                    Platform.runLater(() -> {
                        SenderLabel.setText("");
                        ReceiverLabel.setText("");
                        ObjectLabel.setText("");
                        DateLabel.setText("");
                        MailContent.getEngine().loadContent(""); // Pulisce il contenuto del WebView
                    });
                } else {
                    // Se l'eliminazione fallisce, mostra un messaggio di errore
                    showDangerAlert("Failed to delete the selected email.");
                }
            }
        });
    }


    @FXML
    protected void onForwardButtonClick() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            showDangerAlert("Please select an email to forward.");
            return;
        }
        System.out.println("[INFO] Forwarding Mail.");
        showComposeWindow("", "Fwd: " + selectedEmail.getSubject(), "Forwarded message: " + selectedEmail.getText());
    }


    @FXML
    protected void onReplyButtonClick() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            showDangerAlert("Please select an email to reply.");
            return;
        }
        System.out.println("[INFO] Replying Mail.");
        showComposeWindow(selectedEmail.getSender(), "Re: " + selectedEmail.getSubject(), "On " + selectedEmail.getDate() + ", " + selectedEmail.getSender() + " Wrote: " + selectedEmail.getText());
    }


    @FXML
    protected void onReplyAllButtonAction() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            showDangerAlert("Please select an email to reply.");
            return;
        }
        System.out.println("[INFO] Replying All Mail.");

        String allRecipients = String.join(", ", selectedEmail.getReceivers());
        showComposeWindow(allRecipients, "Re: " + selectedEmail.getSubject(), "On " + selectedEmail.getDate() + ", " + selectedEmail.getSender() + " Wrote: " + selectedEmail.getText());
    }


    @FXML
    protected void onLogoutButtonClick() {
        System.out.println("[INFO] Logging out...");

    }


    private void showComposeWindow(String recipients, String subject, String body) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load();
            ComposeController composeController = loader.getController();
            composeController.setRecipientFieldID(recipients);
            composeController.setObjectFieldID(subject);
            composeController.setMailBody(body);

            Stage composeStage = new Stage();
            composeStage.setScene(new Scene(composeView));
            composeStage.setTitle("ClientSide - Mail Compose");
            composeStage.initModality(Modality.APPLICATION_MODAL);
            composeStage.show();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load MailCompose.fxml: " + e.getMessage());
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
            System.out.println("Showing success alert: " + message); // Aggiungi questa linea per il debug
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
