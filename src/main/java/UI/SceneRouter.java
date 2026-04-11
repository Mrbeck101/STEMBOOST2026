package UI;

import javafx.stage.Stage;

public class SceneRouter {

    private final Stage stage;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    public void goToLogin() {
        stage.setScene(LoginView.create(this));
    }

    public void goToRegister() {
        stage.setScene(RegisterView.create(this));
    }

    public void goToDashboard() {
        stage.setTitle("Dashboard");
        // replace with your main app scene
    }
}
