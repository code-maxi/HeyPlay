package gui.game.hoverball

import adds.*
import gui.gamePanel
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import main.*
import network.data.Info
import network.data.hoverball.*
import tornadofx.*
import kotlin.concurrent.thread


data class Item(val b: Button, val f: () -> Unit, val l: Label, val lf: (Label) -> Unit)

class HoverballBord : Game() {
    lateinit var startFenster: StartFenster
    lateinit var onlineTeams: OnlineTeams
    fun ici() = this::startFenster.isInitialized

    class StartFenster : View() {
        lateinit var settings: Settings
        lateinit var startButton: Button
        lateinit var teamButton: Button

        inner class Settings : View() {
            var a = mapOf<String, Item>()
            private fun <T> addItem(s: String, b: String, arr: Array<T>, akk: String, auf: ((T) -> String) = { it.toString() }, f: (Label) -> Unit) {
                a += s to Item(Button(b), {
                    selectComboValue(arr, b) {
                        hbClient.vorschlagen(
                                it.toString(),
                                "${client.username} will $akk auf ${auf(it)} setzen.\nIst dies OK?", s
                        )
                    }
                }, Label("–"), f)
            }
            override val root = gridpane {
                alignment = Pos.CENTER
                maximal(false)
                addClass("center-component-box2")
                hgap = 5.0
                vgap = 5.0
                var i = 0

                addItem("laufzeit", "Laufzeit (min)", arrayOf(2, 5, 10, 15, 20, 25, 30, 45, 60), "die Laufzeit", { "${it}min" }) { it.text = hbClient.startWerte.duration.toString() }
                addItem("shot", "Anzahl der Schuss-Bälle", arrayOf(1, 2, 3, 4, 5), "die Anzahl der Bälle") { it.text = hbClient.startWerte.shot.toString() }
                addItem("team", "Anzahl der Tore pro Team", arrayOf(1, 2, 3, 4, 5), "die Anzahl der Tore pro Team") { it.text = hbClient.startWerte.team.toString() }

                a.forEach { (_, e) ->
                    this += e.l.apply {
                        addClass("rundenanzahl-label")
                        gridpaneConstraints { columnRowIndex(0, i) }
                    }
                    this += e.b.apply {
                        action { e.f() }
                        addClass("center-component-button")
                        gridpaneConstraints { columnRowIndex(1, i) }
                    }
                    i ++
                }
            }
            fun reload() {
                a.forEach { (_, i) -> Platform.runLater { i.lf(i.l) } }
            }
            init { reload() }
        }
        override val root = borderpane {
            style { backgroundColor += Color.LIGHTBLUE }
            prefWidth = 550.0
            prefHeight = 700.0
            maximal()

            center {
                vbox {
                    alignment = Pos.CENTER
                    spacing = 10.0
                    isFillWidth = false

                    startButton = button("Spiel Starten") {
                        addClass("center-component-button")
                        action {
                            really(
                                "Willst du wirklich eine Anfrage stellen,\ndas Spiel jetzt zu Starten?",
                                thinkingOf = true, pane = gamePanel.stack
                            ) {
                                if (it) hbClient.vorschlagen(
                                        "--", "${client.username} will das Spiel jetzt starten.",
                                        "spiel starten", "Willige erst ein, wenn schon alle da sind, die Rundenanzahl OK ist, ..."
                                )
                            }
                        }
                    }
                    teamButton = button("Cyber-Team einladen") {
                        addClass("center-component-button")
                        style { fontSize = 14.px }
                        action {
                            inviteCyberTeams {
                                client.send(Info(it, "invite cyber team"))
                            }
                        }
                    }
                    this += Settings().apply { settings = this }
                    button("Aussteigen") {
                        addClass("really-ja")
                        action {
                            really("Willst du wirklich aussteigen?", pane = gamePanel.stack) {
                                if (it) {
                                    hbClient.disconnect()
                                    client.bye()
                                    Platform.runLater { primaryStage.close() }
                                }
                            }
                        }
                    }
                }
            }
            top {
                this += hoverball.onlineTeams
            }
        }
    }

    override fun Canvas.installCanvas() {}

    override fun paint() {}
    override fun install() {
        onlineTeams = OnlineTeams()
        startFenster = StartFenster()
    }
}

