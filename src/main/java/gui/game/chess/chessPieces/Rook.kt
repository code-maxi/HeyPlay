package game.chess.chessPieces

import game.chess.*

class Rook(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "rook"
    override val width = fieldSize / 1.2
    override val height = fieldSize / 1.2
    override val color = c

    override fun possibleFields(f: (ChessField) -> Unit) {
        possibleFieldsHelp(this,  f,
            arrayOf<(Int) -> Int>({it+1}, {it+0}),
            arrayOf<(Int) -> Int>({it+0}, {it+1}),
            arrayOf<(Int) -> Int>({it-1}, {it-0}),
            arrayOf<(Int) -> Int>({it-0}, {it-1})
        )
    }
}