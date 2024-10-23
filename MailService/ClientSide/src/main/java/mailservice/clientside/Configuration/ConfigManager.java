package mailservice.clientside.Configuration;

import java.io.*;
import java.util.Properties;

public class ConfigManager {

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
