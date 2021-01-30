package game.chess.chessPieces

import game.chess.*

class Bishop(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "bishop"
    override val width = fieldSize / 1.5
    override val height = fieldSize / 1.5
    override val color = c

    override fun possibleFields(f: (ChessField) -> Unit) {
        possibleFieldsHelp(this, f,
                arrayOf<(Int) -> Int>({ it + 1 }, { it + 1 }),
                arrayOf<(Int) -> Int>({ it - 1 }, { it - 1 }),
                arrayOf<(Int) -> Int>({ it + 1 }, { it - 1 }),
                arrayOf<(Int) -> Int>({ it - 1 }, { it + 1 })
        )
    }
}