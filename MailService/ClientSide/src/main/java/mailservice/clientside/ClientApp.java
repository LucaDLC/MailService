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

import static mailservice.shared.enums.LogType.*;

public class ClientApp extends Application {
    private static ScheduledExecutorService operationPool;
    private static final int threadsNumber = 1;


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("ClientSide - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        stage.setOnCloseRequest(event -> stop());
    }


    @Override
    public void stop() {
        ClientModel.log(SYSTEM, "Application is stopping...");
        stopPeriodicFetch();
    }


    public static void startPeriodicFetch() {
        if (operationPool == null || operationPool.isShutdown() || operationPool.isTerminated()) {
            operationPool = Executors.newScheduledThreadPool(threadsNumber);
        }
        ClientModel client = ClientModel.getInstance();
        client.fetchEmails(true);
        operationPool.scheduleAtFixedRate(() -> client.fetchEmails(false), 5, client.getFetchPeriod(), TimeUnit.SECONDS);
    }

    public static void stopPeriodicFetch(){
        if (operationPool != null && !operationPool.isShutdown()) {
            operationPool.shutdown();
            try {
                if (!operationPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    ClientModel.log(ERROR, "Forcefully shutting down operation pool...");
                    operationPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                ClientModel.log(ERROR, "Interrupted during pool termination.");
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
