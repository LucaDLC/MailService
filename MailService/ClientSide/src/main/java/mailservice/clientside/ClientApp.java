package mailservice.clientside;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mailservice.clientside.Model.ClientModel;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientApp extends Application {
    private static ScheduledExecutorService operationPool;
    private static final int threadsNumber = 2;


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> stop());
    }


    @Override
    public void stop() {
        System.out.println("[INFO] Application is stopping...");
        if (operationPool != null) {
            operationPool.shutdown();
            try {
                if (!operationPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("[ERROR] Forcefully shutting down operation pool...");
                    operationPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Interrupted during pool termination.");
                operationPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

    }


    public static void startPeriodicFetch() {
        ClientModel client = ClientModel.getInstance();
        client.fetchEmails(true);
        operationPool.scheduleAtFixedRate(() -> client.fetchEmails(false), 5, client.getFetchPeriod(), TimeUnit.SECONDS);
    }


    public static void main(String[] args) {
        operationPool = Executors.newScheduledThreadPool(threadsNumber);
        launch(args);
    }
}
