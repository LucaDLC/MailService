package mailservice.serverside.Configuration;
import java.io.Serializable;

public enum CommandResponse implements Serializable {
    SUCCESS,
    ILLEGAL_PARAMS,
    GENERIC_ERROR;
}
