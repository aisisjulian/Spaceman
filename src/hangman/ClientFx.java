package hangman;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;

public class ClientFx extends Application {

    private ClientConnection client;
    private boolean isConnected = false;
    private ArrayList<String> lettersPlayed = new ArrayList<>();
    private Runnable task;
    private Thread t;
    private boolean started = false;
    private boolean isSinglePlayer = false;
    private boolean alreadyPlayed = false;

    private int numLives = 5;
    private String chosenDifficulty = "";
    private int chosenNumPlayers = 0;
    private String word = "welcome";
    private Scene startScene, waitScene, gameScene, endScene;
    private StartScene ss = new StartScene();
    private waitScene ws = new waitScene();
    private GameScene gs = new GameScene();
    private EndScene es;
    private BorderPane startPane, waitPane, gamePane, endPane;
    private VBox connectionBox;
    private VBox optionsBox;
    final String HOVERED_BUTTON_STYLE = "-fx-background-color: deeppink,  -fx-shadow-highlight-color, -fx-outer-border, -fx-inner-border, -fx-body-color;";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception{
        if(isConnected) {
            client.send("DISCONNECTED");
            isConnected = false;
            client.closeConn();
        }
    }
    @Override
    public void start(Stage primaryStage){
        primaryStage.setTitle("Welcome to Spaceman :-)");
        startScene = ss.scene;
        gameScene = gs.scene;
        waitScene = ws.scene;
        primaryStage.setScene(this.startScene);


        // ******************************************************************* //
        //                       START - EVENT HANDLERS                        //
        // ******************************************************************* //

        ss.connectButton.setOnAction(event->{
            if(!ss.ipInput.getText().isEmpty() && !ss.portInput.getText().isEmpty()){
                try {
                    if(!started) {
                        started = true;
                        client = createClient(ss.ipInput.getText(), Integer.parseInt(ss.portInput.getText()), primaryStage);
                        task = () -> client.clientConnect();
                        t = new Thread(task);
                        t.setDaemon(true);
                        t.start();
                    }
                }
                catch(Exception e){
                    ss.connectButton.setDisable(false);
                    ss.ipInput.clear();
                    ss.portInput.clear();
                    System.out.println("exception with connect button");
                }
            }
        });

        ss.startButton.setOnAction(event->{
            if(!chosenDifficulty.equals("") && chosenNumPlayers != 0){
                client.send("NUM-PLAYERS: " + chosenNumPlayers);
                client.send("DIFFICULTY: " + chosenDifficulty);
                primaryStage.setScene(waitScene);
            }

            ss.startButton.setDisable(true);
        });

        // ******************************************************************* //
        //                       GAME - EVENT HANDLERS                         //
        // ******************************************************************* //




        // ******************************************************************* //
        //                       END - EVENT HANDLERS                          //
        // ******************************************************************* //



        primaryStage.show();
    }

