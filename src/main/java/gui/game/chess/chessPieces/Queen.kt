package game.chess.chessPieces

import game.chess.ChessField
import game.chess.fieldSize

class Queen(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "queen"
    override val width = fieldSize / 1.3
    override val height = fieldSize / 1.3
    override val color = c

    override fun possibleFields(f: (ChessField) -> Unit) {
        possibleFieldsHelp(this, f,
            arrayOf<(Int) -> Int>({it+1}, {it+1}),
            arrayOf<(Int) -> Int>({it-1}, {it-1}),
            arrayOf<(Int) -> Int>({it+1}, {it-1}),
            arrayOf<(Int) -> Int>({it-1}, {it+1}),

            arrayOf<(Int) -> Int>({it+1}, {it+0}),
            arrayOf<(Int) -> Int>({it+0}, {it+1}),
            arrayOf<(Int) -> Int>({it-1}, {it-0}),
            arrayOf<(Int) -> Int>({it-0}, {it-1})
        )
    }
}