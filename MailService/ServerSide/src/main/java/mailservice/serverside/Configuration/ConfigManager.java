package mailservice.serverside.Configuration;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

public class ConfigManager {
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
