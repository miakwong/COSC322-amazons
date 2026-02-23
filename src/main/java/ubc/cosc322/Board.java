package ubc.cosc322;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// board is a 10 x 10 grid, keeping track of the state
// of the game.  
// Two make searches faster, we keep track of the White and
// Black queens.  
public class Board {

    public static final int SIZE = 10;

    private final int[][] grid;

    // keep track of the white and black queens
    // This is to improve the speed, because we do not
    // have to use for loops each time to find the queens.
    private final List<int[]> whiteQueens; // each int[]{r,c}
    private final List<int[]> blackQueens;

    // record for reversible moves
    // when we make changes to the board we
    // need to store the previous values so we can
    // revert back.  
    public static class MoveRecord {
        int prevFromVal;
        int prevToVal;
        int prevArrowVal;

        int queenIndex;      // index in queen list
        int prevQueenRow;
        int prevQueenCol;
    }

    // default starting board
    public Board() {
        grid = new int[SIZE][SIZE];
        whiteQueens = new ArrayList<>();
        blackQueens = new ArrayList<>();

        // white
        // row, column, player ID
        placeQueen(0, 3, 1);  
        placeQueen(0, 6, 1);
        placeQueen(3, 0, 1);
        placeQueen(3, 9, 1);

        // black
        // row, column, player ID
        placeQueen(6, 0, 2);
        placeQueen(6, 9, 2);
        placeQueen(9, 3, 2);
        placeQueen(9, 6, 2);
    }

    // from raw 121 list from the server
    public Board(ArrayList<Integer> raw121) {
        grid = new int[SIZE][SIZE];
        whiteQueens = new ArrayList<>();  // keep track of the white queen
        blackQueens = new ArrayList<>();  // keep track of the black queen
        transformListTo2D(raw121);
        rebuildQueenLists();
    }

    // copy constructor to create a new board from the existing
    // board
    public Board(Board other) {
        grid = new int[SIZE][SIZE];
        // duplicate the grid (other -> grid)
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(other.grid[r], 0, this.grid[r], 0, SIZE);
        }
        whiteQueens = new ArrayList<>();  // keep track of the white queen
        blackQueens = new ArrayList<>();  // keep track of the black queen
        
        // copy the white queens from other
        // note: whiteQueens is a list of int[]{r,c}
        for (int[] q : other.whiteQueens) {
            whiteQueens.add(new int[]{q[0], q[1]});
        }
        
