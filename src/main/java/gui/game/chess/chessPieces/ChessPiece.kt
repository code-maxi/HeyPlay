package game.chess.chessPieces

import main.chess
import game.chess.*
import network.data.chess.Point
import main.Start.Companion.picefigures
import javafx.scene.canvas.GraphicsContext
import network.data.chess.ChessPieceData
import network.data.chess.GridPoint
abstract class ChessPiece(iid: Int? = null) {
    abstract val type: String
    abstract val width: Double
    abstract val height: Double
    abstract val color: String
    abstract fun possibleFields(f: (ChessField) -> Unit)

    var possibleFieldsArray: Array<GridPoint>? = null
    var schachPossble: Boolean? = null

    var moved = 0
    var moving = false

    var id: Int = 0

    fun data() = ChessPieceData(
        color,
        type,
        possibleFieldsArray,
        schachPossble,
        moving,
        moved,
        id,
        pp.copy(),
        p.copy()
    )

    fun set(c: ChessPieceData) {
        possibleFieldsArray = c.possibleFieldsArray
        schachPossble = c.schachPossble
        moving = c.moving
        moved = c.moved
        id = c.id
        pp = c.pp.copy()
        p = c.p.copy()
    }

    fun possible(f: (ChessField) -> Unit) {
        if (possibleFieldsArray == null) possibleFields(f)
        else {
            possibleFieldsArray!!.forEach { f(chess.fields[it.x][it.y]) }
        }
    }

    init {
        if (iid == null) {
            id = ID
            ID++
        }
        else {
            id = iid
        }
        chessPieces += id to this
    }

    var pp = GridPoint(0, 0)
        set(value) {
            field = value
            if (!moving) p = Point(chess.borderSize + field.x * fieldSize, chess.borderSize + field.y * fieldSize)
        }

    var p = Point(0.0, 0.0)

    fun p() = getMirrorPoint(p)

    fun draw(g2: GraphicsContext) {
        val pos = p()
        g2.drawImage(picefigures["$type $color"], (pos.x - width/2.0), (pos.y - height/2.0), width, height)
    }

    override fun toString(): String {
        val pos = p()
        return "$type: ${pp.x} ${pp.y} | ${pos.x} ${pos.y}"
    }

    companion object {
        fun getMirrorPoint(point: Point): Point {
            val x = if (chess.bottom == "white") point.x else chess.getW() - point.x
            val y = if (chess.bottom == "white") point.y else chess.getH() - point.y
            return Point(x, y)
        }

        fun newPiece(cd: ChessPieceData, iid: Int?) = when (cd.type) {
            "queen" -> Queen(cd.color, iid)
            "rook" -> Rook(cd.color, iid)
            "bishop" -> Bishop(cd.color, iid)
            "king" -> King(cd.color, iid)
            "knite" -> Knite(cd.color, iid)
            "pawn" -> Pawn(cd.color, iid)
            else -> Pawn(cd.color, iid)
        }


        var ID = 0

        var chessPieces: Map<Int, ChessPiece> = mapOf()

        fun possibleFieldsHelp(c: ChessPiece, f: (ChessField) -> Unit, vararg ww: Array<(Int) -> Int>) {
            c.apply {
                var x = pp.x
                var y = pp.y

                var bool = 0
                var bool2 = false
                var bool3 = true

                fun itself(): Boolean = x == pp.x && y == pp.y
                fun b1(): Boolean {
                    return x >= 0 && y >= 0 && x < W && y < H
                }
                fun b2(): Boolean {
                    val i = itself()
                    var bool4 = false
                    chess.fields[x][y].piece?.let {
                        if (it.pp != pp) bool2 = true
                        if (!i) bool3 = it.color != color
                        if (it is King && it.color != color) bool4 = true
                    }
                    if (bool2) bool ++
                    return ((i || (!i && bool <= 1)) && bool3) || bool4
                }
                fun w(fx: (Int) -> Int, fy: (Int) -> Int) {
                    x = pp.x
                    y = pp.y
                    bool = 0
                    bool2 = false
                    bool3 = true

                    while(b1() && b2()) {
                        f(chess.fields[x][y])
                        x = fx(x)
                        y = fy(y)
                    }
                }

                ww.forEach {
                    w(it[0], it[1])
                }
            }
        }
    }
}

