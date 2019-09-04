
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected  Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.run();
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Enter the server address (\"localhost\" if you are running the server on this machine):");
        return ConsoleHelper.readString();
    }
    protected int getServerPort() {
        ConsoleHelper.writeMessage("Enter the server port:");
        return ConsoleHelper.readInt();
    }
    protected String getUserName() {
        ConsoleHelper.writeMessage("Enter your username:");
        return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole() {
        return true;
    }
    protected SocketThread getSocketThread() {
        return new SocketThread();
    }
    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            e.printStackTrace();
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Something happened");
            }
        }

        if (clientConnected)
            ConsoleHelper.writeMessage("Connection established. To exit, enter 'exit'.");
        else
            ConsoleHelper.writeMessage("An error occurred while working with the client.");
        while (clientConnected) {
            String s = ConsoleHelper.readString();
            if (s.equals("exit"))
                break;
            if (shouldSendTextFromConsole())
                sendTextMessage(s);
        }
    }


    public class SocketThread extends Thread{
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " has joined the chat.");
        }
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " has left the chat.");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized(Client.this) {
                Client.this.notify();
            }
        }

        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(serverAddress, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            }
            catch (Exception e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    String userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                }
                else if(message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    break;
                }
                else
                    throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message received = connection.receive();
                if (received.getType() == MessageType.TEXT)
                    processIncomingMessage(received.getData());
                else if(received.getType() == MessageType.USER_ADDED)
                    informAboutAddingNewUser(received.getData());
                else if (received.getType() == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(received.getData());
                else
                    throw new IOException("Unexpected MessageType");
            }
        }


    }
}