class OnlineTeams(val type: Int = 0) : View() {
    var items = ArrayList<TeamItem>()
    var clickListener: (HoverballTeam) -> Unit = {}
    override val root = vbox {
        maximal()
        spacing = if (type == 0) 0.0 else 5.0
    }
    fun reload(f: () -> Unit = {}) {
        items.clear()
        Platform.runLater {
            root.children.clear()
            hbClient.teams.filter { it is CyberTeam }.forEach { root += TeamItem(it).apply { items plus this } }
            hbClient.teams.filter { it is HumanTeam }.forEach { root += TeamItem(it).apply { items plus this } }
            f()
        }
    }
    init { if (type == 0) reload() }
    inner class TeamItem(t: HoverballTeam) : View() {
        var entfernen: Button? = null
        override val root = vbox {
            spacing = 5.0
            hgrow = Priority.ALWAYS

            if (type == 1 && t is HumanTeam) setOnMouseClicked { clickListener(t) }

            addClass("hoverball-team-item")
            style {
                backgroundColor += teamColor(t.color).lc(1.0, 0.5)
                if (type == 1) backgroundRadius = multi(box(10.px))
            }

            borderpane {
                hgrow = Priority.ALWAYS

                left {
                    hbox {
                        spacing = 10.0
                        alignment = Pos.CENTER
                        label(t.name) { addClass("hoverball-team-item-label1") }
                        label(when (t) {
                            is HumanTeam -> "Human"
                            is CyberTeam -> "Cyber"
                            else -> "---"
                        }) { addClass("hoverball-team-item-label4") }
                    }
                }
                right {
                    hbox {
                        spacing = 10.0

                        if (t is CyberTeam && type == 0) entfernen = button("Entfernen") {
                            addClass("really-ja")
                            style { padding = box(5.px, 10.px) }
                            action {
                                really("Willst du das Team ${t.name} wirklich entfernen?", thinkingOf = true, pane = gamePanel.stack) {
                                    if (it) hbClient.vorschlagen(
                                            t,
                                            "${client.username} will das Cyber-Team ${t.name} entfernen.",
                                            "cyber team entfernen"
                                    )
                                }
                            }
                        }
                        label(when (t) {
                            is HumanTeam -> t.users.size.toString()
                            is CyberTeam -> t.size.toString()
                            else -> "keine anhung"
                        }) { addClass("hoverball-team-item-label2") }
                    }
                }
            }

            if (t is HumanTeam) flowpane {
                hgap = 5.0
                vgap = 5.0
                t.users.forEach { label(it.name) { addClass("hoverball-team-item-label3") } }
                style { padding = box(0.px, 20.px) }
            }
        }
    }
}

class HoverballView : View() {
    override val root = hbox {
        maximal()

        game = HoverballBord()
        game.install()
        game.paint()

        this += hoverball.startFenster
    }
}

class Test : View() {
    override val root = borderpane {
        top {
            hbox {
                useMaxWidth = true
                useMaxHeight = false
                prefHeight = 50.0
                style { backgroundColor += Color.RED }
            }
        }
    }
}
lateinit var cyberFrame: CyberFrame
class CyberFrame : View() {
    lateinit var stage: Stage
    override val root = vbox {
        alignment = Pos.CENTER
        spacing = 15.0

        addClass("background-gradient")
        style { padding = box(20.px) }

        label(hbClient.team) { addClass("welcome-label", "welcome-label2") }
        label(
                """Das Cyber-Team ${hbClient.team} wurde nun erfolgreich hinzugefügt.
                |Wenn du diese wieder entfernen willst, unten  auf 'Mannschaft entfernen'.""".trimMargin()
        ) { addClass("cyber-label") }
        button("Team Entfernen") {
            addClass("cyber-button")
            action {
                really("Willst du wirklich das Team entfernen?", thinkingOf = true, pane = gamePanel.stack) {
                    if (it) {
                        if (hbClient.hoverballStage == "waiting") end()
                        else if (hbClient.hoverballStage == "running")
                            really(
                                    "Das Team steckt gerade mitten in einem Spiel.\nWillst du es Wirklich entfernen?",
                                    "Das Team würde mittem im Spiel entfernt werden.", pane = gamePanel.stack
                            ) { if (it) end() }
                    }
                }
            }
        }
    }
    fun end() {
        client.bye()
        primaryStage.close()
    }
    init {
        cyberFrame = this
        thread {
            Thread.sleep(500)
            //Platform.runLater { primaryStage.pack() }
        }
    }
}

fun chooseColor(l: List<TeamColors>, p: StackPane, to: Boolean, f: (TeamColors) -> Unit) {
    OwnDialog(p, true, "Farbe Wählen", s = 10, p = 10, onClose = { if (to) client.enableAll(true) }) {
        hbox {
            spacing = 5.0
            alignment = Pos.CENTER

            fun c(c: TeamColors) {
                button {
                    addClass("select-trumpf-button")
                    style { padding = box(5.px) }
                    var s = 20.0
                    graphic = Group().apply {
                        circle(s / 2.0, s / 2.0, s) {
                            fill = teamColor(c); stroke = Color.BLACK;
                            strokeWidth = 5.0
                        }
                    }
                    action { f(c); it.schliessen() }
                }
            }
            l.forEach { c(it) }
        }
    }
}