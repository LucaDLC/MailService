package mailservice.clientside;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.Executors;

public class ClientApp extends Application {

    //Inizializzazione delle variabili globali per la gestione della grafica e locali per la gestione delle mail in ricezione


    /*public static Model model;
    public static SceneController sceneController;
    private static ExecutorService clientFX;
    private static ScheduledExecutorService fetchEmails;
    private static Date lastFetch = new Date(Long.MIN_VALUE);

     */
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("ClientSide - ClientApp");
        stage.setScene(scene);
        stage.show();
    }

    public void stop() {
        //clientFX.shutdown();
        //fetchEmails.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch();
    }
}
    