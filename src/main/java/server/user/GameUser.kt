package server.user

import adds.arrayList
import adds.copy
import adds.copyAs
import adds.toHashMap
import main.game
import main.version
import network.data.*
import server.log
import server.server
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket

abstract class GameUser(
    socket: Socket,
    out: ObjectOutputStream?,
    inp: ObjectInputStream?
) : User(socket, out, inp) {
    var id = -1
    var gameName = ""
    var tippt = false
    var role = ""

    fun type() = when (this) {
        is ChessUser -> "chess"
        is VGWUser -> "vgw"
        is MTMUser -> "mtm"
        is WizardUser -> "wizard"
        is HoverballUser -> "hoverball"
        else -> error("")
    }

    lateinit var start: OnlineStart
    open fun sendStage() { if (igi()) send(Info(myGame().stage, "stage")) }
    fun isi() = this::start.isInitialized

    abstract fun myGame(): OverGame
    abstract fun igi(): Boolean

    open fun getStage() = "not needed"
    open fun setStage(s: String) {  }
    open fun setDialog(i: Info?) {  }
    open fun getDialog(): Info? = null
    open fun newPartie() {
        server.time = 0
        server.timer.start()
        setStage("partie")
    }

    open fun addUserFinish() {}
    open fun getProperties() {
        send(Info(version, "server-version"))
        sendGames()
    }

    fun online() = c(this)!!

    abstract fun otherError(message: OnlineStart): String?

    abstract fun addUser()
    abstract fun subListen(o: Serializable)

    abstract fun forall(f: (GameUser) -> Unit)

    abstract fun toOther(f: (GameUser?) -> Unit)
    abstract fun allOn(): Boolean

    fun myGames() = games.filter { (_, g) -> g.type == type() }.toHashMap()

    fun name(m: OnlineStart): String? {
        var n: String? = null
        myGames()[m.game]?.users()?.forEach {
            if (it !== this && it.name == m.o.name) {
                n = "Dieser Name existiert bereits!"
                log("${it.name}  ${m.o.name}")
            }
        }
        return n
    }
    open fun sendGames() { send(GamesData(
        myGames().values.toTypedArray().arrayList().copyAs { it.data() }
    )) }

    fun afterAdded() { sendMessages(); OverGame.reloadGames(type()) }

    fun setUser(o: OnlineStart) {
        start = o.copy()

        gameName = o.game
        name = o.o.name
        role = o.o.role
        tippt = false

        val notStarted = id == -1
        if (notStarted) {
            addUser()
            var d = -1
            var exit = true
            while (!exit || d == -1) {
                println("setting id $d")
                exit = true
                forall { if (it.id == d) exit = false }
                if (!exit || d == -1) d = (Math.random() * 1000.0).toInt()
            }
            id = d
        } else {
            myGame().chat.filter { it.o.id == id }.forEach { it.o = c(this)!! }
            forall { it.sendMessages() }
        }

        started = true

        send(InfoValidID(id, name, "user"))
        if (notStarted && getStage() == "end" && role == "viewer") send(getDialog()!!.copy())
        if (notStarted) {
            addUserFinish()
            send(Info(myGame().link, "meeting-link"))
        }
        OverGame.reloadGames(type())
    }

    private fun setValidUser(s: OnlineStart) { if (validUser(s)) { setUser(s) } }

    fun validUser(o: OnlineStart): Boolean {
        val n = if (o.o.name == name) null else name(o)
        val r = if (o.o.role == role) null else otherError(o)

        if (n == null && r == null) {
            println("valid")
            return true
        }
        else if (n != null) send(Info(n, "user"))
        else if (r != null) send(Info(r, "user"))
        return false
    }

    override fun listen(o: Serializable) {
        when (o) {
            is Online -> { setValidUser(start.copy(o = o.copy())) }
            is OnlineStart -> {
                println("in on staret")
                setValidUser(o.copy())
            }
            is String -> {
                when (o) {
                    "ra" -> {
                        forall {
                            it.send("ra")
                        }
                    }
                    "new partie" -> {
                        newPartie()
                    }
                    "close dialogs" -> {
                        forall { it.send(o) }
                    }
                    "reload canvas" -> {
                        forall { it.send(o) }
                    }
                    "tippt" -> {
                        tippt = true
                        forall { it.send(Info(id.toString(), "tippt")) }
                    }
                    "!tippt" -> {
                        tippt = false
                        forall { it.send(Info(id.toString(), "!tippt")) }
                    }
                    "first-get-properties" -> getProperties()
                }
            }
            is VorschlagenItem -> { myGame().vorschlagen(id, o) }
            is VorschlagenItemBack -> { myGame().vorschlagenBack(o.subject, o.b to online()) }
            is Info -> {
                if (o.subject == "gewonnen" || o.subject == "unentschieden" || o.subject == "aufgegeben" || o.subject == "remis") {
                    setStage("end")
                    setDialog(o)
                    server.timer.stop()
                    forall { if (it.started) it.send(getDialog()!!.copy()) }
                }
                fun sendWrong(n: String) { send(Info("$n hat sich schon abgemeldet.", "dialog wrong")) }
                fun sendByWrong(n: String, c: GameUser?) { c?.send(o.copy()) ?: run { sendWrong(n) } }

                when (o.subject) {
                    "remie anfordern" -> toOther { it?.send(o.copy()) ?: send("remie ist nicht online") }
                    "remie bestätigt" -> toOther { it?.send("remie wurde bestätigt") }
                    "remie nicht bestätigt" -> toOther { it?.send("remie wurde nicht bestätigt") }

                    "neue partie beginnen" -> { toOther { sendByWrong(o.text, it) } }
                    "nicht bestätigt neue partie beginnen" -> { toOther { sendByWrong(o.text, it) } }
                    "bestätigt neue partie beginnen" -> {
                        if (!allOn()) sendWrong("Anderer Spieler")
                        else {
                            forall {
                                it.send("close dialogs")
                                it.send("neue partie kann starten")
                            }
                        }
                    }
                    "think of" -> forall { if (o.text.toBoolean()) it.send("enable anfragen") else it.send("disenable anfragen") }
                    "info" -> myGame().users().forEach { it.send(o.copy()) }
                    "hbinfo" -> myGame().users().forEach { it.send(o.copy()) }
                }
            }
            is Message -> {
                myGame().chat.add(o.copy())
                forall { it.send(o.copy()) }
            }
        }
        subListen(o)
    }

    fun sendMessages() { send(MessagesData(myGame().chat.copy { it.copy() })) }
    companion object {
        fun c(c: GameUser?): Online? = if (c != null) Online(c.name, c.role, c.id, c.gameName, c.tippt) else null
    }
}