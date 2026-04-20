package main.demo;
import UI.*;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            SceneRouter router = new SceneRouter(stage);

            stage.setTitle("STEMBOOST - Learning Management System");
            stage.setScene(LoginView.create(router));

            // Set window size to 80% of screen size, centered
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double width = 1280; //screenBounds.getWidth() * 0.8;
            double height = 720; //screenBounds.getHeight() * 0.8;

            stage.setWidth(width);
            stage.setHeight(height);
            stage.centerOnScreen();

            // Set minimum window size
            stage.setMinWidth(800);
            stage.setMinHeight(600);

            stage.show();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
