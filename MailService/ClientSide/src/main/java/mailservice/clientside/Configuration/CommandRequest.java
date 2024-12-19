package mailservice.clientside.Configuration;
import java.io.Serializable;

public enum CommandRequest implements Serializable {
    FETCH_EMAIL,
    SEND_EMAIL,
    LOGIN_CHECK,
    DELETE_EMAIL;

    public String toString() {
        return this.name();
    }

}

