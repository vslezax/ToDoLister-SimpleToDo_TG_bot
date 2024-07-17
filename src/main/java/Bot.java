import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

enum Mode {
    waitingCommand,
    waitingPriority,
    waitingTitle,
    waitingTime,
    waitingTitleRemove
}

public class Bot extends TelegramLongPollingBot {
    private final Database database = new Database();
    private Mode mode = Mode.waitingCommand;
    Task newTask = new Task();
    int everydayNotificationHour = 20;
    int everydayNotificationMinute = 31;

    @Override
    public String getBotUsername() {
        return "ToDoLister";
    }

    @Override
    public String getBotToken() {
        return "7489859977:AAEo-47X1RwO5JCv8NAWBVj2LcJT3WVGpXw";
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        var id = user.getId();

        switch (mode) {
            case waitingCommand -> {
                if (msg.getText().equals("/addtask")) {
                    sendText(id, "Please, write priority of the task:");
                    mode = Mode.waitingPriority;
                } else if (msg.getText().equals("/removetask")) {
                    sendText(id, "Please, write title to remove:");
                    mode = Mode.waitingTitleRemove;
                } else if (msg.getText().equals("/showtasks")) {
                    showTasks(id);
                } else {
                    sendText(id, """
                            Please use commands below:
                            '/addtask' - adding tasks to your ToDo List
                            '/removetask' - removing tasks from you ToDo List
                            '/showtasks' - shows tasks from your ToDo List
                            """);
                }
            }
            case waitingPriority -> {
                newTask.priority = Integer.parseInt(msg.getText());
                sendText(id, "Please, write title of the task:");
                mode = Mode.waitingTitle;
            }
            case waitingTitle -> {
                newTask.title = msg.getText();
                sendText(id, "Please, write deadline of the task with patter 'yyyy-mm-dd hh:mm':");
                mode = Mode.waitingTime;
            }
            case waitingTime -> {
                if (!msg.getText().isEmpty()) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                        Date time = dateFormat.parse(msg.getText());
                        newTask.expiresTime = time;
                    } catch (ParseException e) {
                        sendText(id, "Invalid date format. Please use format: yyyy-mm-dd hh:mm");
                        return;
                    }
                }
                addTask(id, newTask);
                mode = Mode.waitingCommand;
            }
            case waitingTitleRemove -> {
                removeTask(id, msg.getText());
                mode = Mode.waitingCommand;
            }
        }

        System.out.println(user.getFirstName() + " wrote " + msg.getText());
    }

    public void addTask(Long id, Task task) {
        database.addTask(id, task);
        sendText(id, "Task added successfully.");
    }

    public void removeTask(Long id, String title) {
        if (database.removeTask(id, title)) sendText(id, "Task removed successfully.");
        else sendText(id, "Task has not been removed.");
    }

    public void showTasks(Long id) {
        String msg = database.getTasks(id);
        if (!msg.isEmpty()) sendText(id, msg);
        else sendText(id, "You have no tasks!");
    }

    public void expiredTasks(){
        Map<Long, String> map = database.returnExpiredTasks();

        for (Map.Entry<Long, String> entry : map.entrySet()) {
            sendText(entry.getKey(), entry.getValue() + " - is expired! Task will be removed automatically.");
            removeTask(entry.getKey(), entry.getValue());
        }
    }

    public void notificationSending(){
        Date current = new Date();

        if (current.getHours() == everydayNotificationHour && current.getMinutes() == everydayNotificationMinute){
            List<Long> ids = database.returnIds();

            for (Long id : ids) {
                sendText(id, "Automatic everyday notification sending:");
                showTasks(id);
            }
        }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        sm.setParseMode(ParseMode.MARKDOWN);
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
