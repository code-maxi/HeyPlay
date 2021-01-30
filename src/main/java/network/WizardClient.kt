package network

import adds.SERVER_DEBUG
import adds.copy
import adds.copyAs
import gui.dialog.EndDialog
import adds.really
import gui.game.wizard.info
import gui.game.wizard.sticheVoraussagen
import gui.gamePanel
import javafx.application.Platform
import main.*
import network.data.Info
import network.data.Onlines
import network.data.VorschlagenAngenommen
import network.data.wizard.*
import java.io.Serializable
import kotlin.concurrent.thread

class WizardClient : GameClient() {
    val onlines: ArrayList<WizardOnline>
        get() = main.onlines.copyAs { it as WizardOnline }
    var runde = -1;
    lateinit var hellsehenData: HellsehenData
    var moveUmgehen: Int? = null
        set(value) {
            field = value
            wizard.upperComponent.neuladen()
        }
    var runden: Int = -1
        set(value) {
            field = value
            if (igi()) Platform.runLater {
                wizard.cComponent()?.setLabel()
            }
        }
    var move: WizardMove = WizardMove()

    fun client(i: Int) = onlines.filter { it.o.id == i }[0]

    override fun allOnline(): Boolean { error("es können nicht all online sein") }

    override fun subListen(o: Serializable) {
        fun cr() { if (game.canvasReload) thread { Thread.sleep(200); game.reloadCanvas() } }
        when (o) {
            is VorgeschlagenItem -> {
                when (o.thema) {
                    "runden" -> {
                        wizard.centerComponent.rundenB.isDisable = true
                        Platform.runLater { wizard.actualDialog?.schliessen() }
                        really("Willst du die Rundenanzahl auf ${o.text} ändern?", pane = gamePanel.stack) {
                            send(VorgeschlagenItemBack(if(it) "ja" else "nein", "runden"))
                        }
                    }
                    "spiel starten" -> {
                        wizard.centerComponent.startB.isDisable = true
                        Platform.runLater { wizard.actualDialog?.schliessen() }
                        really("Willst du das Spiel jetzt starten?", "Willige erst ein, wenn schon alle da sind, die Rundenanzahl OK ist, ...", pane = gamePanel.stack) {
                            send(VorgeschlagenItemBack(if(it) "ja" else "nein", "spiel starten"))
                        }
                    }
                }
            }
            is Info -> {
                when (o.subject) {
                    "runden" -> {
                        runden = o.text.toInt()
                        if (igi() && wizard.ipi()) wizard.placement.placementBottom.reload()
                    }
                    "runde" -> runde = o.text.toInt()
                    "stich an" -> {
                        var name = "-"
                        onlines.forEach { if (o.text.toInt() == it.o.id) name = if (it.o.id == id) "dich" else it.o.name }
                        gui.game.wizard.info("Der Stich ging an $name.")
                        thread {
                            Thread.sleep(200)
                            wizard.nameLabels.labels.forEach {
                                if (o.text.toInt() == it.o.id) {
                                    it.animate()
                                }
                            }
                        }
                    }
                    "move umgehen" -> {
                        moveUmgehen = o.text.toInt()
                    }
                }
            }
            is WizardOnline -> {
                main.onlines.forEachIndexed { i, w ->
                    if (w.o().id == o.o.id)
                        main.onlines[i] = o.copy()
                }
                wizard.placement.items.forEach {
                    if (it.w.o.id == o.o.id) {
                        it.w = o.copy()
                        it.reload()
                    }
                }
            }
            is VorgeschlagenFinished -> {
                fun i(b: Boolean = true) {
                    if (o.own && b) info("Dein Vorschlag wurde ${if (o.b) "" else "nicht "}angenommen.")
                }
                when (o.thema) {
                    "runden" -> {
                        wizard.centerComponent.rundenB.isDisable = false
                        i()
                    }
                    "spiel starten" -> {
                        wizard.centerComponent.startB.isDisable = false
                        i(!o.b)
                    }
                }
            }
            is HellsehenData -> hellsehenData = HellsehenData(o.list.copy { it.copy() })
            is String -> {
                when (o) {
                    "remove table cards" -> {
                        println()
                        fun rtc() {
                            wizard.tableCards.clear()
                            wizard.paint()
                            cr()
                        }
                        /*if (wizard.animationen) {
                            wizard.repaintAction = {
                                wizard.tableCards.forEach {
                                    it.animate("top") {
                                        wizard.repaintTimer.stop()
                                        cr()
                                    }
                                }
                            }
                            wizard.repaintTimer.start()
                            println("went on!")
                        } else rtc()*/
                        rtc()
                    }
                    "game end" -> EndDialog(EndDialog.WINNERS)
                    "new runde beginns" -> {
                        wizard.beforeStart = false
                        moveUmgehen = null
                        wizard.placement.placementBottom.reload()
                    }
                    "new runde" -> {
                        info("${runde + 1}. Runde von $runden beginnt.")
                        wizard.beforeStart = true
                        wizard.placement.placementBottom.reload()
                    }
                    "start game" -> {
                        wizard.centerComponent.start()
                        wizard.downerComponent.root.isVisible = true
                    }
                    "ready for stiche voraussagen first" -> {
                        if (SERVER_DEBUG) main.client.send(Info("1", "stiche vorraussagen"))
                        else Platform.runLater {
                            (wizard.trumpf?.type == "n").let { t ->
                                wizard.centerComponent.vorraussagen(if (t) "trumpf" else "stiche") {
                                    sticheVoraussagen() { boxVisible(false) }
                                }
                            }
                        }
                    }
                    "ready for stiche voraussagen" -> {
                        if (SERVER_DEBUG) main.client.send(Info("1", "stiche vorraussagen"))
                        else wizard.centerComponent.vorraussagen("stiche") { sticheVoraussagen() { boxVisible(false) } }
                    }
                    "ready for stiche voraussagen last" -> {
                        if (SERVER_DEBUG) main.client.send(Info("1", "stiche vorraussagen"))
                        else wizard.centerComponent.vorraussagen("stiche") {
                            sticheVoraussagen(
                                ip = ((runde + 1) - main.onlines.apply { println("size: $size") }.sumBy { (it as WizardOnline).vstiche }.apply { println(this) })
                            ) { boxVisible(false) }
                        }
                    }
                }
            }
            is WizardMove -> {
                move = o.copy()
                wizard.move = move.move.toString()
                wizard.upperComponent.neuladen()
                var b = wizard.trumpf != move.trumpf?.copy()
                wizard.trumpf = move.trumpf?.copy()
                wizard.trumpf?.apply {
                    if (b) {
                        if (wizard.animationen) {
                            wizard.repaintAction = { animate("bottom") { wizard.repaintTimer.stop() } }
                            wizard.repaintTimer.start()
                        }
                    } else { y = 0.0 }
                }
                wizard.paint()
            }
            is WizardCard -> {
                wizard.tableCards.add(o.copy().apply {
                    if (wizard.animationen) {
                        wizard.repaintAction = { animate("bottom") { wizard.repaintTimer.stop() } }
                        wizard.repaintTimer.start()
                    }
                })
                wizard.paint()
                cr()
            }
            is TableCards -> {
                wizard.tableCards = o.list.copy { it.copy() }
                wizard.paint()
                cr()
            }
            is Cards -> {
                wizard.ownCards = o.list.copy { it.copy() }
                wizard.cardTable.reloadCards()
            }
        }
    }

    override fun vorschlagAngenommen(v: VorschlagenAngenommen) {
        if (v.thema != "wizard-starten" || !v.b) super.vorschlagAngenommen(v)
    }

    override fun disenableAnfragen(bb: Boolean) {
        wizard.centerComponent.apply {
            rundenB.isDisable = bb
            startB.isDisable = bb
        }
        gamePanel.rundenVerringern.isDisable = bb
    }

    override fun reloadOnlines(o: Onlines) {
        super.reloadOnlines(o)
        if (igi() && wizard.ici()) {
            wizard.centerComponent.reload()
            wizard.placement.reload()
        }
    }
}