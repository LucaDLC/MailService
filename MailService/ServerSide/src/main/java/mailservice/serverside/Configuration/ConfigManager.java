package mailservice.serverside.Configuration;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

public class ConfigManager {
    /* SPIEGAZIONE CONFIG MANAGER
    Questa Classe è usata per inizializzare e leggere le proprietà del ServerSide
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
                prop.setProperty("Server.Timeout", "2000");
                prop.setProperty("Server.Threads", "5");
                prop.setProperty("Server.Port", "42069");


                prop.store(new FileOutputStream(path),  null);
            } else {
                prop.load(new FileInputStream(path));
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static ConfigManager instance;
    public static ConfigManager getInstance(){
        if(instance == null){
            instance = new ConfigManager();
        }
        return instance;
    }

    public String readProperty (String propName){
        return prop.getProperty(propName);
    }

    private File getDir() {
        String uri = new File("").getAbsolutePath() + File.separator + "ServerSide" + File.separator + "src" + File.separator + "main" + File.separator + "Server.properties";
        return new File(uri);
    }

    public boolean validateEmail(String email) {
        return Pattern.matches("^[a-zA-Z0-9.@_%+-]+@rama.it$", email.toLowerCase());
    }
}
