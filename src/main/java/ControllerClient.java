import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import java.net.InetAddress;

public class ControllerClient {
    private static ModelControllerClient client;
    private static CommandContext ctx;

    ControllerClient(String host, int port, String request) {
        String userName = "admin";
        String password = "admin";
        int localport = Main.ReadConfig.port;
        try {
            client = ModelControllerClient.Factory.create(
                    InetAddress.getByName(host), localport,
                    callbacks -> {
                        for (Callback current : callbacks) {
                            if (current instanceof NameCallback) {
                                NameCallback ncb = (NameCallback) current;
                                ncb.setName(userName);
                            } else if (current instanceof PasswordCallback) {
                                PasswordCallback pcb = (PasswordCallback) current;
                                //pcb.setPassword(String.valueOf(passwordTxt.getPassword().toCharArray()));
                                pcb.setPassword(password.toCharArray());
                            } else if (current instanceof RealmCallback) {
                                RealmCallback rcb = (RealmCallback) current;
                                rcb.setText(rcb.getDefaultText());
                            } else {
                                throw new UnsupportedCallbackException(current);
                            }
                        }
                    });

        } catch (java.net.UnknownHostException | java.lang.NullPointerException e) {
            e.fillInStackTrace();
        }

        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(host, port, userName, password.toCharArray());
        } catch (CommandLineException e) {
            e.fillInStackTrace();
        }

        try {
            ctx.connectController();
            ctx.handle(request);
        } catch (CommandLineException e) {
            e.fillInStackTrace();
        }
    }
}
