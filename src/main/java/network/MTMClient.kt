package network

import main.igi
import main.mtm
import adds.info
import main.stack
import gui.game.mtm.MTMBoard
import network.data.mtm.*
import java.io.Serializable
import kotlin.concurrent.thread

class MTMClient : GameClient() {
    override fun allOnline(): Boolean { error("es können nicht all online sein") }
    var zwischenData: MTMData? = null
    var wrongAnfragenList: AnfragenWrong? = null

    override fun subListen(o: Serializable) {
        when (o) {
            is AnfragenWrong -> {
                println("m${o.list.size}")
                wrongAnfragenList = o.clone()
                if (igi() && mtm.iai()) mtm.anfragen.setWrongData()
            }
            is MTMouse -> {
                mtm.mouse = o.clone()
                mtm.paint()
            }
            is MTMShapeSet -> {
                mtm.shapes.let {
                    if (it.isNotEmpty()) it[if (o.add == MTMBoard.VORNE) it.size-1 else 0] = o.clone().shape
                }
                mtm.paint()
            }
            is MTMData -> {
                if (igi()) {
                    mtm.set(o)
                    mtm.paint()
                } else zwischenData = o
            }
            is Anfrage -> {
                mtm.anfragen.addAnfrageItem(o.copy())
            }
            is AnfrageBack -> {
                thread {
                    Thread.sleep(200)
                    val f = { if (!mtm.uisOn) mtm.standart.anfragen.isDisable = false }
                    when (o.bewertung) {
                        "wrong" -> info("Deine Anfrage war falsch.", pane = stack(), f = f)
                        "almost" -> info("Deine Anfrage war fast richtig.", "Weiter so!", pane = stack(), f = f)
                        "right" -> {
                            if (o.a.id == id) info("Deine Anfrage war richtig", "und du bist jetzt dran!", pane = stack()) { send("my move") }
                            else if (id != mtm.move.toInt()) info("Deine Anfrage war falsch", "die richtige Anfrage war \"${o.a.text}\".", pane = stack(), f = f)
                        }
                    }
                }
            }
        }
    }
}