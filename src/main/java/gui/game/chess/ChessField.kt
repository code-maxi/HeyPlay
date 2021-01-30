package game.chess

import main.chess
import game.chess.chessPieces.ChessPiece
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import network.data.chess.ChessFieldData
import network.data.chess.GridPoint
import network.data.chess.Point

class ChessField(val color: String, val pp: GridPoint) {
    val p = Point(chess.borderSize + pp.x * fieldSize, chess.borderSize + pp.y * fieldSize)
    var possible = false
    var from = false
    var hover = false
    var rochade = false
    var lastmove = false

    var id: Int = 0

    fun data() = ChessFieldData(
        possible,
        from,
        hover,
        rochade,
        lastmove,
        id,
        piece?.data()
    )

    fun set(cf: ChessFieldData) {
        possible = cf.possible
        from = cf.from
        hover = cf.hover
        rochade = cf.rochade
        lastmove = cf.lastmove
        id = cf.id

        cf.piece?.let {
            piece = ChessPiece.newPiece(it, it.id)
            piece!!.set(it)
        } ?: run {
            piece = null
        }

        piece = piece
    }

    init {
        id = ID
        ID++
    }

    val center: Point
        get() {
            return Point(p.x + fieldSize / 2.0, p.y + fieldSize / 2.0)
        }

    var piece: ChessPiece? = null
        set(value) {
            field = value
            field?.let {
                if (!it.moving) {
                    it.pp = pp.copy()
                    it.p = center
                }
            }
        }

    fun contains(v: Point): Boolean {
        return v.x > p.x && v.y > p.y && v.x < p.x + fieldSize && v.y < p.y + fieldSize
    }

    fun rect(bottom: String) = arrayOf(if (bottom == "white") p.x else chess.getW2() - p.x, if (bottom == "white") p.y else chess.getH2() - p.y, fieldSize, fieldSize)

    fun draw(g2: GraphicsContext, bottom: String) {
        val rect = rect(bottom)

        if (possible) {
            if (color == "black") g2.fill = Color(50.0/255.0, 50.0/255.0, 50.0/255.0, 1.0)
            if (color == "white") g2.fill = Color(203.0/255.0, 217.0/255.0, 165.0/255.0, 1.0)
        }
        else {
            if (color == "white") g2.fill = white
            if (color == "black") g2.fill = black
        }

        g2.fillRect(rect[0], rect[1], rect[2], rect[3])

        if (hover) {
            g2.fill = Color(0.0/255.0, 119.0/255.0, 197.0/255.0, 60.0/255.0)
            g2.fillRect(rect[0], rect[1], rect[2], rect[3])
        }
        if (rochade) {
            g2.fill = Color(0.0/255.0, 119.0/255.0, 0.0/255.0, 40.0/255.0)
            g2.fillRect(rect[0], rect[1], rect[2], rect[3])
        }
        if (lastmove) {
            g2.fill = if (color == "white")
                Color(210.0/255.0, 250.0/255.0, 210.0/255.0, 1.0) else
                Color(60.0/255.0, 90.0/255.0, 60.0/255.0, 1.0)
            g2.fillRect(rect[0], rect[1], rect[2], rect[3])
        }
        if (piece != null && piece!!.schachPossble == true) {
            g2.fill = Color(255.0/255.0, 0.0/255.0, 0.0/255.0, 30.0/255.0)
            g2.fillRect(rect[0], rect[1], rect[2], rect[3])
        }
    }

    fun drawAfter(g2: GraphicsContext, bottom: String) {
        val rect = rect(bottom)
        if (from) {
            g2.fill = Color.YELLOW
            g2.lineWidth = 2.0
            g2.strokeRect(rect[0], rect[1], rect[2], rect[3])
        }
    }

    override fun toString(): String {
        return "$pp, $p"
    }

    companion object {
        var ID = 0
    }
}