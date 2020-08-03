package hangman;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

// ******************************************************************* //
//                                                                     //
//                              Server Class                            //
//                                                                     //
// ******************************************************************* //

public class Server {

    private int port;
    private Consumer<Serializable> callback;
    Serializable data;

    private ArrayList<ClientThread> clientThreadList = new ArrayList<>();
    private HashMap<String, ClientThread> clientThreadMap = new HashMap<>();
    private ArrayList<Game> gamesList = new ArrayList<>();

    private ArrayList<String> dictionary = new ArrayList<>();
    private ArrayList<String> easyDictionary = new ArrayList<>();
    private ArrayList<String> mediumDictionary = new ArrayList<>();
    private ArrayList<String> hardDictionary = new ArrayList<>();

    static int numClients = 0;

    public Server(int port, Consumer<Serializable> callback){
        this.callback = callback;
        this.port = port;
        initDictionary();
    }

    public void initDictionary(){
        String word = "";
        try {
            Scanner inFile = new Scanner(new File ("src/dictionary-small.txt"));
            List<String> temp = new ArrayList<String>();

            while (inFile.hasNext()){
                word = inFile.next();
                word = word.toUpperCase();
                dictionary.add(word);

                if(word.length() <= 4 && word.length() > 2){
                    easyDictionary.add(word);
                }
                else if(word.length() > 4 && word.length() <= 7){
                    mediumDictionary.add(word);
                }
                else if(word.length() > 7){
                    hardDictionary.add(word);
                }
            }
            inFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    public int getPort(){ return this.port; }

    public void startConn(ServerSocket ss){
        try{
            System.out.println("Waiting for clients on server socket: " + ss);
            while(true){
                ClientThread t = new ClientThread(ss.accept());
                clientThreadList.add(t);
                t.clientIndex = clientThreadList.size()-1;
                numClients++;
                t.start();
            }
        }
        catch(IOException e){
            System.out.println("IOException -> startConn()");
        }
    }

    public void send(Serializable data, int clientIndex){
        try{
            if(clientThreadList.get(clientIndex).isConnected) {
                clientThreadList.get(clientIndex).out.writeObject(data);
            }
        }
        catch(Exception e){
            System.out.println("Exception -> send() " + data.toString());
        }

    }

    public void closeConn() throws Exception{
        for(int i = 0; i < clientThreadList.size(); i++){
            if(clientThreadList.get(i).isConnected){
                send("NO-CONNECTION", i);
                clientThreadList.get(i).socket.close();
            }
        }
        numClients = 0;
    }

    // ******************************************************************* //
    //                                                                     //
    //                              Client Thread Class                    //
    //                                                                     //
    // ******************************************************************* //

    class ClientThread extends Thread{
        private int clientIndex;

        // data members for game //
        private Game game;
        private boolean isInGame;
        private ArrayList<ClientThread> team;
        boolean hasPlayed;
        boolean isPlayingAgain;
        private int numPlayersChosen;
        private String difficultyChosen;

        // data members for io //
        ObjectOutputStream out;
        private Socket socket;
        boolean isConnected;

        ClientThread(Socket s){
            this.socket = s;
            setDaemon(true);
            this.isConnected = true;

            this.isPlayingAgain = false;
            this.hasPlayed = false;
            this.isInGame = false;
            this.numPlayersChosen = 0;
            this.difficultyChosen = "";

        }

        public void run() {
           try(
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())){

               this.out = out;
               socket.setTcpNoDelay(true);
               send("CONNECTION", this.clientIndex);
               callback.accept("CONNECTION");
               while(this.isConnected){
                   Serializable data = (Serializable) in.readObject();
                   System.out.println(data.toString());
                   if(data.toString().equals("DISCONNECTED")){
                       numClients--;
                       this.isConnected = false;
                       callback.accept("DISCONNECTED");
                       if(this.game.isActive){
                           if(this.game.players.indexOf(this) == this.game.currentlyGuessing) {
                               this.game.removePlayer(this);
                               if(this.game.numPlayersConnected == 1){
                                   send("SINGLE-PLAYER", game.players.get(0).clientIndex);
                               }
                               if (this.game.currentlyGuessing < this.game.numPlayersConnected - 1) {
                                   this.game.currentlyGuessing++;
                                   send("WAITING-FOR-GUESS", this.game.players.get(this.game.currentlyGuessing).clientIndex);
                               } else {
                                   this.game.currentlyGuessing = 0;
                                   send("WAITING-FOR-GUESS", this.game.players.get(this.game.currentlyGuessing).clientIndex);
                               }
                               if(this.game.numPlayersConnected == 0){
                                   gamesList.remove(this.game);
                                   this.game.isActive = false;
                               }
                           }
                           else{
                               this.game.removePlayer(this);
                               if(this.game.numPlayersConnected == 1){
                                   send("SINGLE-PLAYER", game.players.get(0).clientIndex);
                               }
                           }
                       }
                   }
                   if(data.toString().split(" ")[0].equals("NUM-PLAYERS:")){
                       this.numPlayersChosen = Integer.valueOf(data.toString().split(" ")[1]);
                   }
                   if(data.toString().split(" ")[0].equals("DIFFICULTY:")){
                       this.difficultyChosen = data.toString().split(" ")[1];
                       if(this.numPlayersChosen == 1 && !difficultyChosen.equals("")){
                           this.game = new Game(this);
                           gamesList.add(this.game);
                       }
                       else if(this.numPlayersChosen != 0 && !difficultyChosen.equals("")){
                           this.game = findGame();
                       }
                   }
                   if(data.toString().split(" ")[0].equals("LETTER:")){
                       String letter = data.toString().split(" ")[1];
                       if(!game.evaluateGuess(letter)){
                           gamesList.remove(this.game);
                       }
                   }

               }
           }
           catch(Exception e){
               callback.accept("NO-CONNECTION");

           }

        }

        public Game findGame(){
            Game g;
            for(int i = 0; i < gamesList.size(); i++){
                g = gamesList.get(i);
                if(!g.isActive && g.numPlayers == this.numPlayersChosen && g.difficulty.equals(this.difficultyChosen)){
                    g.addPlayer(this);
                    return g;
                }
            }
            send("WAIT-FOR-PLAYERS", clientIndex);
            g = new Game(this);
            gamesList.add(g);
            return g;
        }

    }
    // ********************  End Client Thread Class ********************* //
    // ******************************************************************* //


    // ******************************************************************* //
    //                                                                     //
    //                              Game Class                             //
    //                                                                     //
    // ******************************************************************* //
    class Game{
        private int numPlayers = 1;
        private int numPlayersConnected = 0;
        private String difficulty = "easy";
        private ArrayList<ClientThread> players;
        private boolean isActive;
        private int currentlyGuessing; //index of player of whose turn it is
        private ArrayList<Boolean> lettersGuessed; //can change to array of characters?
        private String word;
        private ArrayList<Character> lettersInWord;
        private ArrayList<Boolean> lettersGuessedInWord; //each index represents a character of the string
        private int lives = 5; //subject to change

        Game(ClientThread player){
            isActive = false;
            players = new ArrayList<>();
            players.add(player);
            numPlayersConnected++;
            numPlayers = player.numPlayersChosen;
            difficulty = player.difficultyChosen;
            if(numPlayersConnected == numPlayers){ isActive = true; startGame();} //can change total num players in game HERE
            else{
                send("WAIT-FOR-PLAYERS", player.clientIndex);
            }
        }

        void removePlayer(ClientThread player){
            players.remove(player);
            numPlayersConnected--;
        }

        void addPlayer(ClientThread player){
            players.add(player);
            numPlayersConnected++;
            if(numPlayersConnected == numPlayers && !isActive){ isActive = true; startGame();} //can change total num players in game HERE
            else{
                send("WAIT-FOR-PLAYERS", player.clientIndex);
            }
        }

        void startGame(){
            if(isActive){
                //*****************************************//
                lettersGuessed = new ArrayList<>(); //index represents letter
                for(int i = 0; i < 26; i++){ this.lettersGuessed.add(false); }

                Random r = new Random();
                int w;
                if(difficulty.equals("easy")){
                    w = r.nextInt(easyDictionary.size());
                    word = easyDictionary.get(w);
                }
                else if(difficulty.equals("medium")){
                    w = r.nextInt(mediumDictionary.size());
                    word = mediumDictionary.get(w);
                }
                else{
                    w = r.nextInt(hardDictionary.size());
                    word = hardDictionary.get(w);
                }

                removeDup();

                for(int i = 0; i < players.size(); i++){
                    send("WORD: " + word, players.get(i).clientIndex);
                    send("START", players.get(i).clientIndex);
                }

                currentlyGuessing = 0;
                send("WAITING-FOR-GUESS", players.get(currentlyGuessing).clientIndex);
            }
        }

        boolean evaluateGuess(String letter){
            for(int i = 0; i < players.size(); i++){
                if(players.get(i).isConnected && i != currentlyGuessing) {
                    send("LETTER: " + letter, players.get(i).clientIndex);
                }
            }

            if(currentlyGuessing < numPlayersConnected-1){
                currentlyGuessing++;
            }
            else {
                currentlyGuessing = 0;
            }

            send("WAITING-FOR-GUESS", players.get(currentlyGuessing).clientIndex);

            Character chars = letter.charAt(0);

            if (lettersInWord.contains(chars)){
                this.lettersInWord.remove(chars);
                return checkForWin();
            }
            else{
                System.out.println("not in word");
                lives--;
                return checkForLoss();
            }
        }

        boolean checkForWin(){
           if (lettersInWord.size() == 0 ){
               System.out.println("won");
               for(int i = 0; i < players.size(); i++){
                   send("WIN", players.get(i).clientIndex);
                   players.get(i).numPlayersChosen = 0;
                   players.get(i).difficultyChosen = "";
                   this.isActive = false;
                   gamesList.remove(this);
               }
               return true;
           }
            return false;
        }

        void removeDup(){
            this.lettersInWord = new ArrayList<>();
            int len = word.length();
            for (int i = 0; i < len; i++) {
                char c = word.charAt(i);
                if (!lettersInWord.contains(c)){
                    this.lettersInWord.add(c);
                }
            }
            System.out.println("lettersa in: "+ lettersInWord);
        }


        boolean checkForLoss(){
            if (lives < 0){
                for(int i = 0; i < players.size(); i++){
                    System.out.println("lost");
                    send("LOSE", players.get(i).clientIndex);
                    players.get(i).numPlayersChosen = 0;
                    players.get(i).difficultyChosen = "";
                    this.isActive = false;
                    gamesList.remove(this);
                }
                return true;
            }
            return false;
        }

    }
    // ***************************** End Game Class ********************** //
    // ******************************************************************* //

}
// ************************** End Server Class *********************** //
// ******************************************************************* //