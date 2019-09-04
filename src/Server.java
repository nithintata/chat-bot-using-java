
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    public static void sendBroadcastMessage(Message message) {
        for (Connection c : connectionMap.values()) {
            try {
                c.send(message);
            }
            catch (IOException e) {
                ConsoleHelper.writeMessage("message couldn't be sent");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Enter the port : ");
        int port  = ConsoleHelper.readInt();
        ServerSocket ss =new ServerSocket(port);
        ConsoleHelper.writeMessage("Server is running");
        try {
            while (true) {
                Socket s = ss.accept();
                Handler handler = new Handler(s);
                handler.start();
            }
        }
        catch (Exception e) {
            ConsoleHelper.writeMessage(e.getMessage());
            ss.close();
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("New connection established : " + socket.getRemoteSocketAddress());
            String userName;
            Connection conn;
            for (int i = 0; i < 1; i++) {
                try {
                    conn = new Connection(socket);
                } catch (Exception e) {
                    ConsoleHelper.writeMessage("error occurred while communicating with the remote address");
                    break;
                }
                try {
                    userName = serverHandshake(conn);
                } catch (Exception e) {
                    try {
                        conn.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }

                try {
                    sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                    notifyUsers(conn, userName);
                    serverMainLoop(conn, userName);
                } catch (Exception e) {

                }
                finally {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                    try {
                        conn.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            System.out.println("Connection closed: "+socket.getRemoteSocketAddress());
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message response = null;
            connection.send(new Message(MessageType.NAME_REQUEST));
            while(true) {
                response = connection.receive();
                if(response.getType() != MessageType.USER_NAME){
                    connection.send(new Message(MessageType.NAME_REQUEST));
                    continue;
                }
                String data = response.getData();

                if(data.length() == 0 || response == null){
                    connection.send(new Message(MessageType.NAME_REQUEST));
                    continue;
                }
                if(connectionMap.containsKey(data)){
                    connection.send(new Message(MessageType.NAME_REQUEST));
                    continue;
                }
                connectionMap.put(response.getData(),connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));

                return response.getData();
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (!entry.getKey().equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message receive = connection.receive();
                if (receive.getType() == MessageType.TEXT) {
                    Message textMessage = new Message(MessageType.TEXT, userName + ": " + receive.getData());
                    sendBroadcastMessage(textMessage);
                }
                else {
                    ConsoleHelper.writeMessage("Message is not in TEXT format");
                }
            }
        }

    }
}