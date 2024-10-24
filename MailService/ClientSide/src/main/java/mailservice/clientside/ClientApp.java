package mailservice.clientside;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientApp extends Application {

    private static ExecutorService clientFX;
    private static ScheduledExecutorService fetchEmails;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("ClientSide - ClientApp");
        stage.setScene(scene);
        stage.show();

        // Initialize services
        clientFX = Executors.newSingleThreadExecutor();
        fetchEmails = Executors.newScheduledThreadPool(1);
        fetchEmails.scheduleAtFixedRate(() -> {
            // Fetch emails logic
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        clientFX.shutdown();
        fetchEmails.shutdown();
        try {
            if (!clientFX.awaitTermination(5, TimeUnit.SECONDS)) {
                clientFX.shutdownNow();
            }
            if (!fetchEmails.awaitTermination(5, TimeUnit.SECONDS)) {
                fetchEmails.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientFX.shutdownNow();
            fetchEmails.shutdownNow();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch();
    }
}