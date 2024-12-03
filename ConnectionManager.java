// ConnectionManager.java

import javafx.application.Platform;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class ConnectionManager {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private SecretKey secretKey;

    private String userName;
    private String friendName;
    private String serverToken;

    private P2PMessenger2 app;

    public ConnectionManager(P2PMessenger2 app) {
        this.app = app;
    }

    public void startServer(String userName, String serverToken) {
        this.userName = userName;
        this.serverToken = serverToken;

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                app.log("Server started. Waiting for connection...");

                socket = serverSocket.accept();
                app.log("Connected to: " + socket.getInetAddress().getHostAddress());

                setupStreams(socket);

                String receivedToken = in.readUTF();
                if (!serverToken.equals(receivedToken)) {
                    out.writeUTF("Invalid token. Connection refused.");
                    out.flush();
                    socket.close();
                    app.log("Connection refused: Invalid token.");
                    return;
                }

                out.writeUTF("Connected");
                out.flush();

                friendName = in.readUTF();

                out.writeUTF(userName);
                out.flush();

                secretKey = EncryptionUtils.deriveKey(serverToken);

                Platform.runLater(() -> {
                    app.onConnected();
                });

                app.log("Chatting with " + friendName);

                receiveMessages();
            } catch (IOException e) {
                app.log("Error starting server: " + e.getMessage());
            } catch (Exception e) {
                app.log("Encryption error: " + e.getMessage());
            }
        });
    }

    public void connectToFriend(String friendIp, String token, String userName) {
        this.userName = userName;

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                app.log("Connecting to " + friendIp + "...");

                socket = new Socket(friendIp, PORT);
                app.log("Connected to: " + socket.getInetAddress().getHostAddress());

                setupStreams(socket);

                out.writeUTF(token);
                out.flush();

                String response = in.readUTF();
                if (response == null || !response.equals("Connected")) {
                    app.log("Connection refused by the server.");
                    socket.close();
                    return;
                }

                out.writeUTF(userName);
                out.flush();

                friendName = in.readUTF();

                app.log("Authentication successful.");
                app.log("Chatting with " + friendName);

                secretKey = EncryptionUtils.deriveKey(token);

                Platform.runLater(() -> {
                    app.onConnected();
                });

                receiveMessages();
            } catch (IOException e) {
                app.log("Failed to connect to " + friendIp + ": " + e.getMessage());
            } catch (Exception e) {
                app.log("Encryption error: " + e.getMessage());
            }
        });
    }

    private void setupStreams(Socket socket) throws IOException {
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    private void receiveMessages() {
        try {
            while (true) {
                int messageType = in.readInt();
                if (messageType == 1) {
                    int length = in.readInt();
                    byte[] encryptedMessage = new byte[length];
                    in.readFully(encryptedMessage);
                    String decryptedMessage = EncryptionUtils.decrypt(encryptedMessage, secretKey);
                    Platform.runLater(() -> app.addMessageBubble(decryptedMessage, false));
                } else if (messageType == 2) {
                    receiveFile();
                }
            }
        } catch (IOException e) {
            app.log("Connection lost: " + e.getMessage());
            Platform.runLater(() -> {
                app.onDisconnected();
            });
        } catch (Exception e) {
            app.log("Decryption error: " + e.getMessage());
        }
    }

    public void sendMessage(String messageText) {
        try {
            messageText = MessageUtils.sanitizeInput(messageText);
            String fullMessage = userName + "|" + MessageUtils.getCurrentTime() + "|" + messageText;
            byte[] encryptedMessage = EncryptionUtils.encrypt(fullMessage, secretKey);
            out.writeInt(1);
            out.writeInt(encryptedMessage.length);
            out.write(encryptedMessage);
            out.flush();
            app.addMessageBubble(fullMessage, true);
        } catch (Exception e) {
            app.log("Encryption error: " + e.getMessage());
        }
    }

    public void sendAttachment(File file) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                byte[] encryptedFileBytes = EncryptionUtils.encryptBytes(fileBytes, secretKey);
                out.writeInt(2);
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

    private void receiveFile() {
        try {
            String fileName = in.readUTF();
            fileName = MessageUtils.sanitizeFileName(fileName);
            long length = in.readLong();
            if (length > P2PMessenger2.MAX_FILE_SIZE) {
                app.log("Received file exceeds maximum allowed size. Transfer aborted.");
                return;
            }
            byte[] encryptedFileBytes = new byte[(int) length];
            in.readFully(encryptedFileBytes);
            byte[] decryptedFileBytes = EncryptionUtils.decryptBytes(encryptedFileBytes, secretKey);
            Path downloadDir = new File("downloads").toPath();
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
            }
            File file = new File(downloadDir.toFile(), fileName);
            Files.write(file.toPath(), decryptedFileBytes);
            Platform.runLater(() -> app.addFileMessageBubble(file.getName(), "Received", file, false));
        } catch (IOException e) {
            app.log("Failed to receive file: " + e.getMessage());
        } catch (Exception e) {
            app.log("Decryption error: " + e.getMessage());
        }
    }

    public void closeConnections() throws IOException {
        if (socket != null)
            socket.close();
        if (serverSocket != null)
            serverSocket.close();
    }
}
