package hangman;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerFx extends Application {

    private Server server;
    private ServerSocket ss;
    private Boolean isServerOn;

    private serverDisplay serverScene;
    private int port;


    @Override
    public void start(Stage primaryStage){
        primaryStage.setTitle("Space-Man Server");
        serverScene = new serverDisplay();

        serverScene.serverOff.setOnAction(e->
        {
            try {
                isServerOn = false;
                server.closeConn();
                this.ss.close();
                serverScene.displayServerOff();
                serverScene.message.setText("~ server off ~");
            }
            catch (Exception ex){
                serverScene.message.setText("FAILED TO TURN SERVER OFF");
            }
        });

        primaryStage.setScene(serverScene.scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    // ******************************************************************* //
    //                          GUI Nested Classes                         //
    // ******************************************************************* //
    class serverDisplay {
        private Scene scene;
        private BorderPane serverPane;
        private VBox headerBox, centerBox, topBoxOn;
        private HBox portBox, serverOptions;
        private Label header, message, messageOn;
        private Label portInputLabel, numConnectedLabel;
        private TextArea portInput;
        private Button serverOn, serverOff;

        serverDisplay() {
            isServerOn = false;
            this.serverPane = new BorderPane();
            this.serverPane.setBackground(new Background(new BackgroundFill(Color.MIDNIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            this.serverPane.setPrefSize(500, 400);

            header = new Label("WELCOME TO SPACEMAN");
            header.setTextFill(Color.WHITE);
            header.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 32));
            header.setAlignment(Pos.CENTER);

            message = new Label("~ server off ~");
            message.setTextFill(Color.LEMONCHIFFON);
            message.setFont(Font.font("sans-serif", FontWeight.NORMAL, 18));
            message.setAlignment(Pos.CENTER);

            headerBox = new VBox(10, header, message);
            headerBox.setAlignment(Pos.CENTER);

            portInputLabel = new Label("PORT # : ");
            portInputLabel.setTextFill(Color.WHITE);
            portInputLabel.setFont(Font.font("sans-serif", FontWeight.BOLD, 18));

            portInput = new TextArea();
            portInput.setBackground(new Background(new BackgroundFill(Color.LEMONCHIFFON, CornerRadii.EMPTY, Insets.EMPTY)));
            portInput.setPrefSize(100, 15);

            portBox = new HBox(5, portInputLabel, portInput);
            portBox.setAlignment(Pos.CENTER);

            numConnectedLabel = new Label("[     Clients Connected:   0   ]");
            numConnectedLabel.setTextFill(Color.WHITE);
            numConnectedLabel.setFont(Font.font("sans-serif", FontWeight.BOLD, 15));
            numConnectedLabel.setAlignment(Pos.CENTER);

            serverOn = new Button("ON");
            serverOn.setBackground(new Background(new BackgroundFill(Color.LEMONCHIFFON, new CornerRadii(10), Insets.EMPTY)));
            serverOn.setPrefSize(60, 30);
            serverOn.setTextFill(Color.MIDNIGHTBLUE);
            serverOn.setOnAction(createServer);

            serverOff = new Button("OFF");
            serverOff.setBackground(new Background(new BackgroundFill(Color.LEMONCHIFFON, new CornerRadii(10), Insets.EMPTY)));
            serverOff.setPrefSize(60, 30);
            serverOff.setTextFill(Color.MIDNIGHTBLUE);
            serverOff.setDisable(true);

            serverOptions = new HBox(10, serverOn, serverOff);
            serverOptions.setAlignment(Pos.CENTER);

            centerBox = new VBox(15, portBox, serverOptions, numConnectedLabel);
            centerBox.setPadding(new Insets(50));
            centerBox.setAlignment(Pos.TOP_CENTER);

            serverPane.setCenter(centerBox);
            serverPane.setTop(headerBox);

            scene = new Scene(serverPane, 500, 400);

        }

        void displayServerOn(){
            portInput.setVisible(false);
            portInputLabel.setVisible(false);
            topBoxOn = new VBox(10, header, message, serverOptions, numConnectedLabel);
            topBoxOn.setPadding(new Insets(10));
            topBoxOn.setAlignment(Pos.TOP_CENTER);
            serverPane.setTop(topBoxOn);
            serverOn.setDisable(true);
        }
        void displayServerOff(){
            portInput.setVisible(true);
            portInput.clear();
            portInputLabel.setVisible(true);
            portBox.setVisible(true);
            topBoxOn = new VBox(10, header, message, this.portBox, serverOptions, numConnectedLabel);
            topBoxOn.setPadding(new Insets(10));
            topBoxOn.setAlignment(Pos.TOP_CENTER);
            serverPane.setTop(topBoxOn);
            serverOn.setDisable(false);
            serverOff.setDisable(true);
        }

        EventHandler<ActionEvent> createServer = new EventHandler<ActionEvent>() {

            public void handle(ActionEvent event) {
                String p = portInput.getText().split("\n")[0];

                try {
                    port = Integer.valueOf(p);
                    ss  = new ServerSocket(port);
                    serverOn.setDisable(true);
                    serverOff.setDisable(false);
                    portBox.setVisible(false);
                    message.setText("~ server on ~");
                    isServerOn = true;
                }catch(Exception e){
                    System.out.println("Failure-> creating server socket");
                    portInput.clear();
                    serverOn.setDisable(false);
                    serverOff.setDisable(true);
                }


                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        server = new Server(Integer.parseInt(p), data-> Platform.runLater(()->{
                            if(data.toString().equals("CONNECTION") || data.toString().equals("DISCONNECTED")){
                                numConnectedLabel.setText("[     Clients Connected:   "+server.numClients+ "   ]");
                            }
                        }) );
                        server.startConn(ss);
                        return null;
                    }
                };

                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();

                if (isServerOn){
                    displayServerOn();
                }
            }

        };

        class clientDisplay {

            private HBox clientStats;
            private String connected, status, clientIndex;
            private Label screenNameLabel, connectedLabel, statusLabel;

            clientDisplay(String clientIndex) {
                this.clientIndex = clientIndex;
                this.connected = " ~ ";
                this.status = " ~ "; //letters guessed
                screenNameLabel = new Label(this.clientIndex);
                screenNameLabel.setPrefSize(100, 30);
                screenNameLabel.setAlignment(Pos.CENTER);
                connectedLabel = new Label(connected);
                connectedLabel.setPrefSize(100, 30);
                connectedLabel.setAlignment(Pos.CENTER);
                statusLabel = new Label(status);
                statusLabel.setPrefSize(100, 30);
                statusLabel.setAlignment(Pos.CENTER);
                clientStats = new HBox(3, this.screenNameLabel, this.connectedLabel, this.statusLabel);
                clientStats.setAlignment(Pos.CENTER);
            }
        }
        // ********************  End Nested GUI Classes ********************** //
        // ******************************************************************* //

    }
}