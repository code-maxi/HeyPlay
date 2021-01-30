package network

import main.chess
import game.chess.chessPieces.ChessPiece
import main.igi
import main.blackUser
import main.whiteUser
import network.data.Onlines
import network.data.chess.ChessData
import network.data.chess.ChessPieceData
import java.io.Serializable

class ChessClient : GameClient() {
    override fun allOnline() = whiteUser != null && blackUser != null
    override fun anyoneOnline() = whiteUser != null || blackUser != null

    override fun subListen(o: Serializable) {
        when (o) {
            is ChessData -> {
                chess.set(o)
                chess.paint()
            }
            is ChessPieceData -> {
                ChessPiece.chessPieces[o.id]?.set(o)
                chess.paint()
            }
        }
    }
    override fun reloadOnlines(o: Onlines) {
        super.reloadOnlines(o)
        if (igi()) chess.started = allOnline()
    }
}