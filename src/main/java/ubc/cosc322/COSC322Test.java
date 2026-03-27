
package ubc.cosc322;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;
//import ygraph.ai.smartfox.games.amazons.HumanPlayer;



/**
 * An example illustrating how to implement a GamePlayer
 * @author Yong Gao (yong.gao@ubc.ca)
 * Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer{

	private static final boolean LOCAL_MODE = false;   // toggle local mode
	private static final boolean AIVSAI_MODE = false;  // toggle AI vs AI mode

    private GameClient gameClient = null; 
    private BaseGameGUI gamegui = null;
	
    private String userName = null;
    private String passwd = null;
 
	private int playerId;    // 1 for black, 2 for white
	private int opponentId;  // 1 for black, 2 for white
	
	private Board board;     // the game board
	private Minimax myAI;    // Minimax AI
	
	// some variable to used by Minimax
	int moveCount = 0;     // count the number of turn
	
    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {
    	if (LOCAL_MODE) {
            COSC322Test local = new COSC322Test("local", "local");
            local.runHumanVsAI();
            return;
        } else if (AIVSAI_MODE){
        	COSC322Test local = new COSC322Test("aivsai", "aivsai");
            local.runAIVsAI();
            return;
        }
    	
    	
    	// lets play against the server
    	System.out.println(args[0]);
    	System.out.println(args[1]);
    	
    	COSC322Test player = new COSC322Test(args[0], args[1]);
    	if(player.getGameGUI() == null) {
    		System.out.println("Lets Play");
    	    
    		player.Go();
    	}
    	else {
    		System.out.println("BaseGameGUI system setup");
    	    
    		BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                	player.Go();
                }
            });
    		}
    }
	
    /**
     * Any name and passwd 
     * @param userName
      * @param passwd
     */
    public COSC322Test(String userName, String passwd) {
    	this.userName = userName;
    	this.passwd = passwd;
    	
    	//To make a GUI-based player, create an instance of BaseGameGUI
    	//and implement the method getGameGUI() accordingly
    	System.out.println("Setting gamegui");
    	this.gamegui = new BaseGameGUI(this);
    }
 
    //****************** local move
    private void runHumanVsAI() {
        System.out.println("=== HUMAN vs AI MODE (LOCAL) ===");

        board = new Board();          
        Minimax ai = new Minimax(2, 1, 3);   // AI = player 2, human = player 1
        int currentPlayer = 1;               // human starts

        Scanner sc = new Scanner(System.in);
        int moveCount = 0;

        while (true) {

            System.out.println("\nCurrent board:");
            board.printBoard();

            if (!board.hasAnyMove(currentPlayer)) {
                System.out.println("Player " + currentPlayer + " has no moves. Game over.");
                break;
            }

            Move chosen;

            if (currentPlayer == 1) {
                // HUMAN TURN
                System.out.println("Your turn " + moveCount + " (Player 1, BLUE).");
                System.out.println("Enter move as: qR qC nR nC aR aC");

                while (true) {
                    try {
                        int qR = sc.nextInt();
                        int qC = sc.nextInt();
                        int nR = sc.nextInt();
                        int nC = sc.nextInt();
                        int aR = sc.nextInt();
                        int aC = sc.nextInt();

                        Move humanMove = new Move(qR, qC, nR, nC, aR, aC);

                        // Validate by checking if it's in the legal move list
                        // --todo-- 
                        // might want to look into a better way to check if
                        // humanMove is valid.  board.generateAllMoves is expensive.
                        // but it is not part of the project anyways.  For testing.
                        boolean legal = false;
                        for (Move m : board.generateAllMoves(currentPlayer)) {
                            if (movesEqual(m, humanMove)) {
                                legal = true;
                                break;
                            }
                        }

                        if (legal) {
                            chosen = humanMove;
                            break;
                        } else {
                            System.out.println("Illegal move. Try again.");
                        }

                    } catch (Exception e) {
                        System.out.println("Invalid input. Try again.");
                        sc.nextLine();
                    }
                }

            } else {
                // AI TURN
                System.out.println("AI (Player 2) is thinking...");

                int depth;
                if (moveCount < 10) depth = 2;
                else if (moveCount < 30) depth = 3;
                else depth = 4;

                ai.setDepth(depth);
                chosen = ai.findBestMove(board);

                System.out.println(
                    "AI plays: (" +
                    chosen.qFromRow + "," + chosen.qFromCol + ") -> (" +
                    chosen.qToRow   + "," + chosen.qToCol   + ")  arrow (" +
                    chosen.arrowRow + "," + chosen.arrowCol + ")"
                );
            }

            // Apply move using new API
            board.applyMove(chosen, currentPlayer);

            moveCount++;
            currentPlayer = (currentPlayer == 1 ? 2 : 1);
        }

        sc.close();
    }

    private boolean movesEqual(Move a, Move b) {
        return a.qFromRow == b.qFromRow &&
               a.qFromCol == b.qFromCol &&
               a.qToRow   == b.qToRow   &&
               a.qToCol   == b.qToCol   &&
               a.arrowRow == b.arrowRow &&
               a.arrowCol == b.arrowCol;
    }
   
    //********************************

    
    //***************** AI vs AI *****
    private void runAIVsAI() {
        System.out.println("=== AI vs AI MODE (LOCAL) ===");

        board = new Board();

        // AI1 = Player 1 (Blue)
        Minimax ai1 = new Minimax(1, 2, 2);

        // AI2 = Player 2 (Red)
        Minimax ai2 = new Minimax(2, 1, 2);

        long gameStart = System.currentTimeMillis(); 
        long longestMove = 0; 
        long totalMoveTime = 0;
        
        int currentPlayer = 1;
        int moveCount = 0;
        
      
        while (true) {

            System.out.println("\nMove #" + moveCount);
            board.printBoard();

            if (!board.hasAnyMove(currentPlayer)) {
                System.out.println("Player " + currentPlayer + " has no moves. Game over.");
                break;
            }

            Move chosen;
            
            long start = System.currentTimeMillis();
            
            if (currentPlayer == 1) {
                // Dynamic depth for AI1
                int depth;
                if (moveCount < 10) depth = 2;
                else if (moveCount < 30) depth = 3;
                else if (moveCount < 40) depth = 4;
                else depth = 5;

                ai1.setDepth(depth);
                System.out.println("AI1 (Player 1, BLUE) thinking at depth " + depth + "...");
                chosen = ai1.findBestMove(board);

            } else {
                // Dynamic depth for AI2
            	int depth;
                if (moveCount < 10) depth = 2;
                else if (moveCount < 30) depth = 3;
                else if (moveCount < 40) depth = 4;
                else depth = 5;

                ai2.setDepth(depth);
                System.out.println("AI2 (Player 2, RED) thinking at depth " + depth + "...");
                chosen = ai2.findBestMove(board);
            }
            
            long end = System.currentTimeMillis(); 
            long moveTime = end - start;
            
            totalMoveTime += moveTime; 
            longestMove = Math.max(longestMove, moveTime);
            
            System.out.println(
            	    "AI " + currentPlayer + " plays: (" +
            	    chosen.qFromRow + "," + chosen.qFromCol + ") -> (" +
            	    chosen.qToRow   + "," + chosen.qToCol   + ")  arrow (" +
            	    chosen.arrowRow + "," + chosen.arrowCol + ")"
            	);

            	System.out.println("Move time: " + moveTime + " ms");
            
            board.applyMove(chosen, currentPlayer);
            
            moveCount++;
            // switch currentPlayer to the next players turn
            currentPlayer = (currentPlayer == 1 ? 2 : 1);
        }
        
        long gameEnd = System.currentTimeMillis();
        long totalGameTime = gameEnd - gameStart;

        double totalSeconds = totalGameTime / 1000.0;
        double avgSeconds = (moveCount > 0 ? (totalMoveTime / 1000.0) / moveCount : 0);
        double longestSeconds = longestMove / 1000.0;

        System.out.println("\n=== GAME OVER ===");
        System.out.println("Total moves: " + moveCount);
        System.out.println("Total game time: " + String.format("%.2f seconds", totalSeconds));
        System.out.println("Average move time: " + String.format("%.2f seconds", avgSeconds));
        System.out.println("Longest move time: " + String.format("%.2f seconds", longestSeconds));

    }

    //********************************
    
    // Play against the Sercer
    @Override
    public void onLogin() {
    	System.out.println("Congratualations!!! "
    			+ "I am called because the server indicated that the login is successfully");
    	System.out.println("The next step is to find a room and join it: "
    			+ "the gameClient instance created in my constructor knows how!"); 
    	
    	this.userName = gameClient.getUserName();
    	System.out.println("User Name = "+ this.userName);
    	if (gamegui != null) {
    		gamegui.setRoomInformation(gameClient.getRoomList());
        }
        
        List<Room> rooms = this.gameClient.getRoomList();
        for (Room room : rooms) {
            System.out.println(room);
        }
        this.gameClient.joinRoom(rooms.get(0).getName());
        gameClient.sendTextMessage("hello");
    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
    	//This method will be called by the GameClient when it receives a game-related message
    	//from the server.
	
    	//For a detailed description of the message types and format, 
    	//see the method GamePlayer.handleGameMessage() in the game-client-api document. 
    	 
    	Move myMove = null;
    	System.out.println("handleGameMsg_____");
        System.out.println(messageType);

        switch (messageType) {
            case(GameMessage.GAME_STATE_BOARD):
                System.out.println("Get the current state of the game");
            
            	// A 1D ArrayList of 121 integers
            	// 0 = empty
            	// 1 = white queen
            	// 2 = black queen
            	// 3 = arrow
                // the size of the array is 121.  11 x 11 board
                // row 0 and column 11 are padding
            	ArrayList<Integer> gbCurrentState = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
            	System.out.println("*** gbCurrentList: " + gbCurrentState);
            	
            	// update the gui with the game board
            	this.getGameGUI().setGameState(gbCurrentState);
            	
            	// Only initialize board ONCE 
            	if (board == null)
            		board = new Board(gbCurrentState); 
            	else
            		board.printBoard(); 
            	
            	break;
            case (GameMessage.GAME_ACTION_MOVE):
                System.out.println("Game Move");

                // 1. Extract opponent move (server uses 1-based indexing)
                ArrayList<Integer> queenCurrentPos = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
                ArrayList<Integer> queenNextPos    = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
                ArrayList<Integer> arrowPos        = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);

                // convert what we get from the server to something 
                // that our AI can understand
                int qR  = queenCurrentPos.get(0) - 1;
                int qC  = queenCurrentPos.get(1) - 1;
                int nR  = queenNextPos.get(0)    - 1;
                int nC  = queenNextPos.get(1)    - 1;
                int aR  = arrowPos.get(0)        - 1;
                int aC  = arrowPos.get(1)        - 1;

                // Debug print
                System.out.println("***** Opposition Move *****");
                System.out.println("Current Queen: (" + qR + "," + qC + ")");
                System.out.println("Next Queen:    (" + nR + "," + nC + ")");
                System.out.println("Arrow:         (" + aR + "," + aC + ")");
                System.out.println("***************************");

                // 2. Build Move object using your new Move class
                Move oppMove = new Move(qR, qC, nR, nC, aR, aC);

                // --todo--
                // do we need to verify opponents move
                
                // 3. Apply opponent move to your board
                board.applyMove(oppMove, opponentId);

                // 4. Update GUI with opponent move (1-based indexing) 
                gamegui.updateGameState( new ArrayList<>(List.of(qR+1, qC+1)), 
                		new ArrayList<>(List.of(nR+1, nC+1)), 
                		new ArrayList<>(List.of(aR+1, aC+1)) );
                
                
                // 5. Check if I have any moves left 
                if (!board.hasAnyMove(playerId)) {
                    System.out.println("No moves left -- I Lose.");
                    return true;
                }
                
                int depth;
                if (moveCount < 10) depth = 2;
                else if (moveCount < 30) depth = 3;
                else if (moveCount < 40) depth = 4;
                else depth = 5;

                myAI.setDepth(depth);
                
                // 5. Now generate your move and send it
                myMove = myAI.findBestMove(board);
                moveCount++;

                // 6. Send my move to the server and update the GUI
                sendMoveToServerAndGui(myMove);
        		
        		// 7. Update internal board 
                board.applyMove(myMove, playerId);	
                
                // 8. Check if the opponent has any moves left
                if (!board.hasAnyMove(opponentId)) {
                    System.out.println("You have no more moves left -- I Win.");
                    return true;
                }

                break;

            case (GameMessage.GAME_ACTION_START):
            	System.out.println("$$$$$Game Start");
            	String playerBlack = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
            	String playerWhite = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
           
            	
            	// print the color of the user
            	// Print out which teams we are on
            	System.out.println("Black: " + playerBlack);
            	System.out.println("White: " + playerWhite);
            	
            	// check which color we are playing
            	// we do this by checking our userName
            	if (playerBlack.equals(this.userName)) {
            		playerId = 1;  // black = 1
            		opponentId = 2;  // white = 2
            	}
            	else {
            		opponentId = 1;
            		playerId = 2;
            	}
            	
            	// Initialize local board state before any moves 
            	//board = new Board();
            	//gamegui.setGameState(board.toRaw121());
            	
            	// Initialize AI with correct IDs (you can tune initial depth) 
            	myAI = new Minimax(playerId, opponentId, 2);  // depth default is 2
                
                
            	// Black moves first
            	if (playerId == 1) {
            		System.out.println("Here I go first...");
            		
            		// Make my first move
            		myMove = myAI.findBestMove(board);
            		moveCount++;
            		
            		sendMoveToServerAndGui(myMove);
            		
            		// --todo--
            		// after you send Moves to server the server will send 
            		// message GAME_STATE_BOARD, so do not need to update
            		// our copy of the board
            		// Update internal board 
            		//board.applyMove(myMove, playerId);
            		
            		//board.printBoard();
            		
            	} else {
            		System.out.println("I go after you...");
            		// White waits for opponent's first move (GAME_ACTION_MOVE), 
            		// then a new GAME_STATE_BOARD, then moves.
            	}
            		
                break;
            default:
                break;
        }
    	return true;   	
    }
    
    
    @Override
    public String userName() {
    	return userName;
    }

	@Override
	public GameClient getGameClient() {
		// TODO Auto-generated method stub
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
		// TODO Auto-generated method stub
		return this.gamegui;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
    	gameClient = new GameClient(userName, passwd, this);			
	}

 
	
	public static void printBoard1Dto2D(ArrayList<Integer> board1D) { 
		System.out.println("board size = " + board1D.size());
		if (board1D == null || board1D.size() != 100) { 
			System.out.println("Invalid board: must contain 100 elements."); 
			return; 
		} 
		for (int row = 0; row < 10; row++) { 
			for (int col = 0; col < 10; col++) { 
				int value = board1D.get(row * 10 + col); 
				System.out.print(value + " "); 
			} 
			System.out.println(); 
		}
	}
	
	
	private void sendMoveToServerAndGui(Move m) { 
		// Convert back to 1-based indexing for server
        ArrayList<Integer> myQueenCurr = new ArrayList<>(List.of(m.qFromRow + 1, m.qFromCol + 1));
        ArrayList<Integer> myQueenNext = new ArrayList<>(List.of(m.qToRow   + 1, m.qToCol   + 1));
        ArrayList<Integer> myArrow     = new ArrayList<>(List.of(m.arrowRow + 1, m.arrowCol + 1));

		// Send move to server 
		gameClient.sendMoveMessage(myQueenCurr, myQueenNext, myArrow);
		
		// Update server GUI 
		gamegui.updateGameState(myQueenCurr, myQueenNext, myArrow); 
		
	}
}//end of class
