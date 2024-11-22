package mailservice.clientside.Controller;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML; //importo la classe FXML
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;  //importo la classe Label
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;//importo la classe WebView, che visualizza contenuti web
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mailservice.clientside.ClientApp;
import mailservice.clientside.Configuration.ConfigManager;
import mailservice.clientside.Model.ClientModel;
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

    private ScheduledExecutorService scheduler;
    private Timer emailRefreshTimer;
    private static final long REFRESH_INTERVAL = 10000; //10 secondi di intervallo tra i refresh

    @FXML
    public void initialize() {
        // Impostiamo il testo della MailLabel all'apertura della UI
        ConfigManager configManager = ConfigManager.getInstance();
        MailLabel.setText(configManager.readProperty("Client.Mail"));

        startAutomaticRefresh(); //avvia il refresh automatico
    }

    private void startAutomaticRefresh(){
        if (emailRefreshTimer != null) {
            emailRefreshTimer.cancel(); // Cancella eventuali task in corso
        }

        emailRefreshTimer = new Timer(true);
        emailRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshEmails();
            }
        }, 0, REFRESH_INTERVAL);
    }

    private void refreshEmails(){
        ClientModel clientModel = ClientModel.getInstance();
        if(clientModel.connectToServer()){
            System.out.println("Fetching emails automatically...");
            String[] emails = clientModel.fetchEmails(); //la logica per recuperare le email dal server va nel model
            //serve per aggiornare la GUI in modo sicuro (JavaFX richiede che le modifiche alla GUI vengano eseguite nel thread dell'interfaccia utente)
            javafx.application.Platform.runLater(()->updateEmailList(emails));
            clientModel.disconnectFromServer();
        }else{
            System.out.println("Unable to connect to server for automatic fetch");
        }
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
        getDangerAlert();
        Text dangerText = new Text(message);
        dangerText.setFill(Color.RED);
        dangerAlert.getChildren().add(dangerText);
        dangerAlert.setVisible(true);
        hideAlerts();
        //aggiungo il messaggio di errore al campo dangerAlert
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

    public TextFlow getDangerAlert() {
        dangerAlert.getChildren().clear(); //serve a pulire il campo dove verrà visualizzato il messaggio di errore nel caso in cui ci sia già un messaggio
        return dangerAlert;
    }
}
