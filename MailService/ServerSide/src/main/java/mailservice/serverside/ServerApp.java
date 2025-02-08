package mailservice.serverside;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ServerApp extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(mailservice.serverside.ServerApp.class.getResource("ServerLog.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);

        // Aggiungi il file CSS per l'aspetto dell'interfaccia
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("server.css")).toExternalForm());

        stage.setTitle("ServerSide - Log");
        stage.setScene(scene);

        stage.setMinWidth(640);
        stage.setMinHeight(480);

        stage.show();

    }


    @Override
    public void stop(){
        Platform.exit();
        System.exit(0);
    }


    public static void main(String[] args) {
        launch();
    }

}