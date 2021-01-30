package game.chess.chessPieces

import main.chess
import game.chess.*

class Knite(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "knite"
    override val width = fieldSize / 1.2
    override val height = fieldSize / 1.2
    override val color = c

    override fun possibleFields(ff: (ChessField) -> Unit) {
        fun pruefe(xx: Int, yy: Int) {
            val x = pp.x + xx
            val y = pp.y + yy
            if (x >= 0 && y >= 0 && x < W && y < H) {
                val f = chess.fields[x][y]
                if (f.piece == null || (f.piece != null && f.piece!!.color != color)) ff(f)
            }
        }

        pruefe(-1,  2)
        pruefe( 1,  2)
        pruefe( 2,  1)
        pruefe( 2, -1)
        pruefe( 1, -2)
        pruefe(-1, -2)
        pruefe(-2, -1)
        pruefe(-2,  1)
    }
}