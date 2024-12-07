// UIUtils.java

// Utility class for styling JavaFX UI components
import javafx.scene.control.Button; // For styling Button components
import javafx.scene.control.Label; // For styling Label components
import javafx.scene.effect.DropShadow; // For adding shadow effects
import javafx.scene.paint.Color; // For color definitions

public class UIUtils {

    /**
     * Styles a Button with the specified background color and text color.
     * Adds a shadow effect for visual enhancement.
     * 
     * @param button The Button to style.
     * @param color  The background color in a CSS-compatible string format (e.g., "#1ABC9C").
     */
    public static void styleButton(Button button, String color) {
        // Apply CSS styling for font size, background color, and text color
        button.setStyle("-fx-font-size: 14px; -fx-background-color: " + color + "; -fx-text-fill: white;");

        // Add a drop shadow effect with a 5-pixel radius and gray color
        button.setEffect(new DropShadow(5, Color.GRAY));
    }

    /**
     * Styles a Label with white text color and a font size of 14px.
     * 
     * @param label The Label to style.
     */
    public static void styleLabel(Label label) {
        // Set the text color to white
        label.setTextFill(Color.WHITE);

        // Apply CSS styling for font size
        label.setStyle("-fx-font-size: 14px;");
    }
}
