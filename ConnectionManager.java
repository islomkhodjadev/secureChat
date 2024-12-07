// ConnectionManager.java

// Manages peer-to-peer connections, message sending, and file transfer
import javafx.application.Platform; // For safely updating the UI from background threads
import javax.crypto.SecretKey; // For encryption and decryption
import java.io.*; // For input/output streams
import java.net.ServerSocket; // For server connections
import java.net.Socket; // For client connections
import java.nio.file.Files; // For file handling
import java.nio.file.Path; // For file paths
import java.util.concurrent.Executors; // For managing background threads

public class ConnectionManager {
    private static final int PORT = 12345; // Fixed port for connections
    private ServerSocket serverSocket; // Server socket for listening to incoming connections
    private Socket socket; // Socket for client-server communication
    private DataOutputStream out; // Output stream for sending data
    private DataInputStream in; // Input stream for receiving data
    private SecretKey secretKey; // Encryption key for secure communication

    private String userName; // Local user's name
    private String friendName; // Connected friend's name
    private String serverToken; // Token for secure server authentication

    private P2PMessenger2 app; // Reference to the main app for UI updates

    // Constructor
    public ConnectionManager(P2PMessenger2 app) {
        this.app = app;
    }

    /**
     * Starts the server to listen for incoming connections.
     *
     * @param userName    The local user's name.
     * @param serverToken The token for authenticating the connection.
     */
    public void startServer(String userName, String serverToken) {
        this.userName = userName;
        this.serverToken = serverToken;

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT); // Start server on the specified port
                app.log("Server started. Waiting for connection...");

                socket = serverSocket.accept(); // Wait for a client to connect
                app.log("Connected to: " + socket.getInetAddress().getHostAddress());

                setupStreams(socket); // Initialize input and output streams

                String receivedToken = in.readUTF(); // Read the client's token
                if (!serverToken.equals(receivedToken)) { // Validate the token
                    out.writeUTF("Invalid token. Connection refused.");
                    out.flush();
                    socket.close(); // Close connection if token is invalid
                    app.log("Connection refused: Invalid token.");
                    return;
                }

                out.writeUTF("Connected"); // Acknowledge successful connection
                out.flush();

                friendName = in.readUTF(); // Receive friend's name
                out.writeUTF(userName); // Send local user's name
                out.flush();

                secretKey = EncryptionUtils.deriveKey(serverToken); // Derive encryption key from token

                Platform.runLater(() -> app.onConnected()); // Update UI to show connected state

                app.log("Chatting with " + friendName);

