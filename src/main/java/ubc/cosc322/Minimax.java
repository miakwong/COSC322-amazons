package ubc.cosc322;

import java.util.List;

public class Minimax {

    private final int myId;
    private final int opponentId;
    private int maxDepth;

    public Minimax(int myId, int opponentId, int maxDepth) {
        this.myId = myId;
        this.opponentId = opponentId;
        this.maxDepth = maxDepth;
    }

    public Move findBestMove(Board board) {

        double alpha = Double.NEGATIVE_INFINITY;
        double beta  = Double.POSITIVE_INFINITY;

        double bestValue = Double.NEGATIVE_INFINITY;
        Move bestMove = null;

        List<Move> moves = board.generateAllMoves(myId);
        orderMoves(board, moves, myId);  // your existing heuristics

        for (Move m : moves) {
            Board.MoveRecord rec = board.applyMove(m, myId);

            double value = minimax(board, maxDepth - 1, alpha, beta, false);

            board.undoMove(m, myId, rec);

            if (value > bestValue) {
                bestValue = value;
                bestMove = m;
            }

            alpha = Math.max(alpha, bestValue);
        }

        return bestMove;
    }

    private double minimax(Board board, int depth,
                           double alpha, double beta,
                           boolean maximizing) {

        if (depth == 0) {
        	// modify the function evaluate to fine tune the heuristics
        	// for minimax.
            return evaluate(board);
        }

        int player = maximizing ? myId : opponentId;
        List<Move> moves = board.generateAllMoves(player);

        if (moves.isEmpty()) {
            return maximizing ? -999999 : 999999;
        }

        orderMoves(board, moves, player);

        if (maximizing) {
            double best = Double.NEGATIVE_INFINITY;

            for (Move m : moves) {
                Board.MoveRecord rec = board.applyMove(m, player);

                double value = minimax(board, depth - 1, alpha, beta, false);  //false not maximizing

                board.undoMove(m, player, rec);

                best = Math.max(best, value);
                alpha = Math.max(alpha, best);

                if (beta <= alpha) break;
            }

            return best;

        } else {
            double best = Double.POSITIVE_INFINITY;

            for (Move m : moves) {
                Board.MoveRecord rec = board.applyMove(m, player);

                double value = minimax(board, depth - 1, alpha, beta, true);  //true maximizing

                board.undoMove(m, player, rec);

                best = Math.min(best, value);
                beta = Math.min(beta, best);

                if (beta <= alpha) break;
            }

            return best;
        }
    }

    // Evaluate the terminating node
    private double evaluate(Board board) {
        // mobility difference
        int myMob  = board.mobility(myId);
        int oppMob = board.mobility(opponentId);
        double score = myMob - oppMob;
        
        score += constrainedQueenBonus(board);
        
        return score;
    }

    /**
     * Queens with fewer moves get higher priority.
     */
    private int constrainedQueenBonus(Board board) {

        int opponent = (myId == 1 ? 2 : 1);

        List<int[]> myQueens = (myId == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        List<int[]> oppQueens = (opponent == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        int score = 0;

        // Penalize constrained friendly queens
        for (int[] q : myQueens) {
            int mob = board.getLegalQueenMoves(q[0], q[1]).size();
            if (mob <= 1) score -= 8;   // nearly trapped
            else if (mob <= 2) score -= 4;
            else if (mob <= 3) score -= 2;
        }

        // Reward constrained enemy queens
        for (int[] q : oppQueens) {
            int mob = board.getLegalQueenMoves(q[0], q[1]).size();
            if (mob <= 1) score += 8;
            else if (mob <= 2) score += 4;
            else if (mob <= 3) score += 2;
        }

        return score;
    }

    /**
     * Move ordering:
     * - freeing constrained queens (moderate weight)
     * - arrows that restrict opponent queens (dominant)
     * - arrows that help my queen (small weight)
     */
    private void orderMoves(Board board, List<Move> moves, int playerId) {
    	for (Move m : moves) {
    		// 1. Constrained queen heuristic
            int mobility = queenConstraint(board, m, playerId); 
    		m.score =  1.0 * mobility;  // give it some weight trial and error
    		
    		// 2. Arrow heuristic: punish opponent's weakest queen (dominant)
            double arrowScore = arrowImpact(board, m, playerId);
            m.score += arrowScore * 1.5;  // give it some weight trial and error
    	} 
    	
    	
    	moves.sort((a, b) -> Double.compare(b.score, a.score));
    }
    
    // -------------------------
    //  QUEEN CONSTRAINT HEURISTIC
    // -------------------------
    private static int queenConstraint(Board board, Move m, int playerId) {

        // mobility BEFORE moving
        int before = board.getLegalQueenMoves(m.qFromRow, m.qFromCol).size();

        // simulate queen move (no arrow)
        int oldFrom = board.get(m.qFromRow, m.qFromCol);
        int oldTo   = board.get(m.qToRow,   m.qToCol);

        board.set(m.qFromRow, m.qFromCol, 0);
        board.set(m.qToRow,   m.qToCol,   playerId);

        // mobility AFTER moving
        int after = board.getLegalQueenMoves(m.qToRow, m.qToCol).size();

        // restore
        board.set(m.qFromRow, m.qFromCol, oldFrom);
        board.set(m.qToRow,   m.qToCol,   oldTo);

        // prefer moves that increase mobility
        return (after - before);
    }

    /**
     * Arrow heuristic:
     * Only reward arrows that reduce mobility of opponent queens
     * that already have ≤ 2 legal moves.
     */
    private double arrowImpact(Board board, Move m, int playerId) {

        int opponent = (playerId == 1 ? 2 : 1);

        // Use the board’s queen lists instead of scanning the whole grid
        List<int[]> oppQueens = (opponent == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        // Mobility BEFORE placing the arrow
        int[] before = new int[oppQueens.size()];
        for (int i = 0; i < oppQueens.size(); i++) {
            int[] q = oppQueens.get(i);
            before[i] = board.getLegalQueenMoves(q[0], q[1]).size();
        }

        // Simulate arrow placement
        int ar = m.arrowRow;
        int ac = m.arrowCol;

        int old = board.get(ar, ac);
        board.set(ar, ac, 3);   // temporarily place arrow

        // Mobility AFTER placing the arrow
        int[] after = new int[oppQueens.size()];
        for (int i = 0; i < oppQueens.size(); i++) {
            int[] q = oppQueens.get(i);
            after[i] = board.getLegalQueenMoves(q[0], q[1]).size();
        }

        // Undo temporary arrow
        board.set(ar, ac, old);

        // Compute impact
        double impact = 0;
        for (int i = 0; i < before.length; i++) {

            // Only consider queens that were already constrained
            if (before[i] > 2) continue;

            impact = Math.max(impact, before[i] - after[i]);
        }

        return impact;
    }

    // set the depth of the minimax search
    public void setDepth(int newDepth) { 
    	this.maxDepth = newDepth; 
    }
}
