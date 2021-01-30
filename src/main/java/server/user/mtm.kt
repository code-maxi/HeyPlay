package server.user

import gui.game.mtm.MTMBoard
import adds.copyAs
import adds.kill
import network.data.Info
import network.data.OnlineI
import network.data.OnlineStart
import network.data.mtm.*
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList

class MTMGame(name: String) : OverGame(name, "mtm") {
    var clients = ArrayList<MTMUser>()
    var dialog: Info? = null
    var data: MTMData? = null
    var anfragenWrong = AnfragenWrong()

    var move = "no one"
        set(value) {
            field = value
            forall { it.sendMove() }
        }

    @Synchronized
    override fun install() {
        dialog = null
        data = null
        anfragenWrong = AnfragenWrong()
        move = "no one"
    }

    fun forall(f: (MTMUser) -> Unit) {
        clients.forEach { f(it) }
    }

    override fun users() = clients as ArrayList<GameUser>
    override fun onlines() = clients.copyAs { it.online() as OnlineI }
}

class MTMUser(socket: Socket, out: ObjectOutputStream?, inp: ObjectInputStream?) : GameUser(socket, out, inp) {
    lateinit var game: MTMGame

    override fun getStage() = "not needed"
    override fun setStage(s: String) { }
    override fun setDialog(i: Info?) {  }
    override fun getDialog(): Info? { error("dialog not needed") }
    override fun myGame() = game

    override fun igi() = this::game.isInitialized

    override fun otherError(message: OnlineStart): String? = null

    override fun addUser() {
        if (games[start.game] == null) {
            games[start.game] = MTMGame(start.game)
        }
        game = games[start.game]!! as MTMGame
        game.clients.add(this)
        beforeUsers.kill(this)
    }

    override fun toOther(f: (GameUser?) -> Unit) { error("Es kann keinen anderen geben!") }
    override fun allOn(): Boolean { error("Es sind immer alle on!") }

    override fun addUserFinish() {
        if (game.clients.size <= 1) {
            game.move = id.toString()
        }
        else {
            game.data?.let { send(it) }
            send(game.anfragenWrong.clone())
        }
        sendMove()
        afterAdded()
    }

    override fun subListen(o: Serializable) {
        when (o) {
            is String -> {
                when (o) {
                    "send data to all" -> forall { if (it !== this) game.data?.let { d -> it.send(d) } }
                    "send data to me" ->  game.data?.let { d -> send(d) }
                    "my move" -> {
                        game.move = id.toString()
                        game.anfragenWrong = AnfragenWrong()
                        forall { it.send(game.anfragenWrong.clone()) }
                    }
                }
            }
            is MTMShapeSet -> {
                game.data?.shapes?.let { if (it.isNotEmpty()) it[if (o.add == MTMBoard.VORNE) it.size-1 else 0] = o.clone().shape }
                forall { if (it !== this) it.send(o.clone()) }
            }
            is MTMouse -> {
                forall { if (it !== this) it.send(o.clone()) }
            }
            is MTMData -> {
                game.data = o.copy()
            }
            is Anfrage -> {
                forall { if (it.id == game.move.toInt()) it.send(o.copy()) }
            }
            is AnfrageBack -> {
                game.anfragenWrong.list.add(o.copy())
                game.forall {
                    it.sendWrong()
                    if (
                        (o.bewertung != "right" && o.a.id == it.id) ||
                        (o.bewertung == "right" && it.id != game.move.toInt())
                    ) it.send(o.copy())
                }
            }
        }
    }
    fun sendWrong() { send(game.anfragenWrong.clone()) }

    override fun forall(f: (GameUser) -> Unit) { if (igi()) game.forall { f(it) } }

    @Synchronized
    override fun afterBye() {
        if (igi()) {
            game.apply {
                clients kill this@MTMUser
                if (clients.isNotEmpty() && clients.filter { move.toInt() == it.id }.isEmpty()) {
                    move = clients[(clients.size*Math.random()).toInt()].apply {
                        forall {
                            it.send(Info(
                                "Derjenige, der gerade am Zug war ist ausgestiegen.\nAus diesem Grund ${
                                    if (it === this@MTMUser) "bist du"
                                    else "ist $name"
                                } jetzt dran.",
                                "info")
                            )
                        }
                    }.id.toString()
                    data = MTMData()
                    forall { it.send(data!!) }
                }
                sendOnlines()
            }
        } else beforeUsers kill this@MTMUser
        println("after bye")
        OverGame.reloadGames(type(), igi())
    }

    fun sendMove() { send(Info(game.move, "move")) }
}
