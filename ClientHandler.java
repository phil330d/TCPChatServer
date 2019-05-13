package ue08_tcp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClientHandler implements Comparable<ClientHandler> {
    public static final Charset CHARSET = Charset.forName("ISO-8859-1");
    private final Socket cltSocket;
    private BufferedWriter out = null;
    private BufferedReader in = null;
    private String username;
    private static final String STZ_DAVOR = new String(new byte[]{0x0b, 0x1b, '[', '1', 'A', 0x1b, '7', 0x1b, '[', '1', 'L', '\r'});
    private static final String STZ_DANACH = new String(new byte[]{0x1b, '8', 0x1b, '[', '1', 'B'});
    public static boolean closed = false;
    public static final Pattern statPattern = Pattern.compile("stat((\\d)*([+-]))?");

    public ClientHandler(Socket cltSocket) {
        this.cltSocket = cltSocket;

        new Thread(this::handleClient).start();
    }

    //log
    public ClientHandler(Socket cltSocket, String username) {
        this.cltSocket = cltSocket;
        this.username = username;
    }

    private void handleClient() {
        try {
            out = new BufferedWriter(new OutputStreamWriter(cltSocket.getOutputStream(), CHARSET));
            in = new BufferedReader(new InputStreamReader(cltSocket.getInputStream(), CHARSET));
            this.sendMessage("Willkommen beim Chat-Server der 3CI\r\n\r\nUm die Verbindung zu beenden gib quit ein.\r\n\r\nWelchen Spitznamen moechtest du haben: ", false);

            while (this.getUsername() == null) {
                String temp = in.readLine().trim();
                if (!Server.getUsernames().contains(temp) && !temp.trim().equals("")) {
                    this.username = temp;
                } else {
                    this.sendMessage("Dieser Nickname ist entweder schon vergeben oder nicht erlaubt!\r\n" +
                            "Anderer Nickname: ", false);
                }
            }

            Map<String, Integer> tempMap = Server.getMessageCount();
            tempMap.put(this.username, 0);
            Server.setMessageCount(tempMap);

            Server.sendJoinMessage("\"" + this.getUsername() + "\" hat den Raum betreten.", this);


            System.out.println("New User: \"" + username + "\" connected! (IP: " + cltSocket.getInetAddress().getHostAddress() + "; Port: " + cltSocket.getPort() + ")");

            while (!closed) {
                out.write(this.username + ">");
                out.flush();
                String temp = in.readLine();
                Matcher matcher = statPattern.matcher(temp);
                if (temp.equals("quit")) {
                    out.write("Bye!\n");
                    out.flush();
                    cltSocket.close();
                    Server.sendServerMessage("\"" + this.getUsername() + "\" hat den Raum verlassen.");
                    break;
                } else if (matcher.matches()) {
                    //TODO - not working
                    if (matcher.group(1) != null) {
                        if (matcher.group(3).equals("+")) {
                            Map<String, Integer> sorted = Server.getMessageCount()
                                    .entrySet().stream()
                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey, Map.Entry::getValue,
                                            (e1, e2) -> e1, LinkedHashMap::new
                                    ));
                            out.write(Arrays.toString(Arrays.copyOfRange(sorted.entrySet().toArray(), 0, Integer.min(Integer.parseInt(matcher.group(2)), Server.getMessageCount().keySet().size()))) + "\n");
                        } else if (matcher.group(3).equals("-")) {
                            Map<String, Integer> sorted = Server.getMessageCount()
                                    .entrySet().stream()
                                    .sorted(Map.Entry.comparingByValue())
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey, Map.Entry::getValue,
                                            (e1, e2) -> e1, LinkedHashMap::new
                                    ));
                            out.write(Arrays.toString(Arrays.copyOfRange(sorted.entrySet().toArray(), 0, Integer.min(Integer.parseInt(matcher.group(2)), Server.getMessageCount().keySet().size()))) + "\n");
                        }
                    } else {
                        out.write(Arrays.toString(Server.getMessageCount().entrySet().toArray()) + "\n");
                    }
                    out.flush();
                } else {
                    Server.sendMessageToAll(this, temp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int compareTo(ClientHandler other) {
        return username.compareToIgnoreCase(other.username);
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message, boolean isMessage) {
        try {
            if (isMessage) {
                out.write(STZ_DAVOR + message + STZ_DANACH);
            } else {
                out.write(message);
            }
            out.flush();
        } catch (
                IOException e) {
            try {
                cltSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    public void disconnect() {
        try {
            closed = true;
            this.in.close();
            this.out.close();
            this.cltSocket.close();
            Server.removeClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
