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
import static mailservice.shared.enums.LogType.*;

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
    private Button ComposeButton;
    @FXML
    private Button ForwardButton;
    @FXML
    private Button DeleteButton;
    @FXML
    private Button ReplyButton;
    @FXML
    private Button ReplyAllButton;
    @FXML
    private Button LogoutButton;

    private ClientModel clientModel = null;


    @FXML
    protected void initialize() {
        ClientModel.log(SYSTEM,"Initializing MainController...");
        clientModel = ClientModel.getInstance();

        if (clientModel == null) {
            ClientModel.log(ERROR, "Failed to initialize ClientModel.");
            showStatus("Initialization Error!");
            ComposeButton.setDisable(true);
            ForwardButton.setDisable(true);
            DeleteButton.setDisable(true);
            Platform.exit();
            return;
        }

        ClientModel.log(SYSTEM, "ClientModel initialized successfully.");
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


        clientModel.isServerReachable().addListener((obs, oldValue, newValue) -> {
            if (!newValue && clientModel!=null) {
                showStatus("Server Unreachable!");
            } else {
                hideAlerts();
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
        ClientModel.log(INFO, "Composing Mail.");
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
                Parent composeView = loader.load();
                Scene composeScene = new Scene(composeView);
                Stage composeStage = new Stage();
                composeStage.setScene(composeScene);
                composeStage.setTitle("ClientSide - Mail Compose");
                composeStage.setResizable(false);
                composeStage.initModality(Modality.APPLICATION_MODAL);
                composeStage.show();
            } catch (IOException e) {
                ClientModel.log(ERROR, "Failed to load MailCompose.fxml: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }


    @FXML
    protected void onDeleteButtonClick() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Delete Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Please select an email to delete.");

            errorAlert.showAndWait();
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
                    MailList.getSelectionModel().clearSelection();
                    Platform.runLater(() -> {
                        SenderLabel.setText("");
                        ReceiverLabel.setText("");
                        ObjectLabel.setText("");
                        DateLabel.setText("");
                        MailContent.getEngine().loadContent(""); // Pulisce il contenuto del WebView
                    });

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);  // Tipo di alert per successo
                    alert.setTitle("Email deleted.");
                    alert.setHeaderText(null);  // Nessun testo di intestazione
                    alert.setContentText("Email deleted successfully.");
                    alert.showAndWait();

                } else {
                    // Se l'eliminazione fallisce, mostra un messaggio di errore
                    Alert alert = new Alert(Alert.AlertType.ERROR);  // Tipo di alert per successo
                    alert.setTitle("Error Deleting Email.");
                    alert.setHeaderText(null);  // Nessun testo di intestazione
                    alert.setContentText("Failed to delete the selected email.");
                    alert.showAndWait();
                }
            }
        });
    }


    @FXML
    protected void onForwardButtonClick() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Forward Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Please select an email to forward.");

            errorAlert.showAndWait();
            return;
        }
        ClientModel.log(INFO, "Forwarding Mail.");
        showComposeWindow("", "Fwd: " + selectedEmail.getSubject(), "Forwarded message: " + selectedEmail.getText());
    }


    @FXML
    protected void onReplyButtonClick() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Reply Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Please select an email to reply.");

            errorAlert.showAndWait();
            return;
        }
        ClientModel.log(INFO, "Replying Mail.");
        showComposeWindow(selectedEmail.getSender(), "Re: " + selectedEmail.getSubject(), "On " + selectedEmail.getDate() + ", " + selectedEmail.getSender() + " Wrote: " + selectedEmail.getText());
    }


    @FXML
    protected void onReplyAllButtonAction() {
        Email selectedEmail = MailList.getSelectionModel().getSelectedItem();

        if (selectedEmail == null) {
            // Se nessuna email è selezionata, mostra un avviso
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Reply All Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Please select an email to reply.");

            errorAlert.showAndWait();
            return;
        }
        ClientModel.log(INFO, "Replying All Mail.");

        String allRecipients = String.join(", ", selectedEmail.getReceivers());
        showComposeWindow(allRecipients, "Re: " + selectedEmail.getSubject(), "On " + selectedEmail.getDate() + ", " + selectedEmail.getSender() + " Wrote: " + selectedEmail.getText());
    }


    @FXML
    protected void onLogoutButtonClick() {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Logout Confirmation");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("Are you sure you want to logout?");

        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ClientModel.log(INFO, "Logging out...");
                ClientApp.stopPeriodicFetch();
                clientModel.logout ();

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/Login.fxml"));
                    Parent loginView = loader.load(); //carico il file FXML
                    Scene loginScene = new Scene(loginView); //creo una nuova scena
                    Stage loginStage = new Stage(); //creo una nuova finestra

                    loginStage.setScene(loginScene); //imposto la scena nella finestra
                    loginStage.setTitle("ClientSide - Login");
                    loginStage.setResizable(false);
                    loginStage.initModality(Modality.APPLICATION_MODAL); //consente di interagire con entrambe le finestre
                    loginStage.show();

                    //chiudo la finestra del main
                    Stage stage = (Stage) LogoutButton.getScene().getWindow();
                    stage.setResizable(false);
                    stage.close();

                } catch (IOException e) {
                    ClientModel.log(ERROR, "Unable to load Login.fxml: " + e.getMessage());
                }
            }
        });

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
            ClientModel.log(ERROR, "Failed to load MailCompose.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void showStatus(String message) {
        Platform.runLater(() -> {
            if (dangerAlert != null) {
                dangerAlert.getChildren().clear();
                Text dangerText = new Text(message);
                dangerText.setFill(Color.RED);
                dangerAlert.getChildren().add(dangerText);
                dangerAlert.setVisible(true);
            }
        });
    }



    @FXML
    private void hideAlerts() {
        // Nascondere gli alert dopo 1 secondi
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> {
            dangerAlert.setVisible(false);
        });
        pause.play();
    }
}
