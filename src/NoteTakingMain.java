import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.IOException;

public class NoteTakingMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(NoteTakingMain.class.getResource("main-window-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        scene.getStylesheets().add("styling.css");
        stage.getIcons().add(new Image("Resources/icon.png"));
        stage.setTitle("Notes");
        stage.setScene(scene);
        // Get the controller from the FXMLLoader
        Controller controller = fxmlLoader.getController();

        // Set the onCloseRequest event handler to call handleExit in the controller
        stage.setOnCloseRequest(event -> {
            controller.handleExit();
            event.consume(); // Consume the event to prevent immediate window closure

        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}