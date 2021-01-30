package network

import adds.copyAs
import game.chess.ChessBoard
import gui.chat.chat
import gui.chat.ownMessages
import gui.dialog.EndDialog
import adds.info
import adds.really
import gui.dialog.endDialog
import gui.login
import javafx.application.Platform
import main.*
import network.data.*
import java.io.Serializable


abstract class GameClient : Client() {
    open fun allOnline() = true
    open fun anyoneOnline() = onlines.isNotEmpty()
    abstract fun subListen(o: Serializable)
    var zwischenNewPartie = false
    var zwischenMove = "-1"
    var serverVersion = "-1"
    var meetingLink = ""

    fun type() = when (this) {
        is ChessClient -> "chess"
        is VGWClient -> "vgw"
        is MTMClient -> "mtm"
        is WizardClient -> "wizard"
        is HoverballClient -> "hoverball"
        else -> error("Das kann nicht sein!")
    }

    fun online() = Online(username, userrole, id, gameName, usertippt)

    override fun listen(o: Serializable) {
        when (o) {
            is String -> {
                when (o) {
                    "clear partie" -> if (igi()) game.clearPartie()
                    "new partie" -> {
                        if (igi()) {
                            game.newPartie()
                        } else zwischenNewPartie = true
                    }
                    "reload canvas" -> {
                        if (game.canvasReload) {
                            game.reloadCanvas()
                        }
                    }
                    "close dialogs" -> {
                        endDialog?.closeAll()
                    }
                    "neue partie kann starten" -> game.newPartie()
                    "remie wurde bestätigt" -> {
                        if (game is ChessBoard) chess.remis() else game.unentschieden()
                    }
                    "remie wurde nicht bestätigt" -> info("${if (this is ChessClient) "Remis" else "Gleichstand"} wurde nicht angenommen.", type = "error")
                    "remie ist nicht online" -> info("Anderer Spieler ist nicht online!", type = "error")
                    "disenable anfragen" -> disenableAnfragen(true)
                    "enable anfragen" -> disenableAnfragen(false)
                    "queue exited" -> {
                        send("first-get-properties")
                    }
                }
            }
            is Message -> {
                o.anchor = (if (o.o.id == id) Message.RIGHT else Message.LEFT).apply { println(this) }
                chat.chatPanel.addMessage(o)
            }
            is InfoValidID -> {
                if (o.subject == "user") {
                    println("valid!: $o")
                    id = o.id
                    username = o.name
                    if (igi()) {
                        game.upperComponent.nameReload(o)
                    }
                    else login.start()
                }
            }
            is Onlines -> reloadOnlines(o)
            is GamesData -> reloadGames(o)
            is Info -> {
                when (o.subject) {
                    "user" -> {
                        if (igi()) game.upperComponent.nameReload(o)
                        else info(*(o.text.split("\n").toTypedArray()), type = "error")
                    }
                    "meeting-link" -> { meetingLink = o.text }
                    "move" -> {
                        if (igi()) {
                            game.move = o.text
                            mtm.move = o.text
                            println("mover ersef ${o.text}")
                        }
                        else zwischenMove = o.text
                    }
                    "gewonnen" -> if (endDialog == null) EndDialog(EndDialog.WINNER, o.text)
                    "unentschieden" -> if (endDialog == null) EndDialog(EndDialog.PATT, o.text)
                    "aufgegeben" -> if (endDialog == null) EndDialog(EndDialog.AUFGEGEBEN, o.text)
                    "remis" -> if (endDialog == null) EndDialog(EndDialog.REMIS, o.text)
                    "neue partie beginnen" -> {
                        really("Willst du mit ${o.text} eine neue Partie beginnen?", "Die vorherige Partie wird verworfen.") {
                            send(Info(username,
                                    "${
                                        if (it) ""
                                        else "nicht "
                                    }bestätigt neue partie beginnen")
                            )
                        }
                    }
                    "remie anfordern" -> {
                        really(
                            "${o.text} bietet ${if (this is ChessClient) "Remis" else "Gleichstand"} an. Nimmst du an?",
                            "Es ist dann Gleichstand."
                        ) {
                            send(Info(username,
                                    "remie ${
                                        if (it) ""
                                        else "nicht "
                                    }bestätigt")
                            )
                        }
                    }
                    "nicht bestätigt neue partie beginnen" -> { endDialog?.wrong("${o.text} wollte keine neue Partie beginnen.") }
                    "dialog wrong" -> { endDialog?.wrong(o.text) }
                    "info" -> { info(o.text) }
                    "tippt" -> { chat.onlineUser.reloadItem(o.text.toInt(), true) }
                    "!tippt" -> { chat.onlineUser.reloadItem(o.text.toInt(), false) }
                    "server-version" -> { serverVersion = o.text }
                }
            }
            is VorschlagenItem -> {
                disenableAnfragen(true)
                vorschlagenReally (o) { send(VorschlagenItemBack(it, o.thema)) }
            }
            is VorschlagenAngenommen -> {
                vorschlagAngenommen(o)
            }
            is MessagesData -> {
                Platform.runLater {
                    ownMessages.clear()
                    o.m.forEach { chat.chatPanel.addMessage(it.copy(anchor = if (it.o.id == id) Message.RIGHT else Message.LEFT), false) }
                }
            }
        }
        subListen(o)
    }

    open fun reloadOnlines(o: Onlines) {
        if (gamesOnlines[gameName] != null) {
            gamesOnlines[gameName]!!.onlines = Onlines(o.l.copyAs { it.clone() })
            reloadOnlines()
        }
    }
    open fun reloadGames(o: GamesData) {
        allGamesOnlines.clear()
        for (i in o.l) allGamesOnlines.put(i.name, i.copy())
        reloadOnlines()
    }
    open fun vorschlagAngenommen(v: VorschlagenAngenommen) { info(v.text, pane = game.dialogStack()) }
    open fun disenableAnfragen(bb: Boolean) {}
    open fun vorschlagenReally(o: VorschlagenItem, f: (Boolean) -> Unit) {
        adds.really(o.text, under = o.under, jesText = "OK", f = f)
    }
    open fun vorschlagen(value: Serializable, text: String, thema: String, under: String? = null) {
        send(VorschlagenItem(value, text, thema, online(), under))
        enableAll(false)
    }
    fun enableAll(b: Boolean) { send(Info(b.toString(), "think of")) }

    @Throws(Exception::class)
    override fun installAll(server: String?, port: Int, name: String, role: String) {
        install(server, port)

        println("User started.")
    }
}