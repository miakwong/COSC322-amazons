package ubc.cosc322;

import java.util.List;

public class Minimax {

    private final int myId;
    private final int opponentId;
    private int maxDepth;
    private Map<Long, Double> tt = new HashMap<>();

    public Minimax(int myId, int opponentId, int maxDepth) {
        this.myId = myId;
        this.opponentId = opponentId;
        this.maxDepth = maxDepth;
    }

    public Move findBestMoveIterative(Board board, long timeLimitMs) {
        long endTime = System.currentTimeMillis() + timeLimitMs;
        Move bestMove = null;

        for (int depth = 1; depth <= 10; depth++) {
            if (System.currentTimeMillis() >= endTime) break;

            this.maxDepth = depth;
            Move move = findBestMove(board);

            if (move != null) {
                bestMove = move;
            }
        }

        return bestMove;
    }


    private double queenSpacingPenalty(Board board, Move m, int playerId) {

        List<int[]> myQueens = (playerId == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        int newR = m.qToRow;
        int newC = m.qToCol;

        double score = 0;

        for (int[] q : myQueens) {

            // skip the queen we are moving
            if (q[0] == m.qFromRow && q[1] == m.qFromCol) continue;

            int dist = Math.max(Math.abs(q[0] - newR), Math.abs(q[1] - newC));

            // penalties (too close)
            if (dist <= 1) score -= 15;
            else if (dist == 2) score -= 8;
            else if (dist == 3) score -= 3;

            // reward (far apart)
            else if (dist >= 5) score += 2;
        }

        return score;
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
                bestValue = value;ci
                bestMove = m;
            }

            alpha = Math.max(alpha, bestValue);
        }

        return bestMove;
    }

    private double minimax(Board board, int depth,
                           double alpha, double beta,
                           boolean maximizing) {


        long hash = board.computeHash();

        if (tt.containsKey(hash)) {
            return tt.get(hash);
        }

        if (depth == 0) {
        	// modify the function evaluate to fine tune the heuristics
        	// for minimax.
            double val = evaluate(board);
            tt.put(hash, val);   // store before returning
            return val;
        }

        int player = maximizing ? myId : opponentId;
        List<Move> moves = board.generateAllMoves(player);

        if (moves.isEmpty()) {
            double val = maximizing ? -999999 : 999999;
            tt.put(hash, val);   // store before returning
            return val;
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

            tt.put(hash, best);
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
            tt.put(hash, best);
            return best;
        }
    }

    private int territory(Board board) {
        int score = 0;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {

                if (board.get(r, c) != 0) continue;

                int myDist  = closestQueenDist(board, r, c, myId);
                int oppDist = closestQueenDist(board, r, c, opponentId);

                if (myDist < oppDist) score++;
                else if (oppDist < myDist) score--;
            }
        }

        return score;
    }
    
    private int closestQueenDist(Board board, int r, int c, int playerId) {

        List<int[]> queens = (playerId == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        int min = Integer.MAX_VALUE;

        for (int[] q : queens) {
            int dist = Math.max(Math.abs(q[0] - r), Math.abs(q[1] - c));
            min = Math.min(min, dist);
        }

        return min;
    }

    // Evaluate the terminating node
    private double evaluate(Board board) {
        // mobility difference
        int myMob  = board.mobility(myId);
        int oppMob = board.mobility(opponentId);
        double score = myMob - oppMob;

        // existing heuristic
        score += constrainedQueenBonus(board);

        // territory
        score += 0.7 * territory(board);

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
    		// 1. mobility improvement
            int mobility = queenConstraint(board, m, playerId); 
            m.score = 1.0 * mobility;

            // 2. arrow impact
            double arrowScore = arrowImpact(board, m, playerId);
            m.score += arrowScore * 1.5;

            // 3. Spacing penalts
            m.score += queenSpacingPenalty(board, m, playerId);

            // (optional) small randomness
            m.score += Math.random() * 0.1;
    	} 
    	
    	
    	moves.sort((a, b) -> Double.compare(b.score, a.score));

        // Keep only best moves (reduces branching)
        if (moves.size() > 40) {
            moves.subList(40, moves.size()).clear();
        }
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
