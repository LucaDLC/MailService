package mailservice.clientside.Configuration;

import java.io.Serializable;

public record Request(String logged, CommandRequest cmdName,
                            Email arg) implements Serializable {

    @Override
    public String toString() {
        return "Request{ " +
                "User=" + logged +
                ", Command=" + cmdName.toString() +
                ((arg != null) ?
                        ", Argument =" + arg.getId() :
                        "") +
                " }";
    }
}
