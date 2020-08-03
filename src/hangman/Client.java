package hangman;

import java.io.Serializable;
import java.util.function.Consumer;

public class Client extends ClientConnection {

    private String ip;
    private int port;

    public Client(String ip, int port, Consumer<Serializable> callback) {
        super(callback);
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String getIP() {
        // TODO Auto-generated method stub
        return this.ip;
    }

    @Override
    public int getPort() {
        // TODO Auto-generated method stub
        return this.port;
    }



}