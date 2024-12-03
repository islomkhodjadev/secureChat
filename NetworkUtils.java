// NetworkUtils.java

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class NetworkUtils {
    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unknown";
        }
    }

    public static void fetchPublicIp(Label publicIpLabel, String localIpAddress) {
        try {
            if (isLocalhost(localIpAddress)) {
                Platform.runLater(() -> publicIpLabel.setText("Running on localhost"));
                return;
            }
            URI uri = new URI("http://checkip.amazonaws.com/");
            URL url = uri.toURL();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String publicIp = br.readLine().trim();
            Platform.runLater(() -> publicIpLabel.setText("Your Public IP: " + publicIp));
        } catch (Exception e) {
            Platform.runLater(() -> publicIpLabel.setText("Failed to fetch Public IP"));
        }
    }

    public static boolean isLocalhost(String ipAddress) {
        return ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost");
    }
}
