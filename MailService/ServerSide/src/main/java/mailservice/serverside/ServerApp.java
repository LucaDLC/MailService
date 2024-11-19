package mailservice.serverside;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApp extends Application {

    private static ExecutorService GUI;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(mailservice.serverside.ServerApp.class.getResource("ServerLog.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("ServerSide - Log");
        stage.setScene(scene);
        stage.show();
    }

    public void stop() {
        GUI.shutdown();
        try {
            if (!GUI.awaitTermination(3, TimeUnit.SECONDS)) {
                GUI.shutdownNow();
            }
        } catch (InterruptedException e) {
            GUI.shutdownNow();
        }
    }

    public static void main(String[] args) {
        GUI = Executors.newSingleThreadExecutor();
        GUI.execute(Application::launch);
    }
}