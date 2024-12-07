// P2PMessenger2.java

// Required imports for JavaFX, file handling, and threading
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Side;
import javafx.stage.FileChooser.ExtensionFilter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

public class P2PMessenger2 extends Application {
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // Maximum allowed file size (10 MB)

    // UI components
    private VBox chatWindow; // Chat display area
    private TextField inputField; // Text field for typing messages
    private Button sendButton; // Button to send messages
    private Button startServerButton; // Button to start the server
    private Button connectButton; // Button to connect to a peer
    private TextField tokenField; // Field to display or enter a connection token
    private Label publicIpLabel; // Label to show the public IP address
    private Label localIpLabel; // Label to show the local IP address

    // Application state variables
    private String userName; // Name of the user
    private String friendName; // Name of the connected friend
    private String serverToken; // Server token for secure communication

    private FileChooser fileChooser; // File chooser dialog for sending attachments
    private Label statusLabel; // Label to display connection status
    private boolean isConnected = false; // Flag indicating connection status

    private String localIpAddress; // Local IP address of the user

    private ConnectionManager connectionManager; // Manages server and client connections

    public static void main(String[] args) {
        launch(args); // Launch JavaFX application
    }

    @Override
    public void start(Stage primaryStage) {
        // Set up the primary stage title
        primaryStage.setTitle("Secure P2P Messenger");

        // Create the root layout container
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10)); // Add padding around the root
        root.setStyle("-fx-background-color: #2C3E50;"); // Set background color

        // Top section for IP display and name input
        HBox topSection = new HBox(15);
        topSection.setAlignment(Pos.CENTER_LEFT); // Align contents to the left

        // Fetch local IP address
        localIpAddress = NetworkUtils.getLocalIp();
        localIpLabel = new Label("Your Local IP: " + localIpAddress); // Show local IP
        publicIpLabel = new Label("Fetching your Public IP..."); // Placeholder for public IP

        // Style the labels
        UIUtils.styleLabel(localIpLabel);
        UIUtils.styleLabel(publicIpLabel);

        // Input field for entering the user's name
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name"); // Placeholder text
        nameField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        // Add components to the top section
        topSection.getChildren().addAll(localIpLabel, publicIpLabel, nameField);

        // Chat window with a scrollable pane
        chatWindow = new VBox(10);
        chatWindow.setPadding(new Insets(10)); // Padding for chat messages
        chatWindow.setStyle("-fx-background-color: #34495E;"); // Background color for chat area
        ScrollPane scrollPane = new ScrollPane(chatWindow);
        scrollPane.setFitToWidth(true); // Allow chat messages to expand horizontally
        scrollPane.setStyle("-fx-background: #34495E; -fx-border-color: transparent;");

        // Bottom section for message input and actions
        HBox bottomSection = new HBox(10);
        bottomSection.setAlignment(Pos.CENTER); // Center-align the contents

        inputField = new TextField();
        inputField.setPromptText("Type your message here"); // Placeholder text
        inputField.setDisable(true); // Initially disabled until connected
        inputField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        sendButton = new Button("Send");
        sendButton.setDisable(true); // Initially disabled
        UIUtils.styleButton(sendButton, "#1ABC9C"); // Style the send button

        // Emoji and file attach buttons
        Button emojiButton = new Button("\uD83D\uDE03");
        UIUtils.styleButton(emojiButton, "#F1C40F");

        Button attachButton = new Button("\uD83D\uDCCE");
        UIUtils.styleButton(attachButton, "#E67E22");

        // Add components to the bottom section
        bottomSection.getChildren().addAll(emojiButton, attachButton, inputField, sendButton);
        HBox.setHgrow(inputField, Priority.ALWAYS); // Allow the input field to expand

        // Connection box for starting server and connecting to peers
        HBox connectionBox = new HBox(10);
        connectionBox.setAlignment(Pos.CENTER_LEFT); // Align contents to the left

        // Input fields for friend's IP and connection token
        TextField friendIpField = new TextField();
        friendIpField.setPromptText("Enter friend's IP address");
        friendIpField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        tokenField = new TextField();
        tokenField.setPromptText("Enter connection token");
        tokenField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        // Buttons for server and peer connection
        startServerButton = new Button("Start Server");
        UIUtils.styleButton(startServerButton, "#27AE60");

        connectButton = new Button("Connect");
        UIUtils.styleButton(connectButton, "#2980B9");

        // Label for displaying connection status
        statusLabel = new Label("Not connected");
        UIUtils.styleLabel(statusLabel);

        // Add components to the connection box
        connectionBox.getChildren().addAll(startServerButton, friendIpField, connectButton, tokenField, statusLabel);

        // Main layout containing all sections
        VBox mainLayout = new VBox(10, topSection, connectionBox, scrollPane, bottomSection);
        root.setCenter(mainLayout);

        // Create and set the scene
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        // Initialize file chooser for sending attachments
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("All Files", "*.*"));

        // Fetch public IP in a separate thread
        if (!NetworkUtils.isLocalhost(localIpAddress)) {
            Executors.newSingleThreadExecutor().submit(() -> NetworkUtils.fetchPublicIp(publicIpLabel, localIpAddress));
        } else {
            publicIpLabel.setText("Running on localhost"); // Use localhost as fallback
        }

        // Initialize connection manager
        connectionManager = new ConnectionManager(this);

        // Start server button handler
        startServerButton.setOnAction(e -> {
            userName = nameField.getText().trim();
            if (userName.isEmpty()) {
                alert("Name Required", "Please enter your name before starting the server.");
                return;
            }
            serverToken = MessageUtils.generateSecureToken(); // Generate secure token for the server
            log("Server token: " + serverToken); // Log the server token

            Platform.runLater(() -> {
                tokenField.setText(serverToken); // Display the token
                tokenField.setEditable(false); // Make the token field non-editable
            });

            connectionManager.startServer(userName, serverToken); // Start the server
        });

        // Connect button handler
        connectButton.setOnAction(e -> {
            userName = nameField.getText().trim();
            if (userName.isEmpty()) {
                alert("Name Required", "Please enter your name before connecting.");
                return;
            }
            String friendIp = friendIpField.getText().trim(); // Get friend's IP
            String token = tokenField.getText().trim(); // Get connection token
            if (friendIp.isEmpty() || token.isEmpty()) {
                alert("IP or Token Required", "Please provide both IP address and token.");
                return;
            }
            tokenField.setEditable(true); // Allow token editing for re-connection
            connectionManager.connectToFriend(friendIp, token, userName); // Connect to the friend
        });

        // Send message button handler
        sendButton.setOnAction(e -> {
            String messageText = inputField.getText();
            if (!messageText.isEmpty()) {
                connectionManager.sendMessage(messageText); // Send the message
                inputField.clear(); // Clear the input field after sending
            }
        });

        // Keyboard shortcut (Enter key) for sending messages
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String messageText = inputField.getText();
                if (!messageText.isEmpty()) {
                    connectionManager.sendMessage(messageText);
                    inputField.clear();
                }
            }
        });

        // Emoji picker button handler
        emojiButton.setOnAction(e -> showEmojiPicker());

        // File attachment button handler
        attachButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(null); // Open file chooser dialog
            if (file != null) {
                if (file.length() > MAX_FILE_SIZE) { // Check if file exceeds size limit
                    alert("File Too Large", "The selected file exceeds the maximum allowed size of 10 MB.");
                    return;
                }
                connectionManager.sendAttachment(file); // Send the selected file
            }
        });
    }

    // Show emoji picker
    private void showEmojiPicker() {
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = { "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE09", "\uD83D\uDE0D", "\uD83D\uDE12",
                "\uD83D\uDE14", "\uD83D\uDE22", "\uD83D\uDE2D" }; // List of emojis
        for (String emoji : emojis) {
            MenuItem item = new MenuItem(emoji);
            item.setOnAction(e -> inputField.appendText(emoji)); // Add emoji to input field
            emojiMenu.getItems().add(item);
        }
        emojiMenu.show(inputField, Side.TOP, 0, 0); // Show menu above the input field
    }

    // Log information messages
    public void log(String message) {
        Platform.runLater(() -> addInfoBubble("[INFO] " + message + " (" + MessageUtils.getCurrentTime() + ")"));
    }

    // Show an alert dialog with a title and message
    private void alert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Add a message bubble to the chat window
    public void addMessageBubble(String fullMessage, boolean isOwnMessage) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // Align based on sender

        // Parse the message format: sender|time|message
        String[] parts = fullMessage.split("\\|", 3);
        if (parts.length < 3) {
            log("Invalid message format.");
            return;
        }
        String senderName = parts[0];
        String time = parts[1];
        String messageContent = parts[2];

        // Create styled message components
        Text nameText = new Text(senderName + ": ");
        nameText.setFill(Color.LIGHTGRAY);
        nameText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

        Text timeText = new Text("[" + time + "]\n");
        timeText.setFill(Color.LIGHTGRAY);
        timeText.setFont(Font.font("Verdana", FontPosture.ITALIC, 10));

        Text messageText = new Text(messageContent);
        messageText.setFill(Color.WHITE);
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Combine components into a text flow
        TextFlow textFlow = new TextFlow(nameText, timeText, messageText);
        textFlow.setPadding(new Insets(10));
        textFlow.setMaxWidth(400);
        textFlow.setStyle(
                "-fx-background-color: " + (isOwnMessage ? "#1ABC9C" : "#3498DB") + "; -fx-background-radius: 10;");

        messageBox.getChildren().add(textFlow); // Add the text flow to the message box
        chatWindow.getChildren().add(messageBox); // Add the message box to the chat window
    }

    // Add a file message bubble with an "Open" button
    public void addFileMessageBubble(String fileName, String fileType, File file, boolean isOwnMessage) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // Align based on sender

        VBox contentBox = new VBox(5); // Box to hold file details and open button
        contentBox.setPadding(new Insets(10));
        contentBox.setMaxWidth(400);
        contentBox.setStyle(
                "-fx-background-color: " + (isOwnMessage ? "#1ABC9C" : "#3498DB") + "; -fx-background-radius: 10;");

        // File details
        Text nameText = new Text((isOwnMessage ? userName : friendName) + ": ");
        nameText.setFill(Color.LIGHTGRAY);
        nameText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

        Text timeText = new Text("[" + MessageUtils.getCurrentTime() + "]\n");
        timeText.setFill(Color.LIGHTGRAY);
        timeText.setFont(Font.font("Verdana", FontPosture.ITALIC, 10));

        Text messageText = new Text(fileType + " File: " + fileName);
        messageText.setFill(Color.WHITE);
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Button to open the file
        Button openButton = new Button("Open");
        UIUtils.styleButton(openButton, "#E74C3C");
        openButton.setOnAction(e -> {
            try {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Open File");
                confirmation.setHeaderText("Are you sure you want to open this file?");
                confirmation.setContentText(file.getName());
                if (confirmation.showAndWait().get() == ButtonType.OK) {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(file); // Open the file with the default application
                }
            } catch (IOException ex) {
                alert("Error", "Unable to open file.");
            }
        });

        // Add components to the content box and message box
        contentBox.getChildren().addAll(nameText, timeText, messageText, openButton);
        messageBox.getChildren().add(contentBox);
        chatWindow.getChildren().add(messageBox);
    }

    // Add an informational message bubble to the chat window
    private void addInfoBubble(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER); // Center-align info messages

        Text text = new Text(message);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Verdana", FontWeight.NORMAL, 12));
        TextFlow textFlow = new TextFlow(text);
        textFlow.setPadding(new Insets(10));
        textFlow.setMaxWidth(400);
        textFlow.setStyle("-fx-background-color: #95A5A6; -fx-background-radius: 10;");

        messageBox.getChildren().add(textFlow);
        chatWindow.getChildren().add(messageBox);
    }

    // Update UI when connected
    public void onConnected() {
        inputField.setDisable(false); // Enable the input field
        sendButton.setDisable(false); // Enable the send button
        statusLabel.setText("Connected"); // Update status
        isConnected = true; // Set connection flag
    }

    // Update UI when disconnected
    public void onDisconnected() {
        statusLabel.setText("Not connected"); // Update status
        isConnected = false; // Reset connection flag
        inputField.setDisable(true); // Disable the input field
        sendButton.setDisable(true); // Disable the send button
    }

    @Override
    public void stop() throws Exception {
        super.stop(); // Call parent stop method
        connectionManager.closeConnections(); // Clean up connections
    }
}
