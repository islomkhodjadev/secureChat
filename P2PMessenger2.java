// P2PMessenger2.java

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
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private VBox chatWindow;
    private TextField inputField;
    private Button sendButton;
    private Button startServerButton;
    private Button connectButton;
    private TextField tokenField;
    private Label publicIpLabel;
    private Label localIpLabel;

    private String userName;
    private String friendName;
    private String serverToken;

    private FileChooser fileChooser;
    private Label statusLabel;
    private boolean isConnected = false;

    private String localIpAddress;

    private ConnectionManager connectionManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Secure P2P Messenger");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #2C3E50;");

        HBox topSection = new HBox(15);
        topSection.setAlignment(Pos.CENTER_LEFT);

        localIpAddress = NetworkUtils.getLocalIp();
        localIpLabel = new Label("Your Local IP: " + localIpAddress);
        publicIpLabel = new Label("Fetching your Public IP...");

        UIUtils.styleLabel(localIpLabel);
        UIUtils.styleLabel(publicIpLabel);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        topSection.getChildren().addAll(localIpLabel, publicIpLabel, nameField);

        chatWindow = new VBox(10);
        chatWindow.setPadding(new Insets(10));
        chatWindow.setStyle("-fx-background-color: #34495E;");
        ScrollPane scrollPane = new ScrollPane(chatWindow);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #34495E; -fx-border-color: transparent;");

        HBox bottomSection = new HBox(10);
        bottomSection.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.setPromptText("Type your message here");
        inputField.setDisable(true);
        inputField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        sendButton = new Button("Send");
        sendButton.setDisable(true);
        UIUtils.styleButton(sendButton, "#1ABC9C");

        Button emojiButton = new Button("\uD83D\uDE03");
        UIUtils.styleButton(emojiButton, "#F1C40F");

        Button attachButton = new Button("\uD83D\uDCCE");
        UIUtils.styleButton(attachButton, "#E67E22");

        bottomSection.getChildren().addAll(emojiButton, attachButton, inputField, sendButton);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox connectionBox = new HBox(10);
        connectionBox.setAlignment(Pos.CENTER_LEFT);

        TextField friendIpField = new TextField();
        friendIpField.setPromptText("Enter friend's IP address");
        friendIpField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        tokenField = new TextField();
        tokenField.setPromptText("Enter connection token");
        tokenField.setStyle("-fx-font-size: 14px; -fx-background-color: #ECF0F1;");

        startServerButton = new Button("Start Server");
        UIUtils.styleButton(startServerButton, "#27AE60");

        connectButton = new Button("Connect");
        UIUtils.styleButton(connectButton, "#2980B9");

        statusLabel = new Label("Not connected");
        UIUtils.styleLabel(statusLabel);

        connectionBox.getChildren().addAll(startServerButton, friendIpField, connectButton, tokenField, statusLabel);

        VBox mainLayout = new VBox(10, topSection, connectionBox, scrollPane, bottomSection);
        root.setCenter(mainLayout);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("All Files", "*.*"));

        if (!NetworkUtils.isLocalhost(localIpAddress)) {
            Executors.newSingleThreadExecutor().submit(() -> NetworkUtils.fetchPublicIp(publicIpLabel, localIpAddress));
        } else {
            publicIpLabel.setText("Running on localhost");
        }

        connectionManager = new ConnectionManager(this);

        startServerButton.setOnAction(e -> {
            userName = nameField.getText().trim();
            if (userName.isEmpty()) {
                alert("Name Required", "Please enter your name before starting the server.");
                return;
            }
            serverToken = MessageUtils.generateSecureToken();
            log("Server token: " + serverToken);

            Platform.runLater(() -> {
                tokenField.setText(serverToken);
                tokenField.setEditable(false);
            });

            connectionManager.startServer(userName, serverToken);
        });

        connectButton.setOnAction(e -> {
            userName = nameField.getText().trim();
            if (userName.isEmpty()) {
                alert("Name Required", "Please enter your name before connecting.");
                return;
            }
            String friendIp = friendIpField.getText().trim();
            String token = tokenField.getText().trim();
            if (friendIp.isEmpty() || token.isEmpty()) {
                alert("IP or Token Required", "Please provide both IP address and token.");
                return;
            }
            tokenField.setEditable(true);
            connectionManager.connectToFriend(friendIp, token, userName);
        });

        sendButton.setOnAction(e -> {
            String messageText = inputField.getText();
            if (!messageText.isEmpty()) {
                connectionManager.sendMessage(messageText);
                inputField.clear();
            }
        });
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String messageText = inputField.getText();
                if (!messageText.isEmpty()) {
                    connectionManager.sendMessage(messageText);
                    inputField.clear();
                }
            }
        });

        emojiButton.setOnAction(e -> showEmojiPicker());
        attachButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                if (file.length() > MAX_FILE_SIZE) {
                    alert("File Too Large", "The selected file exceeds the maximum allowed size of 10 MB.");
                    return;
                }
                connectionManager.sendAttachment(file);
            }
        });
    }

    private void showEmojiPicker() {
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = { "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE09", "\uD83D\uDE0D", "\uD83D\uDE12",
                "\uD83D\uDE14", "\uD83D\uDE22", "\uD83D\uDE2D" };
        for (String emoji : emojis) {
            MenuItem item = new MenuItem(emoji);
            item.setOnAction(e -> inputField.appendText(emoji));
            emojiMenu.getItems().add(item);
        }
        emojiMenu.show(inputField, Side.TOP, 0, 0);
    }

    public void log(String message) {
        Platform.runLater(() -> addInfoBubble("[INFO] " + message + " (" + MessageUtils.getCurrentTime() + ")"));
    }

    private void alert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void addMessageBubble(String fullMessage, boolean isOwnMessage) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        String[] parts = fullMessage.split("\\|", 3);
        if (parts.length < 3) {
            log("Invalid message format.");
            return;
        }
        String senderName = parts[0];
        String time = parts[1];
        String messageContent = parts[2];

        Text nameText = new Text(senderName + ": ");
        nameText.setFill(Color.LIGHTGRAY);
        nameText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

        Text timeText = new Text("[" + time + "]\n");
        timeText.setFill(Color.LIGHTGRAY);
        timeText.setFont(Font.font("Verdana", FontPosture.ITALIC, 10));

        Text messageText = new Text(messageContent);
        messageText.setFill(Color.WHITE);
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        TextFlow textFlow = new TextFlow(nameText, timeText, messageText);
        textFlow.setPadding(new Insets(10));
        textFlow.setMaxWidth(400);
        textFlow.setStyle(
                "-fx-background-color: " + (isOwnMessage ? "#1ABC9C" : "#3498DB") + "; -fx-background-radius: 10;");

        messageBox.getChildren().add(textFlow);
        chatWindow.getChildren().add(messageBox);
    }

    public void addFileMessageBubble(String fileName, String fileType, File file, boolean isOwnMessage) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox contentBox = new VBox(5);
        contentBox.setPadding(new Insets(10));
        contentBox.setMaxWidth(400);
        contentBox.setStyle(
                "-fx-background-color: " + (isOwnMessage ? "#1ABC9C" : "#3498DB") + "; -fx-background-radius: 10;");

        Text nameText = new Text((isOwnMessage ? userName : friendName) + ": ");
        nameText.setFill(Color.LIGHTGRAY);
        nameText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

        Text timeText = new Text("[" + MessageUtils.getCurrentTime() + "]\n");
        timeText.setFill(Color.LIGHTGRAY);
        timeText.setFont(Font.font("Verdana", FontPosture.ITALIC, 10));

        Text messageText = new Text(fileType + " File: " + fileName);
        messageText.setFill(Color.WHITE);
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

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
                    desktop.open(file);
                }
            } catch (IOException ex) {
                alert("Error", "Unable to open file.");
            }
        });

        contentBox.getChildren().addAll(nameText, timeText, messageText, openButton);
        messageBox.getChildren().add(contentBox);
        chatWindow.getChildren().add(messageBox);
    }

    private void addInfoBubble(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);

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

    public void onConnected() {
        inputField.setDisable(false);
        sendButton.setDisable(false);
        statusLabel.setText("Connected");
        isConnected = true;
    }

    public void onDisconnected() {
        statusLabel.setText("Not connected");
        isConnected = false;
        inputField.setDisable(true);
        sendButton.setDisable(true);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        connectionManager.closeConnections();
    }
}