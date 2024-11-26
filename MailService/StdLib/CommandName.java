package mailservice.stdlib;
import java.io.Serializable;

public enum CommandRequest implements Serializable {
    FETCH_EMAIL,
    SEND_EMAIL,
    LOGIN_CHECK,
    DELETE_EMAIL;

}

public enum CommandResponse implements Serializable {
    SUCCESS,
    ILLEGAL_PARAMS,
    GENERIC_ERROR;
}
