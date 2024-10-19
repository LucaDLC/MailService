package ClientSide.Code.Scene;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class SceneController {

    //Utilizziamo un record quindi in questo caso 2 oggetti quali Pane e Controller in un solo contenitore che vengono inizializzati e su cui possiamo richiamare i metodi che gestiscono la finestra
    //Successivamente creiamo un Hashmap per associare il nome delle scene ai record contenenti i rispettivi Pane e Controller
    private record ClientWindow(Pane pane, Controller controller) {}

    private final HashMap<SceneName, ClientWindow> sceneMap;
    private final Scene main;


    private SceneController(Scene main){
        this.main = main;
        sceneMap = new HashMap<>();
    }

    public static SceneController getInstance(Scene main){

        return new SceneController(main);
    }


    public Controller getController(SceneName name){
        return sceneMap.get(name).controller();
    }

    public void addScene(SceneName name) throws IOException {
        String path = name.toString() + '/' + name + ".fxml";

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull
                (ClientApp.class.getResource(path)));
        sceneMap.put(name, new ClientWindow(loader.load(), loader.getController()));
    }

    public void switchTo(SceneName name){
        main.setRoot(sceneMap.get(name).pane());
    }
}
