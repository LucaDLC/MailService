package mailservice.serverside.Configuration;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private final Properties prop;
    private static ConfigManager instance;


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


    public static synchronized ConfigManager getInstance(){
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

}
