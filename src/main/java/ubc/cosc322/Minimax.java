package ubc.cosc322;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Minimax {

    private final int myId;
    private final int opponentId;
    private int maxDepth;

    // Fixed transposition table: stores [score, depth] instead of just score
    // This prevents shallow results from incorrectly overriding deeper ones
    private Map<Long, double[]> tt = new HashMap<>();

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
        orderMoves(board, moves, myId);

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

        long hash = board.computeHash();

        // Fixed TT lookup: only reuse if stored result is from same or greater depth
        if (tt.containsKey(hash)) {
            double[] entry = tt.get(hash);
            if (entry[1] >= depth) {
                return entry[0];
            }
        }

        if (depth == 0) {
            double val = evaluate(board);
            tt.put(hash, new double[]{val, depth});
            return val;
        }

        int player = maximizing ? myId : opponentId;
        List<Move> moves = board.generateAllMoves(player);

        if (moves.isEmpty()) {
            double val = maximizing ? -999999 : 999999;
            tt.put(hash, new double[]{val, depth});
            return val;
        }

        orderMoves(board, moves, player);

        if (maximizing) {
            double best = Double.NEGATIVE_INFINITY;

            for (Move m : moves) {
                Board.MoveRecord rec = board.applyMove(m, player);

                double value = minimax(board, depth - 1, alpha, beta, false);

                board.undoMove(m, player, rec);

                best = Math.max(best, value);
                alpha = Math.max(alpha, best);

                if (beta <= alpha) break;
            }

            tt.put(hash, new double[]{best, depth});
            return best;

        } else {
            double best = Double.POSITIVE_INFINITY;

            for (Move m : moves) {
                Board.MoveRecord rec = board.applyMove(m, player);

                double value = minimax(board, depth - 1, alpha, beta, true);

                board.undoMove(m, player, rec);

                best = Math.min(best, value);
                beta = Math.min(beta, best);

                if (beta <= alpha) break;
            }

            tt.put(hash, new double[]{best, depth});
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

        // constrained queen bonus
        score += constrainedQueenBonus(board);

        // territory — increased weight from 0.7 to 1.2 (territory matters a lot in Amazons)
        score += 1.2 * territory(board);

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
            if (mob <= 1) score -= 8;
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
            m.score = 1.0 * mobility;

            // 2. Arrow impact — fixed to also consider free queens, not just constrained ones
            double arrowScore = arrowImpact(board, m, playerId);
            m.score += arrowScore * 1.5;

            // 3. Spacing penalty
            m.score += queenSpacingPenalty(board, m, playerId);

            // 4. Walling off potential
            double wallingOffScore = evaluateWallingMove(board, m, playerId);
            m.score += wallingOffScore;

            // small randomness to break ties
            m.score += Math.random() * 0.1;
        }

        moves.sort((a, b) -> Double.compare(b.score, a.score));

        // Keep only best moves (reduces branching)
        if (moves.size() > 40) {
            moves.subList(40, moves.size()).clear();
        }
    }

    private double evaluateWallingMove(Board board, Move m, int playerId) {
        Board.MoveRecord rec = board.applyMove(m, myId);

        List<Set<Integer>> territories = findConnectedBlocks(board);

        double totalScore = 0;

        for (Set<Integer> territory : territories) {
            int myQueens = countQueensInTerritory(board, territory, playerId);
            int oppQueens = countQueensInTerritory(board, territory, opponentId);
            int roomSize = territory.size();

            if (myQueens > 0 && oppQueens == 0) {
                // Our territory exclusively
                totalScore += roomSize * 1.5;
            } else if (oppQueens > 0 && myQueens == 0) {
                // Their territory exclusively
                totalScore -= roomSize * 1.5;
            } else {
                // Contested territory — use BFS distances to score
                int[] myDists = queenDistance(board, territory, playerId);
                int[] oppDists = queenDistance(board, territory, opponentId);
                double territoryScore = 0;
                for (int idx : territory) {
                    if (myDists[idx] < oppDists[idx]) {
                        territoryScore += 1.0;
                    } else if (oppDists[idx] < myDists[idx]) {
                        territoryScore -= 1.0;
                    }
                }
                totalScore += territoryScore;
            }
        }

        board.undoMove(m, myId, rec);

        return totalScore;
    }

    private int[] queenDistance(Board board, Set<Integer> territory, int playerId) {

        int[] distances = new int[100];
        Arrays.fill(distances, Integer.MAX_VALUE);

        Queue<Integer> queue = new LinkedList<>();
        List<int[]> queens = (playerId == 1) ? board.getWhiteQueens() : board.getBlackQueens();

        for (int[] q : queens) {
            int startIdx = q[0] * 10 + q[1];
            distances[startIdx] = 0;
            queue.add(startIdx);
        }

        while (!queue.isEmpty()) {
            int currIdx = queue.poll();
            int r = currIdx / 10;
            int c = currIdx % 10;
            int currentDist = distances[currIdx];

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    for (int step = 1; step < 10; step++) {
                        int nr = r + dr * step;
                        int nc = c + dc * step;
                        int nextIdx = nr * 10 + nc;
                        if (nr < 0 || nr >= 10 || nc < 0 || nc >= 10 || board.get(nr, nc) != 0) {
                            break;
                        }
                        if (!territory.contains(nextIdx)) continue;
                        if (distances[nextIdx] > currentDist + 1) {
                            distances[nextIdx] = currentDist + 1;
                            queue.add(nextIdx);
                        }
                    }
                }
            }
        }
        return distances;
    }

    private List<Set<Integer>> findConnectedBlocks(Board board) {
        List<Set<Integer>> territories = new ArrayList<>();
        boolean[] visited = new boolean[100];
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (board.get(r, c) == 0 && !visited[r * 10 + c]) {
                    Set<Integer> room = new HashSet<>();
                    dfs(r, c, board, visited, room);
                    territories.add(room);
                }
            }
        }
        return territories;
    }

    private void dfs(int r, int c, Board board, boolean[] visited, Set<Integer> room) {
        if (r < 0 || r >= 10 || c < 0 || c >= 10 || board.get(r, c) != 0 || visited[r * 10 + c]) {
            return;
        }
        visited[r * 10 + c] = true;
        room.add(r * 10 + c);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                dfs(r + dr, c + dc, board, visited, room);
            }
        }
    }

    private int countQueensInTerritory(Board board, Set<Integer> territory, int playerID) {
        int count = 0;
        List<int[]> queens = (playerID == 1) ? board.getWhiteQueens() : board.getBlackQueens();
        for (int[] q : queens) {
            boolean canTouchTerritory = false;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = q[0] + dr;
                    int nc = q[1] + dc;
                    if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && territory.contains(nr * 10 + nc)) {
                        canTouchTerritory = true;
                        break;
                    }
                }
                if (canTouchTerritory) break;
            }
            if (canTouchTerritory) count++;
        }
        return count;
    }

    private static int queenConstraint(Board board, Move m, int playerId) {

        int before = board.getLegalQueenMoves(m.qFromRow, m.qFromCol).size();

        int oldFrom = board.get(m.qFromRow, m.qFromCol);
        int oldTo   = board.get(m.qToRow,   m.qToCol);

        board.set(m.qFromRow, m.qFromCol, 0);
        board.set(m.qToRow,   m.qToCol,   playerId);

        int after = board.getLegalQueenMoves(m.qToRow, m.qToCol).size();

        board.set(m.qFromRow, m.qFromCol, oldFrom);
        board.set(m.qToRow,   m.qToCol,   oldTo);

        return (after - before);
    }

    /**
     * Fixed arrow impact: now considers ALL opponent queens, not just already-constrained ones.
     * This means we also reward arrows that START restricting free queens.
     */
    private double arrowImpact(Board board, Move m, int playerId) {

        int opponent = (playerId == 1 ? 2 : 1);

        List<int[]> oppQueens = (opponent == 1)
                ? board.getWhiteQueens()
                : board.getBlackQueens();

        // Mobility BEFORE placing the arrow
        int[] before = new int[oppQueens.size()];
        for (int i = 0; i < oppQueens.size(); i++) {
            int[] q = oppQueens.get(i);
            before[i] = board.getLegalQueenMoves(q[0], q[1]).size();
        }

        // Simulate full move + arrow
        Board.MoveRecord rec = board.applyMove(m, playerId);

        // Mobility AFTER placing the arrow
        int[] after = new int[oppQueens.size()];
        for (int i = 0; i < oppQueens.size(); i++) {
            int[] q = oppQueens.get(i);
            after[i] = board.getLegalQueenMoves(q[0], q[1]).size();
        }

        board.undoMove(m, playerId, rec);

        // Fixed: consider ALL queens, not just constrained ones
        // Weight the impact more heavily if the queen was already constrained
        double impact = 0;
        for (int i = 0; i < before.length; i++) {
            int reduction = before[i] - after[i];
            if (reduction <= 0) continue;

            if (before[i] <= 2) {
                // Already constrained queen — high value to restrict further
                impact += reduction * 2.0;
            } else {
                // Free queen — still worth restricting
                impact += reduction * 0.5;
            }
        }

        return impact;
    }

    public void setDepth(int newDepth) {
        this.maxDepth = newDepth;
    }
}