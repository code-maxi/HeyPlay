package server.user

import adds.copyAs
import adds.kill
import network.data.Info
import network.data.OnlineI
import network.data.OnlineStart
import network.data.chess.ChessData
import network.data.chess.ChessPieceData
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket
import kotlin.concurrent.thread


class ChessGame(name: String) : OverGame(name, "chess") {
    var clients = ArrayList<ChessUser>()
    var chessStage: String = "waiting"
    var chessData: ChessData? = null
    var dialog: Info? = null

    val white: ChessUser?
        get() = clients.find { it.role == "white" }
    val black: ChessUser?
        get() = clients.find { it.role == "black" }

    override fun users() = clients as ArrayList<GameUser>
    override fun onlines() = clients.copyAs { it.online() as OnlineI }

    override fun install() {
        chessStage = "waiting"
        dialog = null
        chessData = null
    }

    fun forall(f: (ChessUser) -> Unit) {
        clients.forEach { f(it) }
        white?.let { f(it) }
        black?.let { f(it) }
    }
}

class ChessUser(s: Socket, out: ObjectOutputStream, inp: ObjectInputStream) : GameUser(s, out, inp) {
    lateinit var game: ChessGame

    override fun toOther(f: (GameUser?) -> Unit) {
        game.apply {
            if (white === this@ChessUser) f(black)
            if (black === this@ChessUser) f(white)
        }
    }
    override fun allOn() = game.black != null && game.white != null

    override fun myGame() = game
    override fun igi() = this::game.isInitialized

    override fun getStage() = game.chessStage
    override fun setStage(s: String) { game.chessStage = s }

    override fun getDialog() = game.dialog
    override fun setDialog(i: Info?) { game.dialog = i }

    override fun otherError(message: OnlineStart): String? {
        val bwhite = game.white != null
        val bblack = game.black != null

        val b: String? = if (message.o.role == "white" && bwhite) "Weiß ist schon besetzt!"
        else if (message.o.role == "black" && bblack) "Schwarz ist schon besetzt!"
        else if (message.o.role == "viewer" && (!bblack || !bwhite)) "Es existiert noch keine Partie!"
        else null

        return b
    }

    override fun afterBye() {
        if (igi()) game.clients.remove(this)
        else beforeUsers kill this
        OverGame.reloadGames(type(), igi())
    }

    override fun addUser() {
        if (games[start.game] == null) games[start.game] = ChessGame(start.game)
        game = games[start.game]!! as ChessGame
        game.clients.add(this)
        game.sendOnlines()

        game.clients.add(this)
        beforeUsers kill this
    }

    override fun newPartie() {
        super.newPartie()
        forall { it.send("new partie") }
    }
    private fun clearPartie() {
        if (role != "viewer") {
            if (game.stage == "partie") {
                forall { it.send("clear partie") }
                game.chessData = null
                game.stage = "waiting"
            }
        } else if (game.stage == "partie") game.chessData?.copy()?.let { send(it) }
    }

    override fun addUserFinish() {
        if (allOn()) {
            if (game.stage == "waiting") {
                thread {
                    Thread.sleep(1000)
                    newPartie()
                }
            }
        }
        else clearPartie()
        afterAdded()
    }

    override fun forall(f: (GameUser) -> Unit) { game.forall(f) }

    override fun subListen(o: Serializable) {
        when (o) {
            is ChessData -> {
                game.chessData = o.copy()
                forall {
                    if (it !== this) it.send(o.copy())
                }
            }
            is ChessPieceData -> {
                forall {
                    if (it !== this) it.send(o.copy())
                }
            }
        }
    }
}
