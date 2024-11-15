package mailservice.clientside.Configuration;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    /* SPIEGAZIONE CONFIG MANAGER
    Questa Classe è usata per inizializzare e leggere le proprietà del ClientSide
    Creo un file di configurazione se non esiste, altrimenti leggo le proprietà dal file
    Volendo solo un istanza di questa classe, ho usato il Singleton Pattern mettendo il costruttore privato e usando un getInstance per ottenere l'istanza
    Una volta chiamato il ConfigManager.getInstance() posso usare il metodo readProperty(Nome Proprietà) per leggere le proprietà
    Essendo un Singleton, le proprietà vengono caricate una sola volta e poi restano in memoria e ConfigManager.getInstance() restituisce sempre la stessa istanza
    */
    private final Properties prop; //proprietà del file di configurazione
    final File path = getDir(); //ottengo il file di configurazione

    private ConfigManager() {
        prop = new Properties();    //inizializzo le proprietà

        try{
            if (!path.exists()) { //se il file non esiste lo creo
                prop.setProperty("Client.Mail", "Example@Rama.it"); //imposto l'email di default
                prop.setProperty("Client.ServerHost", "127.0.0.1"); //imposto l'indirizzo IP del server
                prop.setProperty("Client.ServerPort", "42069"); //imposto le porta del server
                prop.setProperty("Client.Fetch", "5");  //imposto l'ntervallo di controllo delle email(5 minuti)
                prop.store(new FileOutputStream(path),  null);
            } else {
                prop.load(new FileInputStream(path));   //se esiste, viene letto il file di configurazione per recuperare le impostazioni già salvate
            }
        } catch (IOException e){
            e.printStackTrace();    //stampo errore se qualcosa va storto
        }
    }

    public static ConfigManager getInstance(){
        return new ConfigManager();
    }   //restituisce un'unica istanza del ConfigManager, questo metodo è ciò che permette di usare il Singleton Pattern

    public String readProperty (String propName){
        return prop.getProperty(propName);
    }   //restituisce la proprietà richiesta

    public synchronized void setProperty (String propName, String propValue){
        try{
            if (!path.exists()) { //se il file non esiste lo creo
                prop.setProperty("Client.ServerHost", "127.0.0.1"); //imposto l'indirizzo IP del server
                prop.setProperty("Client.ServerPort", "42069"); //imposto le porta del server
                prop.setProperty("Client.Fetch", "5");  //imposto l'ntervallo di controllo delle email(5 minuti)
            }
            if (propName.equals("Client.Mail")) {
                prop.setProperty("Client.Mail", propValue);
            }
            prop.store(new FileOutputStream(path), null);

        } catch (IOException e){
            e.printStackTrace();    //stampo errore se qualcosa va storto
        }
    }

    private File getDir() {
        String url = new File("").getAbsolutePath() + File.separator + "ClientSide" + File.separator + "src" + File.separator + "main" + File.separator + "User.properties"; //ottengo il percorso del file
        return new File(url);   //restituisco il persorso del file di configurazione
    }
}
