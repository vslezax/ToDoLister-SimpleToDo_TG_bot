import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();                  //We moved this line out of the register method, to access it later
        botsApi.registerBot(bot);
        bot.sendText(450999307L, "*И снова я!*");  //The L just turns the Integer into a Long

        Thread notificationThread = new Thread(() -> {
            System.out.println("    Thread started.");
            while (true) {
                System.out.println("    Notification Module scanning...");
                bot.notificationSending();
                bot.expiredTasks();
                System.out.println("    Notification Module finished.");
                try {
                    Thread.sleep(60000); // 60 секунд
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        notificationThread.start();
    }
}
