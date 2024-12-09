package mailservice.clientside;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailservice.clientside.Configuration.CommandResponse;
import mailservice.clientside.Configuration.ConfigManager;
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
    private static ScheduledExecutorService keepAliveScheduler;
    private static Date lastFetch = new Date(Long.MIN_VALUE);

    NetworkManager networkManager = NetworkManager.getInstance();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/mailservice/clientside/style.css")).toExternalForm());

        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.show();

        startFetchingEmails();
        startPingKeepAlive();

        stage.setOnCloseRequest(event -> {
            stopFetchingEmails();
            stopPingKeepAlive();
        });
    }

    public void startFetchingEmails() {
        fetchEmails = Executors.newSingleThreadScheduledExecutor();
        fetchEmails.scheduleAtFixedRate(() -> {
            try {
                if (!networkManager.isConnected() && !networkManager.connectToServer()) {
                    System.out.println("[ERROR] Unable to reconnect to server.");
                    return;
                }

                String userEmail = ConfigManager.getInstance().readProperty("Client.Mail").trim();
                System.out.println("[DEBUG] Fetching emails for: " + userEmail);

                if (networkManager.sendMessage(CommandRequest.FETCH_EMAIL, userEmail)) {
                    CommandResponse response = networkManager.receiveMessage();
                    if (response == CommandResponse.SUCCESS) {
                        String payload = networkManager.getLastPayload();
                        System.out.println("[INFO] Emails fetched successfully.");
                        if (payload == null || payload.isEmpty() || payload.startsWith("No emails")) {
                            System.out.println("[INFO] No emails found for user: " + userEmail);
                        } else {
                            // Aggiorna la lista delle email (inserisci logica per aggiornare la GUI se necessario)
                            System.out.println("[DEBUG] Emails received: " + payload);
                        }
                    } else {
                        System.out.println("[WARNING] Email fetch failed. Response: " + response);
                    }
                } else {
                    System.out.println("[ERROR] Failed to send FETCH_EMAIL command.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Exception during email fetch: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void startPingKeepAlive() {
        keepAliveScheduler = Executors.newSingleThreadScheduledExecutor();
        keepAliveScheduler.scheduleAtFixedRate(() -> {
            try {
                if (networkManager.isConnected()) {
                    if (networkManager.sendMessage(CommandRequest.PING, "")) {
                        System.out.println("[INFO] PING sent to keep the connection alive.");
                    } else {
                        System.out.println("[WARNING] PING failed. Attempting to reconnect...");
                        networkManager.connectToServer();
                    }
                } else {
                    System.out.println("[WARNING] Not connected. Attempting to reconnect...");
                    networkManager.connectToServer();
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Exception during PING: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS); // Esegue il PING ogni 30 secondi
    }

    @Override
    public void stop() {
        System.out.println("[INFO] Application is stopping...");
        stopFetchingEmails();
        stopPingKeepAlive();
        networkManager.disconnectFromServer();
    }

    private void stopFetchingEmails() {
        if (fetchEmails != null && !fetchEmails.isShutdown()) {
            fetchEmails.shutdownNow();
            System.out.println("[INFO] Stopped fetching emails.");
        }
    }

    private void stopPingKeepAlive() {
        if (keepAliveScheduler != null && !keepAliveScheduler.isShutdown()) {
            keepAliveScheduler.shutdownNow();
            System.out.println("[INFO] Stopped PING keep-alive task.");
        }
    }

    public static void main(String[] args) {
        try {
            Application.launch(ClientApp.class, args);
        } catch (Exception e) {
            System.out.println("Error initializing application: " + e.getMessage());
        }
    }
}