        // copy the black queens from other
        // note: blackQueens is a list of int[]{r,c}
        for (int[] q : other.blackQueens) {
            blackQueens.add(new int[]{q[0], q[1]});
        }
    }

    // places a queen into the grid 
    // r = row, c = column and playerId is either 1 (white)  or 2 (black)
    private void placeQueen(int r, int c, int playerId) {
        grid[r][c] = playerId;
        if (playerId == 1) {
            whiteQueens.add(new int[]{r, c});
        } else if (playerId == 2) {
            blackQueens.add(new int[]{r, c});
        }
    }

    // rebuild the  whiteQueens and blackQueens list from the
    // grid.  This method needs to be called when placeQueen 
    // method is used when placing a queen manually.  
    private void rebuildQueenLists() {
        whiteQueens.clear();
        blackQueens.clear();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 1) whiteQueens.add(new int[]{r, c});
                else if (grid[r][c] == 2) blackQueens.add(new int[]{r, c});
            }
        }
    }

    
	 // The array of integers returned by amazonsGameMessages has a length of 121
	 // 0 = empty
	 // 1 = white queen
	 // 2 = black queen
	 // 3 = arrow
	 // It is actually a 11 x 11 board
	 // row 0 and column 11 are padded with 0s
	 // This board class removes the padding and shrinks it down to a 10 x 10 board
	 public void transformListTo2D(ArrayList<Integer> raw121) {
        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 10; c++) {
                int value = raw121.get(r * 11 + c);
                grid[r - 1][c - 1] = value;
            }
        }
    }

	 // transform the 10 x 10 board back to Raw121 for the
	 // server.  Add back the padding.
	 public ArrayList<Integer> toRaw121() {
		    ArrayList<Integer> raw121 = new ArrayList<>(121);

	    for (int r = 0; r < 11; r++) {
	        for (int c = 0; c < 11; c++) {

	            // Padding rows/columns (server uses 0 for padding)
	            if (r == 0 || r == 10 || c == 0 || c == 10) {
	                raw121.add(0);
	            } else {
	                // Map playable rows 1–10 → internal rows 0–9
	                raw121.add(grid[r - 1][c - 1]);
	            }
	        }
	    }

	    return raw121;
	}



    // reversible apply
    public MoveRecord applyMove(Move m, int playerId) {
    	// create a new MoveRecord
        MoveRecord rec = new MoveRecord();

        int r1 = m.qFromRow, c1 = m.qFromCol;
        int r2 = m.qToRow,   c2 = m.qToCol;
        int r3 = m.arrowRow, c3 = m.arrowCol;

        // save the previous move, before updating
        rec.prevFromVal  = grid[r1][c1];
        rec.prevToVal    = grid[r2][c2];
        rec.prevArrowVal = grid[r3][c3];

        // find the queen from the queen list of the board
        // to do this we iterate through the list and see if the
        // row and column matches the queen that is being moved.
        List<int[]> qList = (playerId == 1) ? whiteQueens : blackQueens;
        int idx = -1;  
        for (int i = 0; i < qList.size(); i++) {
            int[] q = qList.get(i);
            if (q[0] == r1 && q[1] == c1) {
                idx = i;
                break;
            }
        }
        
        // store the previous queen list index, row, and column
        rec.queenIndex   = idx;
        rec.prevQueenRow = r1;
        rec.prevQueenCol = c1;

        // apply the changes to the board
        grid[r1][c1] = 0;
        grid[r2][c2] = playerId;
        grid[r3][c3] = 3;

        // update the queen list of the board
        if (idx >= 0) {
            qList.set(idx, new int[]{r2, c2});
        }

        return rec;
    }

    // undo the move
    public void undoMove(Move m, int playerId, MoveRecord rec) {
        int r1 = m.qFromRow, c1 = m.qFromCol;
        int r2 = m.qToRow,   c2 = m.qToCol;
        int r3 = m.arrowRow, c3 = m.arrowCol;

        // restore the previous saved move values
        grid[r1][c1] = rec.prevFromVal;
        grid[r2][c2] = rec.prevToVal;
        grid[r3][c3] = rec.prevArrowVal;

        // restore the queen list value
        List<int[]> qList = (playerId == 1) ? whiteQueens : blackQueens;
        if (rec.queenIndex >= 0) {
            qList.set(rec.queenIndex, new int[]{rec.prevQueenRow, rec.prevQueenCol});
        }
    }

    // generate all moves for a player using queen lists
    public List<Move> generateAllMoves(int playerId) {
        List<Move> moves = new ArrayList<>();
        List<int[]> qList = (playerId == 1) ? whiteQueens : blackQueens;

        // for each queen in queen list get all the legal queen moves
        // arrow is also a queen move.
        for (int[] q : qList) {
            int qr = q[0];
            int qc = q[1];

            // create a list of all possible queen moves for one queen
            List<int[]> queenMoves = getLegalQueenMoves(qr, qc);

            // now we need to create all the possible moves of the arrow
            // for a queen.
            for (int[] dest : queenMoves) {
                int dr = dest[0];
                int dc = dest[1];

                // to generate arrow moves from the queen's new position
                // we need to temporarily move queen on grid to generate arrow moves
                int oldFrom = grid[qr][qc];
                int oldTo   = grid[dr][dc];

                // set the queen move
                grid[qr][qc] = 0;
                grid[dr][dc] = playerId;

                // Arrow moves share the same rule as queen moves
                List<int[]> arrows = getLegalQueenMoves(dr, dc);

                // restore the queen move
                grid[qr][qc] = oldFrom;
                grid[dr][dc] = oldTo;

                // now we have all moves for one queen with all
                // possible arrow moves.  This is large.  We save
                // all possible moves (states) into a list
                for (int[] arr : arrows) {
                    moves.add(new Move(
                            qr, qc,
                            dr, dc,
                            arr[0], arr[1]
                    ));
                }
            }
        }

        return moves;
    }

    // Simple heuristic to count the total moves of all white or black queens
    public int mobility(int playerId) {
        int count = 0;
        List<int[]> qList = (playerId == 1) ? whiteQueens : blackQueens;
        
        // for each queen in the queen list, we count every possible move
        // the queen can make.
        for (int[] q : qList) {
            count += getLegalQueenMoves(q[0], q[1]).size();
        }
        return count;
    }

    // returns a list of all the legal queen moves for a single queen.
    public List<int[]> getLegalQueenMoves(int r, int c) {
        List<int[]> moves = new ArrayList<>();
        int[] dirs = {-1, 0, 1};
        // directions: -1, -1 Down Left
        //			   -1,  0 Down
        //             -1,  1 Down Right
        //              0,  0 Ignore (continue)
        //              0, -1 Left
        //              0,  1 Right
        //              1, -1 Up Left
        //              1,  0 Up
        //              1,  1 Up Right

        for (int dr : dirs) {
            for (int dc : dirs) {
                if (dr == 0 && dc == 0) continue;

                int nr = r + dr;
                int nc = c + dc;

                while (inBounds(nr, nc) && grid[nr][nc] == 0) {
                    moves.add(new int[]{nr, nc});
                    nr += dr;
                    nc += dc;
                }
            }
        }
        return moves;
    }

    // check if the player has any moves left.
    // --todo-- can we improve on this.  May be it is OK
    public boolean hasAnyMove(int playerId) {
        return !generateAllMoves(playerId).isEmpty();
    }

    // check of the row and column is ouside the bounds of the board
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    // get what is in the row and column of the grid(board)
    public int get(int r, int c) {
        return grid[r][c];
    }
    
    // set the row and column of the grid(board) with a value
    public void set(int r, int c, int value) { 
    	grid[r][c] = value; 
    }

    // return a list of white queens
    public List<int[]> getWhiteQueens() { 
    	return Collections.unmodifiableList(whiteQueens); 
    } 
    
    // return a list of black queens
    public List<int[]> getBlackQueens() { 
    	return Collections.unmodifiableList(blackQueens);   	
    }
    
    // prints the board to screen that looks nice
    // color makes it easy on your eyes when playing
    // against the AI.
    public void printBoard() {
        final String RESET = "\u001B[0m";
        final String BLUE  = "\u001B[34;1m";
        final String RED   = "\u001B[31;1m";
        final String YEL   = "\u001B[33;1m";
        final String GRAY  = "\u001B[90m";

        System.out.println("***** Game Board *****");
        System.out.print("    ");
        for (int c = 0; c < SIZE; c++) System.out.printf("%2d ", c);
        System.out.println();
        System.out.print("    ");
        for (int c = 0; c < SIZE; c++) System.out.print("---");
        System.out.println();

        for (int r = 0; r < SIZE; r++) {
            System.out.printf("%2d | ", r);
            for (int c = 0; c < SIZE; c++) {
                int v = grid[r][c];
                String out;
                switch (v) {
                    case 1:  out = BLUE + "Q" + RESET; break;
                    case 2:  out = RED  + "Q" + RESET; break;
                    case 3:  out = YEL  + "X" + RESET; break;
                    default: out = GRAY + "." + RESET; break;
                }
                System.out.print(out + "  ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