    private Client createClient(String IP, int portIn, Stage primaryStage) {
        return new Client(IP, portIn, data -> {
            Platform.runLater(() -> {

                if(data.toString().split(" ")[0].equals("WORD:")){
                    this.word = data.toString().split(" ")[1];
                    System.out.println(word);
                    this.gs = new GameScene();
                    gs.initSpaceshipImages(primaryStage);
                    this.gameScene = gs.scene;
                    gs.disableKeyboard();
                    gs.submitButton.setDisable(true);
                    primaryStage.setScene(this.gameScene);
                }
                if(data.toString().split(" ")[0].equals("LETTER:")){
                    String l = data.toString().split(" ")[1];
                    lettersPlayed.add(l);
                    gs.updateWordDisplay();

                    boolean eval = false;
                    for (int i = 0; i < word.length(); i++) {
                        if (word.charAt(i) == l.charAt(0)) {
                                gs.header.setOpacity(1.0);
                                gs.header.setText("YAY! Letter '" + l + "' is in the word!");
                                gs.fadeHeader.playFromStart();
                            eval = true;
                        }
                    }
                    if (!eval) {
                        numLives--;
                            gs.header.setOpacity(1.0);
                            gs.header.setText("BOO! Letter '" + l + "' is not in the word!");
                            gs.fadeHeader.playFromStart();
                        if(numLives >= 0) {
                            gs.updateSpaceShipImage();
                        }
                    }
                }
                switch (data.toString()) {
                    case "CONNECTION":
                        ss.header.setText("~ Connected to Server ~");
                        ss.header.setPrefSize(300, 40);
                        ss.header.setAlignment(Pos.CENTER);
                        isConnected = true;
                        ss.ipInput.clear();
                        ss.ipInput.setVisible(false);
                        ss.ipLabel.setVisible(false);
                        ss.portLabel.setVisible(false);
                        ss.portInput.clear();
                        ss.portInput.setVisible(false);
                        ss.connectButton.setDisable(true);
                        startPane.setCenter(optionsBox);
                        break;
                    case "NO-CONNECTION":
                        isConnected = false;
                        ss.ipInput.clear();
                        ss.ipInput.setVisible(true);
                        ss.ipLabel.setVisible(true);
                        ss.portLabel.setVisible(true);
                        ss.portInput.clear();
                        ss.portInput.setVisible(true);
                        ss.connectButton.setDisable(false);
                        ss.connectButton.setText("CONNECT");
                        ss.header.setText("~ No Connection ~");
                        ss.header.setPrefSize(300, 40);
                        ss.header.setAlignment(Pos.CENTER);
                        started = false;
                        primaryStage.setScene(this.startScene);
                        break;
                    case "START":
                        this.numLives = 5;
                        break;
                    case "SINGLE-PLAYER":
                        isSinglePlayer = true;
                        break;
                    case "WAITING-FOR-GUESS":
                        if((gs.header.getText().equals("") && !isSinglePlayer) || gs.header.getText().equals("Waiting for next player to guess...")) {
                            gs.header.setText("Choose a letter to guess.");
                            gs.fadeHeader.setOnFinished(ev-> {gs.header.setText("");});
                            gs.fadeHeader.play();
                        }
                        else if (!isSinglePlayer){
                            gs.fadeHeader.setOnFinished(e->{gs.header.setText("Choose a letter to guess.");
                                 gs.fadeHeader.play();});
                        }
                        gs.enableKeyboard();
                        break;
                    case "WIN":
                        gs.disableKeyboard();
                        this.es = new EndScene(primaryStage);
                        this.endScene = es.scene;
                        gs.updateSpaceShipImage();
                        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000));
                        fadeTransition.setNode(gs.spaceship);
                        fadeTransition.setFromValue(1.0);
                        fadeTransition.setToValue(0);
                        fadeTransition.setCycleCount(2);
                        fadeTransition.setAutoReverse(false);
                        fadeTransition.setOnFinished(ex-> primaryStage.setScene(endScene));
                        gs.header.setText("YOU WIN :-)");
                        gs.fadeHeader.setOnFinished(ex-> fadeTransition.playFromStart());
                        gs.fadeHeader.playFromStart();
                        es.endMessage.setText("congrats, you win :-)");

                        break;
                    case "LOSE":
                        gs.disableKeyboard();
                        this.es = new EndScene(primaryStage);
                        this.endScene = es.scene;
                        gs.header.setText("YOU LOSE :-(");
                        gs.fadeHeader.playFromStart();
                        gs.updateSpaceShipImage();
                        es.endMessage.setText("sorry, you lose :-(");
                        break;
                }
            });
        });


    }

    // ******************************************************************* //
    //                          GUI Nested Classes                         //
    // ******************************************************************* //
    class StartScene{
        private Scene scene;
        private Image startBackgroundImage;
        private Background startBackground;
        private Button singlePlayerButton, multiPlayerButton, twoPlayerButton, threePlayerButton, fourPlayerButton, easyButton, mediumButton, hardButton;
        private Button connectButton, startButton;
        private TextArea ipInput, portInput;
        private Label header, ipLabel, portLabel;

        public StartScene(){
            startPane = new BorderPane();
            startBackgroundImage  = new Image("startScene.png");
            startBackground = new Background(new BackgroundFill(new ImagePattern(startBackgroundImage), CornerRadii.EMPTY, Insets.EMPTY));
            Image buttonBackgroundImage  = new Image("buttonBackground.png");
            Background buttonBackground = new Background(new BackgroundFill(new ImagePattern(buttonBackgroundImage), CornerRadii.EMPTY, Insets.EMPTY));
            startPane.setBackground(startBackground);

            header = new Label("");
            header.setTextFill(Color.WHITE);
            header.setPrefSize(250, 40);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(15));
            portLabel = new Label("PORT :");
            portLabel.setTextFill(Color.WHITE);
            portLabel.setFont(Font.font("Courier", FontWeight.BOLD, 17));
            portLabel.setPrefSize(75, 15);
            portInput = new TextArea();
            portInput.setPrefSize(150, 15);
            portInput.setPadding(new Insets(5, 0, 5, 0));


            ipLabel = new Label(" IP  :");
            ipLabel.setTextFill(Color.WHITE);
            ipLabel.setFont(Font.font("Courier", FontWeight.BOLD, 17));
            ipLabel.setPrefSize(75, 15);
            ipInput = new TextArea();
            ipInput.setPrefSize(150, 15);
            ipInput.setPadding(new Insets(5, 0, 5, 0));

            HBox ipBox = new HBox(ipLabel, ipInput);
            HBox portBox = new HBox(portLabel, portInput);
            ipBox.setAlignment(Pos.CENTER);
            portBox.setAlignment(Pos.CENTER);

            connectButton = new Button("CONNECT");
            connectButton.setPrefSize(175, 80);
            connectButton.setBackground(buttonBackground);
            connectButton.setTextFill(Color.NAVY);
            connectButton.setFont(Font.font("Courier", FontWeight.EXTRA_BOLD, 22));
            connectButton.setTextAlignment(TextAlignment.CENTER);
            connectionBox = new VBox(10, header, ipBox, portBox, connectButton);
            connectionBox.setAlignment(Pos.CENTER);

            singlePlayerButton = new Button(" Single-Player");
            singlePlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            singlePlayerButton.setPrefSize(150, 40);
            singlePlayerButton.setTextFill(Color.INDIGO);
            singlePlayerButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 18));
            singlePlayerButton.setOnMouseClicked(e -> {
                isSinglePlayer = true;
                chosenNumPlayers = 1;
                if(!chosenDifficulty.equals("")){
                    startButton.setDisable(false);
                }
                singlePlayerButton.setBackground(new Background(new BackgroundFill(Color.DEEPPINK, new CornerRadii(7), Insets.EMPTY)));
                multiPlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            });
            singlePlayerButton.setOnAction(e->{
                isSinglePlayer = true;
                chosenNumPlayers = 1;
                if(!chosenDifficulty.equals("")){
                    startButton.setDisable(false);
                }
                twoPlayerButton.setDisable(true); twoPlayerButton.setOpacity(.5);
                threePlayerButton.setDisable(true); threePlayerButton.setOpacity(.5);
                fourPlayerButton.setDisable(true); fourPlayerButton.setOpacity(.5);
            } );

            multiPlayerButton = new Button("Multi-Player");
            multiPlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            multiPlayerButton.setPrefSize(150, 40);
            multiPlayerButton.setTextFill(Color.INDIGO);
            multiPlayerButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 18));
            multiPlayerButton.setOnMouseClicked(e -> {
                isSinglePlayer = false;
                twoPlayerButton.setDisable(false);
                threePlayerButton.setDisable(false);
                fourPlayerButton.setDisable(false);
                multiPlayerButton.setBackground(new Background(new BackgroundFill(Color.DEEPPINK, new CornerRadii(7), Insets.EMPTY)));
                singlePlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            });

            HBox playerMode = new HBox(10, singlePlayerButton, multiPlayerButton);
            playerMode.setAlignment(Pos.CENTER);

            twoPlayerButton = new Button("2-Players");
            twoPlayerButton.setBackground(new Background(new BackgroundFill(Color.LAVENDERBLUSH, new CornerRadii(20), Insets.EMPTY)));
            twoPlayerButton.setPrefSize(75, 25);
            twoPlayerButton.setTextFill(Color.INDIGO);
            twoPlayerButton.setFont(Font.font("verdana", FontWeight.BOLD, 11));
            twoPlayerButton.setOnAction(e->{
                chosenNumPlayers = 2;
                if(!chosenDifficulty.equals("") && !isSinglePlayer){
                    startButton.setDisable(false);
                }
                twoPlayerButton.setOpacity(.5);
                threePlayerButton.setOpacity(1);
                fourPlayerButton.setOpacity(1);
            });

            threePlayerButton = new Button( "3-Players");
            threePlayerButton.setBackground(new Background(new BackgroundFill(Color.LAVENDERBLUSH, new CornerRadii(20), Insets.EMPTY)));
            threePlayerButton.setPrefSize(75, 25);
            threePlayerButton.setTextFill(Color.INDIGO);
            threePlayerButton.setFont(Font.font("verdana", FontWeight.BOLD, 11));
            threePlayerButton.setOnAction(e->{
                chosenNumPlayers = 3;
                if(!chosenDifficulty.equals("") && !isSinglePlayer){
                    startButton.setDisable(false);
                }
                twoPlayerButton.setOpacity(1);
                threePlayerButton.setOpacity(.5);
                fourPlayerButton.setOpacity(1);
            });

            fourPlayerButton = new Button("4-Players");
            fourPlayerButton.setBackground(new Background(new BackgroundFill(Color.LAVENDERBLUSH, new CornerRadii(20), Insets.EMPTY)));
            fourPlayerButton.setPrefSize(75, 25);
            fourPlayerButton.setTextFill(Color.INDIGO);
            fourPlayerButton.setFont(Font.font("verdana", FontWeight.BOLD, 11));
            fourPlayerButton.setOnAction(e->{
                chosenNumPlayers = 4;
                if(!chosenDifficulty.equals("") && !isSinglePlayer){
                    startButton.setDisable(false);
                }
                twoPlayerButton.setOpacity(1);
                threePlayerButton.setOpacity(1);
                fourPlayerButton.setOpacity(.5);
            });

            HBox difficultyBox;
            easyButton = new Button("Easy");
            easyButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            easyButton.setPrefSize(80, 20);
            easyButton.setTextFill(Color.INDIGO);
            easyButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 14));
            easyButton.setOnMouseClicked(e -> {
                chosenDifficulty = "easy";
                if((!isSinglePlayer && chosenNumPlayers > 1) || (isSinglePlayer && chosenNumPlayers == 1)){
                    startButton.setDisable(false);
                }
                easyButton.setBackground(new Background(new BackgroundFill(Color.DEEPPINK, new CornerRadii(7), Insets.EMPTY)));
                mediumButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                hardButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            });
            mediumButton = new Button("Medium");
            mediumButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            mediumButton.setPrefSize(80, 20);
            mediumButton.setTextFill(Color.INDIGO);
            mediumButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 14));
            mediumButton.setOnMouseClicked(e -> {
                chosenDifficulty = "medium";
                if((!isSinglePlayer && chosenNumPlayers > 1) || (isSinglePlayer && chosenNumPlayers == 1)){
                    startButton.setDisable(false);
                }
                mediumButton.setBackground(new Background(new BackgroundFill(Color.DEEPPINK, new CornerRadii(7), Insets.EMPTY)));
                easyButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                hardButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            });
            hardButton = new Button("hard");
            hardButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            hardButton.setPrefSize(90, 20);
            hardButton.setTextFill(Color.INDIGO);
            hardButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 14));
            hardButton.setOnMouseClicked(e -> {
                chosenDifficulty = "hard";
                if((!isSinglePlayer && chosenNumPlayers > 1) || (isSinglePlayer && chosenNumPlayers == 1)){
                    startButton.setDisable(false);
                }
                hardButton.setBackground(new Background(new BackgroundFill(Color.DEEPPINK, new CornerRadii(7), Insets.EMPTY)));
                mediumButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                easyButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            });
            difficultyBox = new HBox(8, easyButton, mediumButton, hardButton);
            difficultyBox.setAlignment(Pos.CENTER);
            easyButton.setAlignment(Pos.CENTER); mediumButton.setAlignment(Pos.CENTER); hardButton.setAlignment(Pos.CENTER);

            startButton = new Button("START");
            startButton.setPrefSize(150, 80);
            startButton.setBackground(buttonBackground);
            startButton.setTextFill(Color.MIDNIGHTBLUE);
            startButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 19));
            startButton.setTextAlignment(TextAlignment.CENTER);
            startButton.setDisable(true);

            HBox numPlayersBox = new HBox(8, twoPlayerButton, threePlayerButton, fourPlayerButton);
            numPlayersBox.setAlignment(Pos.CENTER);
            optionsBox = new VBox(15, header, playerMode, numPlayersBox, difficultyBox, startButton);
            optionsBox.setAlignment(Pos.CENTER);
            startPane.setCenter(connectionBox);
            scene = new Scene(startPane, 500, 500);
        }
    }

    class waitScene{
        private Scene scene;
        private Image waitBackgroundImage;
        private Background waitBackground;
        private Label waitMessage;
        private Label alienPic;

        public waitScene(){
            waitPane = new BorderPane();
            waitBackgroundImage  = new Image("startScene.png");
            waitBackground = new Background(new BackgroundFill(new ImagePattern(waitBackgroundImage), CornerRadii.EMPTY, Insets.EMPTY));
            waitPane.setBackground(waitBackground);

            waitMessage = new Label ("* Waiting for Players to Join *");
            waitMessage.setPrefSize(500, 80);
            waitMessage.setBackground(new Background(new BackgroundFill(new Color(0x19/255.0, 0x19/255.0, 0x70/255.0, .7), new CornerRadii(0), Insets.EMPTY)));
            waitMessage.setTextFill(Color.LAVENDERBLUSH);
            waitMessage.setFont(Font.font("serif", FontWeight.EXTRA_LIGHT, 22));
            waitMessage.setAlignment(Pos.CENTER);
            waitPane.setBottom(waitMessage);
            alienPic = new Label();
            Image p = new Image("spaceship7.png");
            ImageView iv = new ImageView(p);
            iv.setFitHeight(350);
            iv.setFitWidth(400);
            alienPic.setGraphic(iv);
            alienPic.setAlignment(Pos.BOTTOM_CENTER);
            alienPic.setPrefSize(200,250);

            RotateTransition rotateTransition = new RotateTransition();
            ScaleTransition scaleTransition = new ScaleTransition();
            scaleTransition.setDuration(Duration.millis(1200));
            scaleTransition.setNode(alienPic);
            scaleTransition.setByY(-.30);
            scaleTransition.setByX(-.30);
            scaleTransition.setCycleCount(50);
            scaleTransition.setAutoReverse(true);
            rotateTransition.setDuration(Duration.millis(1000));
            rotateTransition.setNode(alienPic);
            rotateTransition.setByAngle(360);
            rotateTransition.setCycleCount(100);
            rotateTransition.setAutoReverse(false);
            rotateTransition.play();
            scaleTransition.play();
            waitPane.setCenter(alienPic);
            scene = new Scene(waitPane, 500, 500);
        }
    }



    class GameScene{
        private Scene scene;
        private Image gameBackgroundImage;
        private Background gameBackground;

        private ArrayList<Button> keyboard;
        private HBox bottomDisplay; //to display keyboard and letter entered
        private VBox keyboardBox;
        private HBox row1;
        private HBox row2;
        private HBox row3;

        private HBox wordDisplay;
        private ArrayList<Label> wordDisplayList;
        private VBox bottomBox;

        private VBox enterBox;
        private Label letterChosenLabel;
        private String letter;
        private Button pressed;
        private Button submitButton;
        private Button endButton;

        private Label spaceship;
        private int imageNum = 0;
        private ArrayList<Image> ssImageList;
        Stage primaryStage;

        private Label header;
        FadeTransition fadeHeader = new FadeTransition(Duration.millis(4000));

        public GameScene(){
            numLives = 5;
            gamePane = new BorderPane();
            gameBackgroundImage = new Image("gameScene2.png");
            gameBackground = new Background(new BackgroundFill(new ImagePattern(gameBackgroundImage), CornerRadii.EMPTY, Insets.EMPTY));
            gamePane.setBackground(gameBackground);
            initWordDisplay();
            initKeyboard();
            this.bottomBox = new VBox(10, this.wordDisplay, this.bottomDisplay);
            bottomBox.setAlignment(Pos.CENTER);
            gamePane.setBottom(bottomBox);
            scene = new Scene(gamePane, 800, 600);
            lettersPlayed = new ArrayList<>();

            this.header = new Label("");
            header.setFont(Font.font("Courier", FontWeight.EXTRA_LIGHT, 24));
            header.setTextFill(Color.WHITE);
            header.setPrefSize(1000, 50);
            header.setBackground(new Background(new BackgroundFill(new Color(0xCD/255.0, 0x28/255.0, 0x39/255.0, .7), new CornerRadii(0), Insets.EMPTY)));
            header.setAlignment(Pos.CENTER);
            fadeHeader.setNode(header);
            fadeHeader.setFromValue(1.0);
            fadeHeader.setToValue(0.2);
            fadeHeader.setCycleCount(1);
            fadeHeader.setAutoReverse(false);
            fadeHeader.setOnFinished(e-> {header.setText("");});
            gamePane.setTop(header);
        }

        public void initWordDisplay(){
            this.wordDisplay = new HBox(10);
            this.wordDisplayList = new ArrayList<>();
            for(int i = 0; i < word.length(); i++){
                Label character = new Label("_");
                character.setTextFill(Color.MIDNIGHTBLUE);
                character.setFont(Font.font("Courier", FontWeight.EXTRA_BOLD, 33));
                character.setAlignment(Pos.CENTER);
                character.setPrefSize(40, 40);
                character.setBackground(new Background(new BackgroundFill(Color.LEMONCHIFFON, new CornerRadii(5), Insets.EMPTY)));
                wordDisplayList.add(character);
                this.wordDisplay.getChildren().add(wordDisplayList.get(i));
            }
            this.wordDisplay.setAlignment(Pos.CENTER);
        }

        public void updateWordDisplay(){
            initWordDisplay();
            for(int i = 0; i < word.length(); i++){
                if(lettersPlayed.contains(word.charAt(i)+"")){
                    wordDisplayList.get(i).setText(word.charAt(i)+"");
                }
            }
            this.bottomBox = new VBox(10, this.wordDisplay, this.bottomDisplay);
            bottomBox.setAlignment(Pos.CENTER);
            gamePane.setBottom(bottomBox);
        }

        public void initSpaceshipImages(Stage primaryStage){
            this.primaryStage = primaryStage;
            this.spaceship = new Label();
            this.spaceship.setAlignment(Pos.BOTTOM_CENTER);
            this.ssImageList = new ArrayList<>();

            for(int i = 0; i < 7; i++){
                Image spaceShipImage = new Image("spaceship" + (i+1) + ".png");
                this.ssImageList.add(spaceShipImage);
            }
            ImageView iv = new ImageView(ssImageList.get(imageNum));
            iv.setFitWidth(350);
            iv.setFitHeight(350);
            this.spaceship.setGraphic(iv);
            gamePane.setCenter(this.spaceship);
        }

        public void updateSpaceShipImage(){
            imageNum++;
            ImageView iv = new ImageView(ssImageList.get(imageNum));
            iv.setFitWidth(350);
            iv.setFitHeight(350);
            this.spaceship.setGraphic(iv);

            if(imageNum == 6){
                disableKeyboard();
                Path path = new Path();
                MoveTo moveTo = new MoveTo(100, 175);
                CubicCurveTo cubicCurveTo = new CubicCurveTo(400, 40, 175, 250, 700, 0);
                path.getElements().add(moveTo);
                path.getElements().add(cubicCurveTo);
                PathTransition pathTransition = new PathTransition();
                pathTransition.setDuration(Duration.millis(2000));
                pathTransition.setNode(this.spaceship);
                pathTransition.setPath(path);
                pathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
                pathTransition.setAutoReverse(false);
                RotateTransition rotateTransition = new RotateTransition();
                rotateTransition.setDuration(Duration.millis(550));
                rotateTransition.setNode(this.spaceship);
                rotateTransition.setByAngle(360);
                rotateTransition.setCycleCount(4);
                rotateTransition.setAutoReverse(false);
                rotateTransition.play();
                pathTransition.setOnFinished(e -> {
                primaryStage.setScene(endScene);});
                pathTransition.play();

            }
            else {
                TranslateTransition translateTransition = new TranslateTransition();
                translateTransition.setDuration(Duration.millis(50));
                translateTransition.setNode(wordDisplay);
                translateTransition.setByX(30);
                translateTransition.setByX(-30);
                translateTransition.setCycleCount(3);
                translateTransition.setAutoReverse(true);
                translateTransition.play();
                ScaleTransition scaleTransition = new ScaleTransition();
                scaleTransition.setDuration(Duration.millis(300));
                scaleTransition.setNode(this.spaceship);
                scaleTransition.setByY(1.0);
                scaleTransition.setByX(1.0);
                scaleTransition.setCycleCount(2);
                scaleTransition.setAutoReverse(true);
                scaleTransition.play();
            }
            gamePane.setCenter(this.spaceship);
        }

        public void initKeyboard(){
            this.keyboard = new ArrayList<>();
            this.row1 = new HBox(10);
            this.row2 = new HBox(10);
            this.row3 = new HBox(10);

            for (int i = 0; i < 26; i++){
                char letter = (char)(65 + i);
                Button k = new Button("" + letter + "");
                k.setTextAlignment(TextAlignment.CENTER);
                this.keyboard.add(k);
                this.keyboard.get(i).setPrefSize(32, 32);
                k.setOnAction(displayLetter);
                if (i < 9){
                    row1.getChildren().add(k);
                }
                else if(i < 17){
                    row2.getChildren().add(k);
                }
                else {
                    row3.getChildren().add(k);
                }
            }

            row1.setAlignment(Pos.CENTER);
            row2.setAlignment(Pos.CENTER);
            row3.setAlignment(Pos.CENTER);
            keyboardBox = new VBox(10, row1, row2, row3);
            keyboardBox.setAlignment(Pos.CENTER_LEFT);
            keyboardBox.setPrefSize(510, 150);
            keyboardBox.setPadding(new Insets(3,100,3,0));
            this.keyboardBox.setDisable(false);

            /*display letter entered code*/
            bottomDisplay = new HBox();
            bottomDisplay.setBackground(new Background(new BackgroundFill(new Color(0x19/255.0, 0x19/255.0, 0x70/255.0, .7), new CornerRadii(0), Insets.EMPTY)));
            this.enterBox = new VBox(10);
            letterChosenLabel = new Label();
            letterChosenLabel.setTextFill(Color.GOLD);
            letterChosenLabel.setAlignment(Pos.BASELINE_LEFT);
            letterChosenLabel.setFont(Font.font("Courier", FontWeight.EXTRA_BOLD, 42));
            letterChosenLabel.setAlignment(Pos.CENTER);

            submitButton = new Button("Submit");
            submitButton.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 12));
            submitButton.setAlignment(Pos.CENTER);
            submitButton.setOnAction(sendLetter);

            enterBox.getChildren().addAll(letterChosenLabel, submitButton);
            enterBox.setPrefSize(120, 150);
            enterBox.setPadding(new Insets(0, 0,0, 0));

            bottomDisplay.setPrefSize(800, 150);
            this.bottomDisplay.setAlignment(Pos.CENTER);
            this.bottomDisplay.getChildren().addAll(this.enterBox, this.keyboardBox);
            this.enterBox.setAlignment(Pos.CENTER);
        }

        void enableKeyboard(){
            for(int i = 0; i < keyboard.size(); i++) {
                if(lettersPlayed.contains(keyboard.get(i).getText())){
                    keyboard.get(i).setDisable(true);
                }
                else{
                    keyboard.get(i).setDisable(false);
                }
            }
        }

        void disableKeyboard(){
            for(int i = 0; i < keyboard.size(); i++) {
                this.keyboard.get(i).setDisable(true);
            }
        }

        EventHandler<ActionEvent> sendLetter = event -> {
            pressed.setDisable(true);

            if (!(letter.isBlank())) {
                lettersPlayed.add(letter);
                submitButton.setDisable(true); /*disable until its that players turn*/
                updateWordDisplay();
                boolean eval = false;
                for(int i = 0; i < word.length(); i++){
                    if(word.charAt(i) == letter.charAt(0)){
                            gs.header.setOpacity(1.0);
                            gs.header.setText("YAY! Letter '" + letter + "' is in the word!");
                            gs.fadeHeader.playFromStart();
                        eval = true;
                    }
                }
                if(!eval){
                    numLives--;
                        gs.header.setOpacity(1.0);
                        gs.header.setText("BOO! Letter '" + letter + "' is not in the word!");
                        gs.fadeHeader.playFromStart();
                    if(numLives >= 0) {
                        updateSpaceShipImage();
                    }
                }
                if(gs.header.getText().equals("") && !isSinglePlayer) {
                    gs.header.setText("Waiting for next player to guess...");
                    gs.fadeHeader.setOnFinished(ev-> {gs.header.setText("");});
                    gs.fadeHeader.play();
                }
                else if(!isSinglePlayer ){
                    gs.fadeHeader.setOnFinished(e->{gs.header.setText("Waiting for next player to guess...");
                         gs.fadeHeader.play();});
                }
                client.send("LETTER: " + letter);
                disableKeyboard();
            }


        };

        EventHandler<ActionEvent> displayLetter = event -> {
            int i = this.keyboard.indexOf((Button) event.getSource());
            pressed = this.keyboard.get(i);
            letter = pressed.getText();
            letterChosenLabel.setText(letter);
            for (int j = 0; j < lettersPlayed.size(); j++){
                if (lettersPlayed.get(j).equals(letter)){ alreadyPlayed = true; }
            }
            if (!alreadyPlayed){ submitButton.setDisable(false); }
        };
    }

    class EndScene{
        private Scene scene;
        private Image endBackgroundImage;
        private Background endBackground;
        private Label endTitle;
        private Label endMessage;
        private Button quitBtn, playAgainBtn;
        private HBox buttonBox;
        private VBox endBox;
        private HBox cword;

        public EndScene(Stage primaryStage){
            endPane = new BorderPane();
            endBackgroundImage = new Image("endScene.png");
            endBackground = new Background(new BackgroundFill(new ImagePattern(endBackgroundImage), CornerRadii.EMPTY, Insets.EMPTY));
            endPane.setBackground(endBackground);

            endTitle = new Label("GAME OVER");
            endTitle.setPrefSize(1000, 50);

            endTitle.setTextFill(Color.WHITE);
            endTitle.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 50));
            endTitle.setAlignment(Pos.CENTER);

            endMessage = new Label("");
            endMessage.setPrefSize(1000, 50);

            endMessage.setTextFill(Color.WHITE);
            endMessage.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 20));
            endMessage.setAlignment(Pos.CENTER);

            // display letters guessed
            for (int i = 0; i < lettersPlayed.size(); i++){
                System.out.print(lettersPlayed.get(i));
            }

            cword = new HBox(3);
            for (int i = 0; i < word.length(); i++){
                Label letter = new Label(word.charAt(i) + "");
                if (lettersPlayed.contains(word.charAt(i) + "")){
                    letter.setTextFill(Color.GOLD);
                }
                else { letter.setTextFill(Color.LEMONCHIFFON); }
                letter.setAlignment(Pos.BASELINE_LEFT);
                letter.setFont(Font.font("Courier", FontWeight.EXTRA_BOLD, 42));
                cword.getChildren().add(letter);
            }

            quitBtn = new Button("Quit");
            quitBtn.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            quitBtn.setPrefSize(130, 40);
            quitBtn.setTextFill(Color.INDIGO);
            quitBtn.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 18));

            quitBtn.setOnAction(actionEvent -> {
                try {
                    client.closeConn();
                    Stage stage = (Stage) quitBtn.getScene().getWindow();
                    // do what you have to do
                    stage.close();
                }
                catch(Exception e){

                }
                System.out.println("Quit game");
            });

            playAgainBtn = new Button("Play Again");

            playAgainBtn.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
            playAgainBtn.setPrefSize(130, 40);
            playAgainBtn.setTextFill(Color.INDIGO);
            playAgainBtn.setFont(Font.font("sans-serif", FontWeight.EXTRA_BOLD, 18));
            playAgainBtn.setOnAction(event->{
                System.out.println("play again");
                primaryStage.setScene(startScene);
                ss.startButton.setDisable(false);
                ss.singlePlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                ss.multiPlayerButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                ss.easyButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                ss.mediumButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                ss.hardButton.setBackground(new Background(new BackgroundFill(Color.GOLD, new CornerRadii(7), Insets.EMPTY)));
                ss.twoPlayerButton.setDisable(true); ss.twoPlayerButton.setOpacity(.5);
                ss.threePlayerButton.setDisable(true); ss.threePlayerButton.setOpacity(.5);
                ss.fourPlayerButton.setDisable(true); ss.fourPlayerButton.setOpacity(.5);
                ss.startButton.setDisable(true);
                numLives = 5;
                gs.initSpaceshipImages(primaryStage);
            });

            buttonBox = new HBox(10, quitBtn, playAgainBtn);
            buttonBox.setAlignment(Pos.CENTER);

            endBox = new VBox(30, endTitle, endMessage, buttonBox);
            endBox.setAlignment(Pos.CENTER);
            endPane.setCenter(endBox);

            endPane.setBottom(cword);
            cword.setPadding(new Insets(60, 0, 60, 0));
            cword.setBackground(new Background(new BackgroundFill(new Color(0x48/255.0, 0x12/255.0, 0x70/255.0, .7), new CornerRadii(0), Insets.EMPTY)));
            cword.setAlignment(Pos.CENTER);

            scene = new Scene(endPane, 500, 500);
        }

    }

    // ********************  End Nested GUI Classes ********************** //
    // ******************************************************************* //
}