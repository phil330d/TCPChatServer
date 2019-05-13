package ue08_tcp;

import java.net.Socket;

public class LogClient {
    public LogClient(Socket cltSocket) {
        new ClientHandler(cltSocket, "log");
    }

    private void log() {

    }
}
