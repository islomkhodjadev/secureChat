// UIUtils.java

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class UIUtils {
    public static void styleButton(Button button, String color) {
        button.setStyle("-fx-font-size: 14px; -fx-background-color: " + color + "; -fx-text-fill: white;");
        button.setEffect(new DropShadow(5, Color.GRAY));
    }

    public static void styleLabel(Label label) {
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 14px;");
    }
}
