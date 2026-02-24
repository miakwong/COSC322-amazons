package ubc.cosc322;

// Simple move class
public final class Move {

    public final int qFromRow;
    public final int qFromCol;
    public final int qToRow;
    public final int qToCol;
    public final int arrowRow;
    public final int arrowCol;

    public double score;  // for move ordering

    public Move(int qFromRow, int qFromCol,
                int qToRow,   int qToCol,
                int arrowRow, int arrowCol) {

        this.qFromRow = qFromRow;
        this.qFromCol = qFromCol;
        this.qToRow   = qToRow;
        this.qToCol   = qToCol;
        this.arrowRow = arrowRow;
        this.arrowCol = arrowCol;
        this.score    = 0.0;
    }
}
