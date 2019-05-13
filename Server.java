package ue08_tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
Philipp Theurer
3CI
4/2/19
*/
public class Server {
    private static int server_port = 10_023;
    private static Set<ClientHandler> connectedClients = new HashSet<>();


    public static void main(String[] args) {
        try {
//            Scanner scanner = new Scanner(System.in);
//            System.out.println("Enter Port: [Leave blank for default (10023)]");
            //TODO uncomment for port selection
            //TODO disconnect without Socket closed Exception
            //TODO "type help for command list"
            //TODO "auch für die Serverkonsole braucht man einen Thread?"
//            String temp = scanner.nextLine();
//            if (!temp.isEmpty()) {
//                server_port = Integer.parseInt(temp);
//            }


            ServerSocket srvSocket = new ServerSocket(server_port);
            System.out.println("Server started on Port: " + server_port);


            new Thread(() -> {
                try {
                    while (true) {
                        ClientHandler chatClientHandler = new ClientHandler(srvSocket.accept());
                        addClient(chatClientHandler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            int seconds = 30;
            System.out.println("Enter \"quit\" if you want to quit!");
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while ((input = console.readLine()) != null) {
                if (input.equals("quit")) {
                    System.out.println("Beginning Shutdown!");
                    sendServerMessage("Server is shutting down in 30 seconds!");
                    for (int i = 0; i < 6; i++) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Shutting down in " + seconds + " seconds!");
                        seconds -= 5;
                    }
                    System.out.println("Shutting down now!");
                    sendServerMessage("Bye!");
                    for (ClientHandler connectedClient : connectedClients) {
                        //TODO doesn't quite work
                        connectedClient.disconnect();
                    }

                    srvSocket.close();
                } else if (input.equals("list")) {
                    System.out.print("Connected Clients: ");
                    for (ClientHandler client : connectedClients) {
                        System.out.print(client.getUsername() + "; ");
                    }
                    System.out.println();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static boolean addClient(ClientHandler clientHandler) {
        synchronized (connectedClients) {
            return connectedClients.add(clientHandler);
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        synchronized (connectedClients) {
            connectedClients.remove(clientHandler);
        }
    }

    public static void sendMessageToAll(ClientHandler sourceClient, String message) {
        if (!message.isEmpty()) {
            Collection<ClientHandler> clients;

            synchronized (connectedClients) {
                clients = new ArrayList<>(connectedClients);
            }

            Matcher m = Pattern.compile("(\\w+):(.+)").matcher(message);
            if (m.matches()) {
                for (ClientHandler clientHandler : clients) {
                    if (clientHandler.getUsername().matches(m.group(1))) {
                        sendPM("PM from " + sourceClient.getUsername() + ": " + m.group(2), clientHandler);
                    }
                }
            } else {
                for (ClientHandler clientHandler1 : clients) {
                    if (clientHandler1 != sourceClient) {
//                    clientHandler1.sendMessage("\r\n" + sourceClient.getUsername() + ": " + message, true);
                        clientHandler1.sendMessage(sourceClient.getUsername() + ": " + message, true);
                    }
                }
            }
        }
    }

    public static void sendJoinMessage(String message, ClientHandler source) {
        if (!message.isEmpty()) {
            Collection<ClientHandler> clients;

            synchronized (connectedClients) {
                clients = new ArrayList<>(connectedClients);
            }

            for (ClientHandler clientHandler : clients) {
                if (clientHandler != source) {
                    clientHandler.sendMessage(message, true);
                }
            }
        }
    }

    public static void sendServerMessage(String message) {
        if (!message.isEmpty()) {
            Collection<ClientHandler> clients;

            synchronized (connectedClients) {
                clients = new ArrayList<>(connectedClients);
            }


            for (ClientHandler clientHandler : clients) {
                clientHandler.sendMessage(message, true);
            }
        }
    }

    public static void sendPM(String message, ClientHandler dest) {
        if (!message.isEmpty()) {
            dest.sendMessage(message, true);
        }
    }

    public static Collection<String> getUsernames() {
        synchronized (connectedClients) {
            return connectedClients.stream().map(ClientHandler::getUsername).collect(Collectors.toList());
        }

    }
}
