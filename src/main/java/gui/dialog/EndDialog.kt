package gui.dialog

import adds.*
import main.wizard
import gui.gamePanel
import gui.igpin
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import main.Start
import main.client
import network.data.Info
import network.data.wizard.WizardOnline
import tornadofx.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

var endDialog: EndDialog? = null
class EndDialog(
        val type: Int,
        val winner: String = "ERROR"
) {
    lateinit var exitButton: Button
    lateinit var partie: Button
    lateinit var dialog: OwnDialog

    fun closeAll() {
        Platform.runLater { dialog.schliessen() }
        endDialog = null
    }

    val root = VBox().apply {
        style { padding = box(40.px) }
        alignment = Pos.CENTER

        if (type == WINNERS) spacing = 25.0

        imageview(Start.images["${when(type) {
                WINNER -> "winner"
                PATT, REMIS -> "patt"
                AUFGEGEBEN -> "aufgeben"
                WINNERS -> "winners"
                else -> "--"
        }}-end"]!!) {
            fitWidth = 300.0
            fitHeight = 300.0
            vboxConstraints { marginBottom = 20.0 }
        }

        label(when(type) {
            WINNER, AUFGEGEBEN -> winner
            PATT -> "Patt"
            REMIS -> "Remis"
            WINNERS -> "Platzierung"
            else -> "--"
        }) {
            addClass("end-dialog-label1")
            vboxConstraints { marginBottom = 2.0 }
        }

        if (type != WINNERS) label(when(type) {
            WINNER -> "hat gewonnen!"
            AUFGEGEBEN -> "hat aufgegeben!"
            PATT, REMIS -> "unentschieden"
            else -> "--"
        }) {
            addClass("end-dialog-label2")
            vboxConstraints { marginBottom = 30.0 }
        }

        if (type == WINNERS) this += WonPlacement()

        button("Beenden") {
            addClass("end-dialog-exit")
            exitButton = this
            vboxConstraints { marginBottom = 5.0 }
            action {
                quit()
            }
        }

        if (client.userrole != "viewer" && type != WINNERS) button("Neue Partie") {
            addClass("end-dialog-partie")
            exitButton = this
            action {
                if (!isDisable) {
                    client.send(Info(client.username, "neue partie beginnen"))
                    isDisable = true
                }
            }
            partie = this
        }
    }

    fun quit() {
        client.bye()
        dialog.schliessen()
        gamePanel.close()
        exitProcess(0)
    }

    fun wrong(t: String) {
        partie.isDisable = false
        info(t)
    }

    init {
        endDialog = this
        thread {
            if (!igpin()) Thread.sleep(1500)
            Platform.runLater {
                dialog = OwnDialog(
                    gamePanel.root, false, null, p = 20,
                    install = {
                        c += "blue-background"
                        bc = Color(0.0,0.0,0.0,0.6)
                    }
                ) {
                    this += root
                }
            }
        }
    }
    class WonPlacement : View() {
        override val root = vbox {
            spacing = 5.0
        }
        init {
            wizard.placement.items.forEachIndexed { i, p -> root += WonPlacementItem(p.w.copy(), i+1) }
        }
    }

    class WonPlacementItem(var w: WizardOnline, var place: Int = 0) : View() {
        lateinit var name: Label
        lateinit var points: Label
        lateinit var placeL: Label
        lateinit var main: BorderPane

        override val root = borderpane {
            useMaxWidth = true

            left {
                placeL = label {
                    addClass("placement-name")
                    borderpaneConstraints {
                        marginRight = 5.0
                        alignment = Pos.CENTER
                    }
                }
            }
            center {
                main = borderpane {
                    addClass("placement-pane-${if (w.o.id == client.id) "own" else "other"}")
                    useMaxWidth = true
                    left {
                        name = label { addClass("placement-name") }
                    }
                    right {
                        points = label { addClass("placement-points") }
                    }
                }
            }
        }
        private fun reload() {
            Platform.runLater {
                name.text = w.o.name
                points.text = w.points.toString()
                placeL.text = place.toString()
            }
        }
        init {
            reload()
        }
    }

    companion object {
        const val WINNER = 0
        const val PATT = 1
        const val AUFGEGEBEN = 2
        const val REMIS = 3
        const val WINNERS = 4
    }
}