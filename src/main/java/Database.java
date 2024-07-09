import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:todo.db";

    private Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    private void createTables() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS tasks (" +
                "user_id INTEGER NOT NULL, " +
                "task_name TEXT NOT NULL, " +
                "task_time TEXT, " +
                "task_priority INTEGER, " +
                "PRIMARY KEY (user_id, task_name)" +
                ");";

        try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create table", e);
        }
    }

    public void addTask(Long userId, Task task) {
        String insertSQL = "INSERT INTO tasks (user_id, task_name, task_time, task_priority) VALUES (?, ?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setLong(1, userId);
            statement.setString(2, task.title);
            statement.setString(3, task.time);
            statement.setInt(4, task.priority);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add task", e);
        }
    }

    public String getTasks(Long userId) {
        List<Task> tasks = new ArrayList<>();
        String selectSQL = "SELECT task_name, task_time, task_priority FROM tasks WHERE user_id = ?;";
        StringBuilder output = new StringBuilder();

        try (PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String title = resultSet.getString("task_name");
                String time = resultSet.getString("task_time");
                int priority = resultSet.getInt("task_priority");
                Task task = new Task();
                task.title = title;
                task.time = time;
                task.priority = priority;
                tasks.add(task);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch tasks", e);
        }

        tasks.sort(Comparator.comparingInt(Task::getPriority).reversed());

        if (tasks.isEmpty()) return "";

        for (Task task : tasks) {
            output.append("\uD83D\uDCCC *(" + task.priority + ")*, _" + task.time + "_\n    " + task.title + "\n\n");
        }

        return output.toString();
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