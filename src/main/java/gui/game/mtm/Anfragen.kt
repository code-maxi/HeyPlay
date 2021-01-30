package gui.game.mtm

import adds.info
import main.mtm
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import adds.bindScrollBottom
import main.client
import adds.maximal
import main.mtmClient
import network.data.mtm.Anfrage
import network.data.mtm.AnfrageBack
import tornadofx.*

class Anfragen : View() {
    var wrongAnfragen = arrayListOf<WrongAnfrageItem>()
    var pane = vbox {
        maximal(w = false)
        prefHeight = 500.0
        minWidth = 250.0
        prefWidth = 450.0
        spacing = 3.0
    }
    lateinit var spane: ScrollPane
    private var leer = true
    lateinit var leerLabel: Label
    val leerLabelPane = BorderPane().apply {
        useMaxWidth = true
        center {
            leerLabel = label("Anfragen") {
                addClass("small-label")
            }
        }
    }
    override val root = vbox {
        maximal()
        alignment = Pos.TOP_CENTER
        addClass("anfrage-container")

        spane = ScrollPane().apply {
            maximal()
            isFitToWidth = true
            addClass("blue-scroll-pane")
            content = pane
            bindScrollBottom()
        }
        this += leerLabelPane
    }
    init {
        pane.children.onChange {
            if (pane.children.size == 0) {
                spane.replaceWith(leerLabelPane)
                leer = true
            }
            else if (leer) {
                leerLabelPane.replaceWith(spane)
                leer = false
            }
        }
        setWrongData()
    }
    private fun addItem(t: Parent) { Platform.runLater { pane.children.add(t) } }

    fun reloadList() {
        Platform.runLater {
            pane.children.clear()
            for (i in wrongAnfragen.indices) pane.children.add(wrongAnfragen[i].root)
        }
    }

    fun addWrongItem(t: AnfrageBack) { wrongAnfragen.add(WrongAnfrageItem(t)) }
    fun addAnfrageItem(t: Anfrage) { addItem(AnfrageItem(t).root) }

    fun setWrongData() {
        mtmClient.wrongAnfragenList?.list?.let {
            println(it.size)
            if (client.id != mtm.move.toInt()) {
                wrongAnfragen.clear()
                for (i in it) wrongAnfragen.add(WrongAnfrageItem(i))
                reloadList()
            }
        }
    }

    abstract inner class AfrageItemSuper(t: String, component: AfrageItemSuper.() -> Parent) : View() {
        fun removeMe() {
            Platform.runLater {
                pane.children.remove(root)
            }
        }

        override val root = borderpane {
            addClass("anfrage-pane")
            left {
                label(t) {
                    addClass("anfrage-label")
                    borderpaneConstraints { marginRight = 20.0 }
                }
            }
            right { this += component() }
        }
    }
    inner class AnfrageItem(val a: Anfrage) : AfrageItemSuper(a.text, {
        HBox().apply {
            spacing = 5.0
            fun end(s: String) {
                client.send(AnfrageBack(a.copy(), s))
                if (s == "right") {
                    info("Du hast die richtige Anfrage an ${a.name} gegeben.", "Dieser ist jetzt dran.")
                    Platform.runLater { pane.children.clear() }
                }
                removeMe()
            }
            button("Richtig") { addClass("anfrage-button"); action { end("right") } }
            button("Fast") { addClass("anfrage-button"); action { end("almost") } }
            button("Falsch") { addClass("anfrage-button"); action { end("wrong") } }
        }
    })
    inner class WrongAnfrageItem(t: AnfrageBack) : AfrageItemSuper(t.a.text, {
        Label().apply {
            when(t.bewertung) {
                "right" -> {
                    text = "Richtig"
                    addClass("right-anfrage-back")
                }
                "almost" -> {
                    text = "Fast"
                    addClass("almost-anfrage-back")
                }
                "wrong" -> {
                    text = "Falsch"
                    addClass("wrong-anfrage-back")
                }
            }
        }
    })
    companion object {
        const val ANFRAGEN = 0
        const val ANFRAGEN_BACK = 1
    }
}
