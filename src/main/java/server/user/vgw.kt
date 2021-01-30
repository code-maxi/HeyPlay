package server.user

import adds.copyAs
import adds.kill
import network.data.Info
import network.data.OnlineI
import network.data.OnlineStart
import network.data.vgw.VGWData
import network.data.vgw.VGWMove
import network.data.vgw.VGWon
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList

class VGWGame(name: String) : OverGame(name, "vgw") {
    var clients = ArrayList<VGWUser>()
    var data: VGWData? = null
    var dialog: Info? = null
    var ergebnis: VGWon? = null

    val yellow: VGWUser?
        get() = clients.find { it.role == "yellow" }
    val red: VGWUser?
        get() = clients.find { it.role == "red" }

    override fun users() = clients as ArrayList<GameUser>
    override fun onlines() = clients.copyAs { it.online() as OnlineI }

    @Synchronized
    override fun install() {
        data = null
        stage = "waiting"
        dialog = null
        ergebnis = null
    }

    fun forall(f: (VGWUser) -> Unit) { clients.forEach { f(it) } }
}

class VGWUser(s: Socket, out: ObjectOutputStream, inp: ObjectInputStream) : GameUser(s, out, inp) {
    lateinit var game: VGWGame

    override fun addUserFinish() {
        game.apply {
            if (role == "viewer") {
                data?.let { send(it) }
                ergebnis?.let { send(it) }
            }
            else {
                data = null
                ergebnis = null
            }
            afterAdded()
        }
    }

    override fun myGame() = game
    override fun igi() = this::game.isInitialized

    override fun getStage() = game.stage
    override fun setStage(s: String) { game.stage = s }
    override fun getDialog() = game.dialog
    override fun setDialog(i: Info?) { game.dialog = i }

    override fun otherError(m: OnlineStart): String? {
        val game = games[m.game]?.let { return@let it as VGWGame }
        val bred = game?.red != null
        val byellow = game?.yellow != null

        val b: String? = if (m.o.role == "red" && bred) "Rot ist schon besetzt!"
        else if (m.o.role == "yellow" && byellow) "Gelb ist schon besetzt!"
        else if (m.o.role == "viewer" && (!byellow || !bred)) "Es existiert noch keine Partie!"
        else null

        return b
    }

    override fun newPartie() {
        super.newPartie()
        game.data = null
    }

    override fun afterBye() {
        if (igi()) game.clients kill this@VGWUser
        else beforeUsers kill this@VGWUser
        OverGame.reloadGames(type(), igi())
    }

    override fun addUser() {
        println("user added")
        if (games[start.game] == null) games[start.game] = VGWGame(start.game)
        game = games[start.game]!! as VGWGame
        game.clients.add(this)
        beforeUsers kill this
        println("addedUser: ${online()}")
    }

    override fun toOther(f: (GameUser?) -> Unit) {
        if (game.red === this) f(game.yellow)
        if (game.yellow === this) f(game.red)
    }

    override fun allOn() = game.red != null && game.yellow != null

    override fun forall(f: (GameUser) -> Unit) { game.forall(f) }

    override fun subListen(o: Serializable) {
        when (o) {
            is VGWData -> {
                game.data = o.copy()
            }
            is VGWMove -> {
                forall {
                    if (it != this) it.send(o.copy())
                }
            }
            is String -> {
                if (o.startsWith("CM")) {
                    forall {
                        it.send(o)
                    }
                }
            }
            is Info -> {
                when (o.subject) {
                    "hover" -> forall { if (it !== this) it.send(o.copy()) }
                }
            }
            is VGWon -> {
                forall { it.send(o.clone()) }
            }
        }
    }
}