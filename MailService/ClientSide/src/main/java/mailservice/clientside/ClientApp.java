package mailservice.clientside;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailservice.clientside.Network.NetworkManager;
import mailservice.clientside.Configuration.CommandRequest;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientApp extends Application {

    private static ScheduledExecutorService fetchEmails;
    private static Date lastFetch = new Date(Long.MIN_VALUE);

    @Override
    public void start(Stage stage) throws IOException {
        // Caricamento dell'interfaccia utente tramite FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Aggiungere il file CSS alla scena
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/mailservice/clientside/style.css")).toExternalForm());

        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.show();

        // Avvio del recupero periodico delle email
        startFetchingEmails();

        // Gestione della chiusura dell'applicazione
        stage.setOnCloseRequest(event -> stopFetchingEmails());
    }

    public static void startFetchingEmails() {
        fetchEmails = Executors.newSingleThreadScheduledExecutor();
        fetchEmails.scheduleAtFixedRate(() -> {
            try {
                if (!NetworkManager.isConnected() && !NetworkManager.connectToServer()) {
                    System.out.println("[ERROR] Unable to reconnect to server.");
                    return;
                }

                // Invio del comando FETCH_EMAIL con il timestamp dell'ultimo fetch
                if (NetworkManager.sendMessage(CommandRequest.FETCH_EMAIL, lastFetch.toString())) {
                    lastFetch = new Date();
                    System.out.println("[INFO] Emails fetched successfully at: " + lastFetch);
                } else {
                    System.out.println("[WARNING] Email fetch failed.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Exception during email fetch: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        NetworkManager.disconnectFromServer();
        stopFetchingEmails();
    }

    private void stopFetchingEmails() {
        if (fetchEmails != null && !fetchEmails.isShutdown()) {
            fetchEmails.shutdownNow();
            System.out.println("[INFO] Stopped fetching emails.");
        }
    }

    public static void main(String[] args) {
        try {
            // Avvio dell'applicazione JavaFX
            Application.launch(ClientApp.class, args);
        } catch (Exception e) {
            System.out.println("Error initializing application: " + e.getMessage());
        }
    }
}
