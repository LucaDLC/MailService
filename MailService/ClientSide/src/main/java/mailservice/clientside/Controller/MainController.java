package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;//importo la classe WebView, che visualizza contenuti web
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.Configuration.CommandRequest;
import mailservice.clientside.Configuration.CommandResponse;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;
import mailservice.clientside.Network.NetworkManager;

import java.util.Timer;
import java.util.TimerTask;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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

    private Timer emailRefreshTimer;
    private static final long REFRESH_INTERVAL = 10000; //10 secondi di intervallo tra i refresh

    @FXML
    public void initialize() {
        // Impostiamo il testo della MailLabel all'apertura della UI
        ConfigManager configManager = ConfigManager.getInstance();
        MailLabel.setText(configManager.readProperty("Client.Mail"));

        startAutomaticRefresh(); //avvia il refresh automatico

        MailList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals("No emails found.")) {
                displayEmailDetails(newValue); // Mostra i dettagli della email selezionata
            }
        });
    }

    private void displayEmailDetails(String emailDetails) {
        try {
            // Supponiamo che i dettagli della email siano salvati in questo formato:
            // "From: sender@example.com|To: receiver1@example.com,receiver2@example.com|Object: Subject|Date: 2024-12-07"
            String[] parts = emailDetails.split("\\|");
            String sender = parts[0].substring("From: ".length());
            String receivers = parts[1].substring("To: ".length());
            String object = parts[2].substring("Object: ".length());
            String date = parts[3].substring("Date: ".length());

            // Aggiorna i campi nella GUI
            SenderLabel.setText(sender);
            ReceiverLabel.setText(receivers);
            ObjectLabel.setText(object);
            DateLabel.setText(date);

            // Mostra il contenuto nel WebView (se supportato)
            MailContent.getEngine().loadContent("<p><strong>Oggetto:</strong> " + object + "</p><p>Contenuto non disponibile.</p>");
        } catch (Exception e) {
            System.err.println("Error parsing email details: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private ScheduledExecutorService emailRefreshScheduler;

    private void startAutomaticRefresh(){
        if (emailRefreshScheduler != null && !emailRefreshScheduler.isShutdown()) {
            emailRefreshScheduler.shutdownNow(); // Ferma il task precedente
        }

        emailRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
        emailRefreshScheduler.scheduleAtFixedRate(() -> refreshEmails(), 0, REFRESH_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void stopAutomaticRefresh() {
        if (emailRefreshScheduler != null && !emailRefreshScheduler.isShutdown()) {
            emailRefreshScheduler.shutdown();
        }
    }

    private void refreshEmails() {
        new Thread(() -> {
            NetworkManager networkManager = NetworkManager.getInstance();
            if (networkManager.connectToServer()) {
                System.out.println("Fetching emails automatically...");
                String[] emails = ClientModel.getInstance().fetchEmails();
                javafx.application.Platform.runLater(() -> updateEmailList(emails));
                networkManager.disconnectFromServer();
            } else {
                if (!networkManager.connectToServer()) {
                    javafx.application.Platform.runLater(() -> showDangerAlert("Unable to connect to server for automatic fetch"));
                }

                System.out.println("Unable to connect to server for automatic fetch");
            }
        }).start();
    }


    //metodo per aggiornare la ListView con le email ricevute
    public void updateEmailList(String[] emails){
        MailList.getItems().clear(); //pulisco la lista esistente
        for(String email : emails){
            MailList.getItems().add(email); //aggiungo l'email alla lista
        }
    }

    //implementazione delle azioni da eseguire quando si preme il bottone
    //metodo che viene chiamato quando si preme il bottone
    @FXML
    protected void onComposeButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/mailservice/clientside/MailCompose.fxml"));
            Parent composeView = loader.load();

            ComposeController composeController = loader.getController();
            composeController.setUpdateCallback(this::updateEmailList); // Passa il metodo di aggiornamento

            Scene composeScene = new Scene(composeView);
            Stage composeStage = new Stage();
            composeStage.setScene(composeScene);
            composeStage.setTitle("ClientSide - Mail Compose");
            composeStage.initModality(Modality.APPLICATION_MODAL);
            composeStage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            showDangerAlert("Error loading compose window.");
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
                    if (networkManager.connectToServer()) {
                        boolean success = networkManager.sendMessage(CommandRequest.DELETE_EMAIL, emailData);
                        if (success && CommandResponse.SUCCESS.name().equals(networkManager.getLastPayload())) {
                            Platform.runLater(() -> {
                                MailList.getItems().removeAll(selectedMails);
                                showSuccessAlert("Emails deleted successfully.");
                            });
                        } else {
                            Platform.runLater(() -> showDangerAlert("Failed to delete emails."));
                        }
                    } else {
                        Platform.runLater(() -> showDangerAlert("Failed to connect to the server."));
                    }
                }
            });
        } else {
            showDangerAlert("No email selected for deletion.");
        }
    }

    @FXML
    //handler per l'azione del bottone Delete
    protected void onDeleteButtonAction() {
        System.out.println("Deleting Email...");
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
    protected void onForwardButtonAction() {
        System.out.println("Forwarding Email...");
    }
    @FXML
    protected void onReplyButtonClick() {
        ObservableList<String> selectedMails = MailList.getSelectionModel().getSelectedItems();
        if(!selectedMails.isEmpty()) {
            showComposeWindow("Reply");
        }
    }
    @FXML
    protected void onReplyButtonAction() {
        System.out.println("Replying Email...");
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

    @FXML
    private void updateEmailList() {
        NetworkManager networkManager = NetworkManager.getInstance();

        if (!networkManager.connectToServer()) {
            System.out.println("Error: Not connected to the server");
            showDangerAlert("Error: Not connected to the server");
            return;
        }

        // Ottieni il nome utente corrente
        String username = ConfigManager.getInstance().readProperty("Client.Mail").split("@")[0];

        // Invia il comando FETCH_EMAIL con il nome utente
        boolean success = networkManager.sendMessage(CommandRequest.FETCH_EMAIL, username);
        if (success) {
            String payload = networkManager.getLastPayload(); // Recupera le email dal server
            if (payload != null) {
                // Controlla se il payload indica che non ci sono email
                if (payload.startsWith("No emails found")) {
                    System.out.println(payload);
                    Platform.runLater(() -> {
                        MailList.getItems().clear(); // Pulisci la lista
                        MailList.getItems().add("No emails found."); // Mostra un messaggio informativo
                    });
                } else {
                    // Dividi le email ricevute e aggiorna la lista
                    String[] emails = payload.split(",");
                    Platform.runLater(() -> {
                        MailList.getItems().clear();
                        for (String email : emails) {
                            MailList.getItems().add(email);
                        }
                    });
                }
            }
        } else {
            System.out.println("Failed to fetch emails.");
            showDangerAlert("Failed to fetch emails.");
        }
    }


}
