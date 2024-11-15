package mailservice.serverside;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ServerApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(mailservice.serverside.ServerApp.class.getResource("ServerLog.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("ServerSide - Log");
        stage.setScene(scene);
        stage.show();
    }

    public void stop() {
        Platform.exit(); //termina la piattaforma JavaFX e chiude ccompletamente l'applicazione
    }

    public static void main(String[] args) {
        launch();
    }
}