import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ApplicationStart extends Application {
    public void start(Stage stage) {
        VBox root = new VBox();
        Scene s = new Scene(root, 300, 300, Color.BLACK);


        final Canvas canvas = new Canvas(250,250);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.BLUE);
        gc.fillRect(75,75,100,100);

        root.getChildren().add(new gui.MenuBar());
        root.getChildren().add(canvas);
        stage.setScene(s);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}
