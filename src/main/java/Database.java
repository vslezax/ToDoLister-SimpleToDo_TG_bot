import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:todo.db";

    private Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            clearDatabase();
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    private void clearDatabase() {
        String dropTableSQL = "DROP TABLE IF EXISTS tasks;";
        try (PreparedStatement statement = connection.prepareStatement(dropTableSQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to clear database", e);
        }
    }

    private void createTables() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS tasks (" +
                "user_id INTEGER NOT NULL, " +
                "task_name TEXT NOT NULL, " +
                "task_priority INTEGER, " +
                "task_time TIMESTAMP, " +
                "PRIMARY KEY (user_id, task_name)" +
                ");";

        try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create table", e);
        }
    }

    public void addTask(Long userId, Task task) {
        String insertSQL = "INSERT INTO tasks (user_id, task_name, task_priority, task_time) VALUES (?, ?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setLong(1, userId);
            statement.setString(2, task.title);
            statement.setInt(3, task.priority);
            if (task.expiresTime != null) {
                statement.setTimestamp(4, new Timestamp(task.expiresTime.getTime()));
            } else {
                statement.setTimestamp(4, null);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add task", e);
        }
    }

    public String getTasks(Long userId) {
        List<Task> tasks = new ArrayList<>();
        String selectSQL = "SELECT task_name, task_priority, task_time FROM tasks WHERE user_id = ?;";
        StringBuilder output = new StringBuilder();

        try (PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String title = resultSet.getString("task_name");
                int priority = resultSet.getInt("task_priority");
                Timestamp notificationTimestamp = resultSet.getTimestamp("task_time");
                Date notification = notificationTimestamp != null ? new Date(notificationTimestamp.getTime()) : null;

                Task task = new Task();
                task.title = title;
                task.priority = priority;
                task.expiresTime = notification;
                tasks.add(task);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch tasks", e);
        }

        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());

        if (tasks.isEmpty()) return "";

        for (Task task : tasks) {
            output.append("\uD83D\uDCCC *(" + task.priority + ")*, _" + task.expiresTime + "_");
            output.append("\n    " + task.title + "\n\n");
        }

        return output.toString();
    }

    public Map<Long, String> returnExpiredTasks(){
        Vector<Task> tasks = new Vector<>();
        Vector<Long> ids = new Vector<>();
        String selectSQL = "SELECT user_id, task_name, task_priority, task_time FROM tasks;";
        Map<Long, String> expiredTasks = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ids.add(Long.parseLong(resultSet.getString("user_id")));
                String title = resultSet.getString("task_name");
                int priority = resultSet.getInt("task_priority");
                Timestamp notificationTimestamp = resultSet.getTimestamp("task_time");
                Date notification = notificationTimestamp != null ? new Date(notificationTimestamp.getTime()) : null;

                Task task = new Task();
                task.title = title;
                task.priority = priority;
                task.expiresTime = notification;
                tasks.add(task);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch tasks", e);
        }

        Date currentTime = new Date();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.elementAt(i);
            long id = ids.elementAt(i);

            if (task.expiresTime != null && task.expiresTime.before(currentTime)) {
                expiredTasks.put(id, task.title);
            }
        }

        return expiredTasks;
    }

    public List<Long> returnIds(){
        List<Long> ids = new ArrayList<>();
        String selectSQL = "SELECT DISTINCT user_id FROM tasks;";

        try (PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ids.add(Long.parseLong(resultSet.getString("user_id")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch tasks", e);
        }

        return ids;
    }

    public boolean removeTask(Long userId, String taskName) {
        String deleteSQL = "DELETE FROM tasks WHERE user_id = ? AND task_name = ?;";
        try (PreparedStatement statement = connection.prepareStatement(deleteSQL)) {
            statement.setLong(1, userId);
            statement.setString(2, taskName);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to remove task", e);
        }
    }
}
