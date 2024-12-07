// NetworkUtils.java

// Utility class for handling network-related operations
import javafx.application.Platform; // For updating UI from non-UI threads
import javafx.scene.control.Label; // For updating a JavaFX Label

import java.io.BufferedReader; // For reading data from an InputStream
import java.io.InputStreamReader; // For converting InputStream to Reader
import java.net.*; // For networking utilities such as InetAddress and URL

public class NetworkUtils {

    /**
     * Gets the local IP address of the current machine.
     * 
     * @return The local IP address as a String, or "Unknown" if an error occurs.
     */
    public static String getLocalIp() {
        try {
            // Fetch the local IP address of the machine
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // Return "Unknown" if an error occurs (e.g., no network connection)
            return "Unknown";
        }
    }

    /**
     * Fetches the public IP address of the current machine and updates the provided Label.
     * Uses the "http://checkip.amazonaws.com/" service to get the public IP.
     * 
     * @param publicIpLabel   The Label to update with the public IP.
     * @param localIpAddress  The local IP address of the machine.
     */
    public static void fetchPublicIp(Label publicIpLabel, String localIpAddress) {
        try {
            // Check if the machine is running on localhost
            if (isLocalhost(localIpAddress)) {
                // Update the UI to indicate localhost mode
                Platform.runLater(() -> publicIpLabel.setText("Running on localhost"));
                return;
            }

            // Create a URI for the public IP service
            URI uri = new URI("http://checkip.amazonaws.com/");
            URL url = uri.toURL();

            // Open a stream to the URL and read the public IP
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String publicIp = br.readLine().trim(); // Read and trim the IP address

            // Update the Label with the fetched public IP
            Platform.runLater(() -> publicIpLabel.setText("Your Public IP: " + publicIp));
        } catch (Exception e) {
            // Handle any exceptions (e.g., network issues) and update the Label
            Platform.runLater(() -> publicIpLabel.setText("Failed to fetch Public IP"));
        }
    }

    /**
     * Checks if the provided IP address corresponds to localhost.
     * 
     * @param ipAddress The IP address to check.
     * @return True if the IP address is "127.0.0.1" or "localhost", false otherwise.
     */
    public static boolean isLocalhost(String ipAddress) {
        // Return true if the IP address is localhost or loopback address
        return ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost");
    }
}