                receiveMessages(); // Start listening for messages
            } catch (IOException e) {
                app.log("Error starting server: " + e.getMessage());
            } catch (Exception e) {
                app.log("Encryption error: " + e.getMessage());
            }
        });
    }

    /**
     * Connects to a friend's server.
     *
     * @param friendIp The friend's IP address.
     * @param token    The token for authentication.
     * @param userName The local user's name.
     */
    public void connectToFriend(String friendIp, String token, String userName) {
        this.userName = userName;

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                app.log("Connecting to " + friendIp + "...");

                socket = new Socket(friendIp, PORT); // Connect to the friend's server
                app.log("Connected to: " + socket.getInetAddress().getHostAddress());

                setupStreams(socket); // Initialize input and output streams

                out.writeUTF(token); // Send the token
                out.flush();

                String response = in.readUTF(); // Read server's response
                if (response == null || !response.equals("Connected")) {
                    app.log("Connection refused by the server.");
                    socket.close();
                    return;
                }

                out.writeUTF(userName); // Send local user's name
                out.flush();

                friendName = in.readUTF(); // Receive friend's name

                app.log("Authentication successful.");
                app.log("Chatting with " + friendName);

                secretKey = EncryptionUtils.deriveKey(token); // Derive encryption key from token

                Platform.runLater(() -> app.onConnected()); // Update UI to show connected state

                receiveMessages(); // Start listening for messages
            } catch (IOException e) {
                app.log("Failed to connect to " + friendIp + ": " + e.getMessage());
            } catch (Exception e) {
                app.log("Encryption error: " + e.getMessage());
            }
        });
    }

    /**
     * Sets up input and output streams for the socket.
     *
     * @param socket The socket for communication.
     */
    private void setupStreams(Socket socket) throws IOException {
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    /**
     * Continuously listens for incoming messages or file transfers.
     */
    private void receiveMessages() {
        try {
            while (true) {
                int messageType = in.readInt(); // Read message type (1 = text, 2 = file)
                if (messageType == 1) { // Text message
                    int length = in.readInt();
                    byte[] encryptedMessage = new byte[length];
                    in.readFully(encryptedMessage);
                    String decryptedMessage = EncryptionUtils.decrypt(encryptedMessage, secretKey);
                    Platform.runLater(() -> app.addMessageBubble(decryptedMessage, false));
                } else if (messageType == 2) { // File transfer
                    receiveFile();
                }
            }
        } catch (IOException e) {
            app.log("Connection lost: " + e.getMessage());
            Platform.runLater(() -> app.onDisconnected());
        } catch (Exception e) {
            app.log("Decryption error: " + e.getMessage());
        }
    }

    /**
     * Sends a text message to the connected peer.
     *
     * @param messageText The message to send.
     */
    public void sendMessage(String messageText) {
        try {
            messageText = MessageUtils.sanitizeInput(messageText); // Sanitize input
            String fullMessage = userName + "|" + MessageUtils.getCurrentTime() + "|" + messageText;
            byte[] encryptedMessage = EncryptionUtils.encrypt(fullMessage, secretKey); // Encrypt the message
            out.writeInt(1); // Message type: 1 (text)
            out.writeInt(encryptedMessage.length);
            out.write(encryptedMessage);
            out.flush();
            app.addMessageBubble(fullMessage, true); // Display the message locally
        } catch (Exception e) {
            app.log("Encryption error: " + e.getMessage());
        }
    }

    /**
     * Sends a file to the connected peer.
     *
     * @param file The file to send.
     */
    public void sendAttachment(File file) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                byte[] encryptedFileBytes = EncryptionUtils.encryptBytes(fileBytes, secretKey); // Encrypt the file
                out.writeInt(2); // Message type: 2 (file)
                out.writeUTF(file.getName());
                out.writeLong(encryptedFileBytes.length);
                out.write(encryptedFileBytes);
                out.flush();
                Platform.runLater(() -> app.addFileMessageBubble(file.getName(), "Sending", file, true));
            } catch (IOException e) {
                app.log("Failed to send file: " + e.getMessage());
            } catch (Exception e) {
                app.log("Encryption error: " + e.getMessage());
            }
        });
    }

    /**
     * Receives a file from the connected peer.
     */
    private void receiveFile() {
        try {
            String fileName = in.readUTF();
            fileName = MessageUtils.sanitizeFileName(fileName); // Sanitize file name
            long length = in.readLong();
            if (length > P2PMessenger2.MAX_FILE_SIZE) {
                app.log("Received file exceeds maximum allowed size. Transfer aborted.");
                return;
            }
            byte[] encryptedFileBytes = new byte[(int) length];
            in.readFully(encryptedFileBytes);
            byte[] decryptedFileBytes = EncryptionUtils.decryptBytes(encryptedFileBytes, secretKey); // Decrypt the file
            Path downloadDir = new File("downloads").toPath(); // Save file in "downloads" directory
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir); // Create directory if not exists
            }
            File file = new File(downloadDir.toFile(), fileName);
            Files.write(file.toPath(), decryptedFileBytes); // Write file to disk
            Platform.runLater(() -> app.addFileMessageBubble(file.getName(), "Received", file, false));
        } catch (IOException e) {
            app.log("Failed to receive file: " + e.getMessage());
        } catch (Exception e) {
            app.log("Decryption error: " + e.getMessage());
        }
    }

    /**
     * Closes all active connections and sockets.
     */
    public void closeConnections() throws IOException {
        if (socket != null)
            socket.close();
        if (serverSocket != null)
            serverSocket.close();
    }
}
