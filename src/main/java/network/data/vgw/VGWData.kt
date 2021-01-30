package network.data.vgw

import network.data.Online
import network.data.chess.GridPoint
import java.io.Serializable

data class VGWData(val pieces: Array<Array<VGWPieceData?>> = arrayOf()) : Serializable
data class VGWPieceData(val pp: GridPoint, val c: String) : Serializable
data class VGWMove(val i: Int, val c: String) : Serializable
data class VGWOnlines(val red: Online?, val yellow: Online?, val onlines: Array<Online>) : Serializable
data class VGWon(val list: Array<GridPoint>) : Serializable {
    fun clone() = VGWon(list.clone())
}