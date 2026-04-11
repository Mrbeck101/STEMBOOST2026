package main.demo;
import UI.*;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        SceneRouter router = new SceneRouter(stage);

        stage.setTitle("My App");
        stage.setScene(LoginView.create(router));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
