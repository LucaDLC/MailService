package mailservice.clientside.Configuration;

import java.io.Serializable;

public record Request(String logged, CommandRequest cmdName,
                            Email mail) implements Serializable {

    @Override
    public String toString() {
        return "Request{ " +
                "User=" + logged +
                ", Command=" + cmdName.toString() +
                ((mail != null) ?
                        ", Mail =" + mail.getId() :
                        "") +
                " }";
    }
}
