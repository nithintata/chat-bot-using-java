
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client{
    public static void main(String[] args) throws IOException {
        BotClient bot = new BotClient();
        bot.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int)(100*Math.random());
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Hello, there. I'm a bot. I understand the following commands: date, day, month, year, time, hour, minutes, seconds.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (message.contains(":")) {
                String[] arr = message.split(": ");
                String pattern = null;
                switch (arr[1]) {
                    case "date" : pattern = "d.MM.YYYY";break;
                    case "day" : pattern = "d";break;
                    case "month" : pattern = "MMMM";break;
                    case "year" : pattern = "YYYY";break;
                    case "time" : pattern ="H:mm:ss";break;
                    case "hour" : pattern = "H";break;
                    case "minutes" : pattern = "m";break;
                    case "seconds" : pattern = "s";break;
                }
                if (pattern != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    sendTextMessage("Information for " + arr[0] + ": " + sdf.format(Calendar.getInstance().getTime()));
                }
            }
        }
    }
}
