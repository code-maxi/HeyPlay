package server.user
import adds.copy
import adds.toHashMap
import network.data.*
import server.server
import kotlin.concurrent.thread

var games = hashMapOf<String, OverGame>()
var beforeUsers = arrayListOf<GameUser>()

data class Vorschlagen(
    val v: VorschlagenItem? = null,
    var ve: Array<Pair<Boolean, Online>> = arrayOf()
)
abstract class OverGame(val name: String, val type: String) {
    abstract fun users(): ArrayList<GameUser>
    val link = "https://jitsi.riot.im/spielekiste-$type-$name"

    var vorschlagen = hashMapOf<String, Vorschlagen>()
    var chat = arrayListOf<Message>()

    var stage = "waiting"
        set(value) {
            field = value
            sendGames(type)
        }

    fun data() = GameData(
        name, type, stage,
        Onlines(onlines().copy { it.clone() })
    )
    fun slog(o: Any) { println("$type[$name] $o") }
    fun myBefore() = beforeUsers.filter { it.type() == type }

    fun all(bc: Boolean = false, f: (GameUser) -> Unit) {
        users().forEach { f(it) }
        if (bc) myBefore().forEach { f(it) }
    }

    fun vorschlagen(id: Int, v: VorschlagenItem) {
        if (users().size <= 1) {
            users().forEach { it.send("enable anfragen") }
            themaAngenommen(true, v.copy())
        }
        else {
            vorschlagen.put(v.thema, Vorschlagen(v.copy()))
            users().forEach {
                it.send("disenable anfragen")
                if (it.id != id) if (it !== this) it.send(v.copy())
            }
        }
    }
    fun vorschlagenBack(t: String, o: Pair<Boolean, Online>) {
        vorschlagen[t]!!.let { v ->
            v.ve += o
            println("${users().size-1} : ${v.ve.size}")
            if (v.ve.size == users().size-1) {
                val nein = v.ve.filter { !it.first }
                users().forEach {
                    it.send(VorschlagenAngenommen(
                        nein.isEmpty(),
                        v.v!!.thema,
                        "Der Vorschlag wurde ${
                            if (nein.isEmpty()) "angenommen"
                            else "wegen \n${nein.joinToString { it.second.name }} nicht angenommen"
                        }.",
                        v.v.from
                    ))
                }
                themaAngenommen(nein.isEmpty(), v.v!!)
                users().forEach { it.send("enable anfragen") }
                vorschlagen.remove(t)
            }
        }
    }
    open fun themaAngenommen(b: Boolean, v: VorschlagenItem) {
        if (b) {
            when (v.value) {
                is String -> when(v.thema) {
                    "kill-chat" -> {
                        chat.clear()
                        users().forEach { it.sendMessages() }
                    }
                }
            }
        }
    }

    fun close(s: Boolean = true) {
        games.remove(name)
        if (s) sendGames(type)
    }

    abstract fun onlines(): ArrayList<OnlineI>

    open fun sendOnlines() {
        all {
            it.send(Onlines(onlines().copy { it.clone() }))
        }
    }

    fun installSuper() {
        chat.clear()
        install()
    }
    abstract fun install()

    companion object {
        fun myGames(type: String?) = games.filter { type == null || it.value.type == type }.toHashMap()
        fun sendGames(type: String?) {
            myGames(type).forEach { (_, u) -> u.users().forEach { it.sendGames() } }
            beforeUsers.forEach { it.sendGames() }
        }
        fun reloadGames(type: String?, sending: Boolean = true) {
            println("reload games")
            val games = myGames(type)
            for (i in games.values.indices) {
                val g = games.values.toTypedArray()[i]
                if (g.users().size == 0) g.close(false)
            }
            if (games.size == 0 && beforeUsers.size == 0) {
                thread {
                    Thread.sleep(1000)
                    server.refresh()
                }
            }
            else if (sending) {
                sendGames(type)
                println("sending games")
            }
        }
    }
}