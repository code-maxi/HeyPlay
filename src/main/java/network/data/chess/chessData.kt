package network.data.chess

import network.data.Online
import java.io.Serializable

data class ChessData(
    val id: Int,
    val move: String,
    val fields: Array<Array<ChessFieldData>>
) : Serializable

data class ChessFieldData(
    val possible: Boolean,
    val from: Boolean,
    val hover: Boolean,
    val rochade: Boolean,
    val lastmove: Boolean,
    val id: Int,
    val piece: ChessPieceData?
) : Serializable

data class ChessOnlines(val white: Online?, val black: Online?, val onlines: Array<Online>) : Serializable
data class ChessPieceData(
    val color: String,
    val type: String,
    val possibleFieldsArray: Array<GridPoint>?,
    val schachPossble: Boolean?,
    val moving: Boolean,
    val moved: Int,
    val id: Int,
    val pp: GridPoint,
    val p: Point
) : Serializable

data class GridPoint(val x: Int, val y: Int) : Serializable {
    operator fun minus(that: GridPoint): GridPoint = GridPoint(x - that.x, y - that.y)
    operator fun plus(that: GridPoint): GridPoint = GridPoint(x + that.x, y + that.y)
    operator fun times(s: Int) = GridPoint(x * s, y * s)
}

data class Point(var x: Double = 0.0, var y: Double = 0.0) : Serializable