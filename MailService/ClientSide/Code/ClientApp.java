package ClientSide.Code;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ClientApp extends Application {

    //Inizializzazione delle variabili globali per la gestione della grafica e locali per la gestione delle mail in ricezione

    public static Model model;
    public static SceneController sceneController;
    private static ExecutorService clientFX;
    private static ScheduledExecutorService fetchEmails;
    private static Date lastFetch = new Date(Long.MIN_VALUE);

    @Override
    public void start(Stage stage) throws IOException {

        //Creazione dell'interfaccia grafica lato Client, Vengono inizializzate il Main e la MailCompose ma viene mostrata inizialmente solo il Main da dove si ha accesso alla lista di mail ricevute
        //Dal Main si può accedere alla MailCompose che viene visualizzata successivamente all'intenzione di voler scrivere o rispondere ad una mail

        Scene scene = new Scene(new Pane(), 1280, 720);
        sceneController = SceneController.getInstance(scene);
        sceneController.addScene(SceneName.MAIN);
        sceneController.addScene(SceneName.MAILCOMPOSE);
        stage.setTitle("ClientMailApp");
        sceneController.switchTo(SceneName.MAIN);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop(){
    // Aggiungere cosa deve fare alla chiusura dell'app
    }

    public static void main(String[] args) {
        model = Model.getInstance();
        appFX = Executors.newSingleThreadExecutor();
        fetchEmails = Executors.newSingleThreadScheduledExecutor();
        Client client = model.getClient();

        //Start JavaFX app using method reference
        appFX.execute(Application::launch);

    }