package game.chess.chessPieces

import main.chess
import game.chess.ChessField
import game.chess.H
import game.chess.W
import game.chess.fieldSize

class Pawn(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "pawn"
    override val width = fieldSize / 1.3
    override val height = fieldSize / 1.3
    override val color = c

    override fun possibleFields(ff: (ChessField) -> Unit) {
        fun pruefe(x: Int, y: Int, fff: (ChessField) -> Boolean) {
            if (x >= 0 && y >= 0 && x < W && y < H) {
                val f = chess.fields[x][y]
                if (fff(f)) ff(f)
            }
        }

        val g = if (color == "white") 1 else -1

        if (moved == 0) {
            if (
                pp.x > 0 && pp.x < W-1 &&
                pp.y > 0 && pp.y < H-1 &&
                chess.fields[pp.x][pp.y-g].piece == null
            ) pruefe(pp.x, pp.y-g*2) { it.piece == null }
        }

        fun s1(f: ChessField) = f.piece != null && f.piece!!.color != color
        fun s2(f1: ChessField, f2: ChessField) =
                f1.piece == null &&
                s1(f2) && f2.piece!!.moved == 1 &&
                ((f2.piece!!.color == "black" && f2.piece!!.pp.y == 3) ||
                (f2.piece!!.color == "white" && f2.piece!!.pp.y == 4))

        fun s(f: ChessField) =
                s1(f) ||
                s2(f, chess.fields[f.pp.x][pp.y])

        pruefe(pp.x, pp.y-g) { it.piece == null }
        pruefe(pp.x-1, pp.y-g) { s(it) }
        pruefe(pp.x+1, pp.y-g) { s(it) }
    }
}