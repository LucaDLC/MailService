package mailservice.clientside.Configuration;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    /* SPIEGAZIONE CONFIGMANAGER
    Questa Classe è usata per inizializzare e leggere le proprietà del ClientSide
    Creo un file di configurazione se non esiste, altrimenti leggo le proprietà dal file
    Volendo solo un istanza di questa classe, ho usato il Singleton Pattern mettendo il costruttore privato e usando un getInstance per ottenere l'istanza
    Una volta chiamato il ConfigManager.getInstance() posso usare il metodo readProperty(Nome Proprietà) per leggere le proprietà
    Essendo un Singleton, le proprietà vengono caricate una sola volta e poi restano in memoria e ConfigManager.getInstance() restituisce sempre la stessa istanza
    */
    private final Properties prop;

    private ConfigManager() {
        prop = new Properties();
        final File path = getDir();

        try{
            if (!path.exists()) {
                prop.setProperty("Client.Email", "CHANGE_ME@EXAMPLE.IT");
                prop.setProperty("Client.ServerHost", "127.0.0.1");
                prop.setProperty("Client.ServerPort", "42069");
                prop.setProperty("Client.Fetch", "5");


                prop.store(new FileOutputStream(path),  null);
            } else {
                prop.load(new FileInputStream(path));
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static ConfigManager getInstance(){
        return new ConfigManager();
    }

    public String readProperty (String propName){
        return prop.getProperty(propName);
    }

    private File getDir() {
        String uri = new File("").getAbsolutePath() + "/ClienSide/src/main/user.properties";
        return new File(uri);
    }
}
