package main

import adds.*
import adds.Dialog
import game.chess.ChessBoard
import gui.chat.chat
import gui.*
import gui.dialog.endDialog
import gui.game.hoverball.HoverballBord
import gui.game.mtm.MTMBoard
import gui.game.vgw.VGWBoard
import gui.game.wizard.WizardBoard
import javafx.animation.TranslateTransition
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.FontPosture
import javafx.stage.Stage
import javafx.util.Duration
import main.*
import network.MTMClient
import network.WizardClient
import network.data.Info
import network.data.InfoValidID
import network.data.Online
import network.data.wizard.WizardOnline
import tornadofx.*
import java.awt.event.ActionEvent
import java.io.Serializable
import javax.swing.Timer

var onlinesPane = hashMapOf<String, OnlinePanes>()
lateinit var game: Game
lateinit var startDialog: StartDialog
fun stack(): StackPane = if (igi()) (if (endDialog == null) game.dialogStack() else gamePanel.root) else login.root
fun igi() = ::game.isInitialized
fun isdi() = ::startDialog.isInitialized
fun wcf() = client is WizardClient && igi()
fun mcf() = client is MTMClient && igi()

fun reloadOnlines() {
    onlinesPane[client.type()]?.reload()
    if (igi()) {
        if (game.uinstallt()) game.upperComponent.neuladen()
        chat.onlineUser.reload()
    } else { login.startOnlines.reload() }
}

val chess: ChessBoard
    get() = game as ChessBoard

val vgw: VGWBoard
    get() = game as VGWBoard

val mtm: MTMBoard
    get() = game as MTMBoard

val wizard: WizardBoard
    get() = game as WizardBoard

val hoverball: HoverballBord
    get() = game as HoverballBord

val repaintTimer = Timer(5) {
    game.paint()
}

abstract class Game(var width: Double? = null, var height: Double? = null) {
    var existPartie = false
    var send = true
    var canvasReload = true
    var animationen = true
    var canvas = OwnCanvas()
    var repaintAction: (ActionEvent) -> Unit = {}
    var repaintTimer = Timer(20) { repaintAction(it); paint() }

    lateinit var upperComponent: Component
    lateinit var downerComponent: View

    open fun dialogStack() = gamePanel.stack

    fun uinstallt() = this::upperComponent.isInitialized
    fun dinstallt() = this::downerComponent.isInitialized

    fun canvas() = canvas
    abstract fun Canvas.installCanvas()


    fun reloadCanvas() {
        val re = OwnCanvas().apply {
            installCanvas()
            canvas = this
            paint()
        }
        Platform.runLater { canvas.replaceWith(re) }
    }

    open var move = ""

    abstract fun paint()

    open fun newPartie() {}
    open fun clearPartie() {}

    open fun gewonnen() {}
    open fun unentschieden() {}
    open fun aufgegeben() {}

    abstract fun install()

    inner class OwnCanvas : Canvas() {
        init {
            widthProperty().onChange { paint() }
            heightProperty().onChange { paint() }
        }
        override fun isResizable() = true

        override fun prefWidth(p: Double) = this@Game.width ?: -1.0
        override fun prefHeight(p: Double) = this@Game.height ?: -1.0
        override fun minWidth(p: Double) = this@Game.width ?: -1.0
        override fun minHeight(p: Double) = this@Game.height ?: -1.0
    }
}

abstract class Component : View() {
    abstract fun neuladen()
    abstract fun nameReload(i: Serializable)
}

class NameLabel(var o: Online, val changeable: Boolean = client.id == o.id, val f: NameLabel.(String) -> Unit) : Label(o.name) {
    var textfield: TextField? = null
    var changeing = false
    inner class MyModel : ViewModel() {
        var name = bind { SimpleStringProperty() }
    }
    val model = MyModel()
    fun changeBack() {
        changeing = false
        text = textfield!!.text
        f(textfield!!.text)
        Platform.runLater {
            textfield!!.replaceWith(this, ViewTransition.Fade(0.3.seconds))
        }
    }
    fun animate() {
        Platform.runLater {
            if (game.animationen) TranslateTransition(Duration.millis(300.0), textfield ?: this).apply {
                byY = 10.0
                cycleCount = 4
                isAutoReverse = true
                play()
            }
        }
    }
    init {
        setOnMouseClicked { it1 ->
            if (it1.clickCount == 2 && changeable) {
                Platform.runLater {
                    replaceWith(
                            Pane().apply {
                                this@NameLabel.changeing = true
                                this@NameLabel.textfield = textfield(model.name) {
                                    text = this@NameLabel.text
                                    validator { n ->
                                        var m: ValidationMessage? = null
                                        nameValidator(n).let { if (it != null) m = error(it) }
                                        m
                                    }
                                    addClass("textfield-player")
                                    prefWidth = 110.0
                                    enter {
                                        if (model.isValid) this@NameLabel.f(text)
                                        else info("Textfeld ist nicht valid!", type = "error")
                                    }
                                    Platform.runLater { requestFocus() }
                                }
                            }, ViewTransition.Fade(0.3.seconds)
                    )
                }
            }
        }
    }
}

abstract class UpperComponentExample(leftI: Array<Pair<String, NameLabel>>, rightI: Array<Pair<String, NameLabel>>) : Component() {
    var map: Map<String, NameLabel> = mapOf()
    val timer = GameTimer()

    fun sendName(s: String) {
        client.username = s
        client.sendUser()
    }

    fun m(s: String) = map[s]!!

    override val root = gridpane {
        useMaxSize = false
        hgap = 15.0
        row {
            alignment = Pos.CENTER
            for (i in leftI) {
                pane {
                    useMaxSize = false
                    this += i.apply { map += this }.second
                }
            }
            this += timer
            for (i in rightI) {
                pane {
                    useMaxSize = false
                    this += i.apply { map += this }.second
                }
            }
        }
    }

    init {
        neuladen()
    }

    override fun nameReload(it: Serializable) {
        if (it is InfoValidID) {
            for (i in map) {
                if (i.value.changeing) i.value.changeBack()
            }
        }
        else if (it is Info) {
            info(it.text)
        }
    }
}

abstract class StandartGameView(g: Game, s: VBox.() -> Unit = {}) : View() {
    override val root = vbox {
        game = g
        game.install()
        game.paint()
        spacing = 4.0
        s()
        addThings()
    }
    open fun VBox.addThings() {
        this += game.upperComponent
        this += game.canvas
        this += game.downerComponent
    }
}

fun askAufgeben() {
    if (client.allOnline()) {
        really("Willst du wirklich aufgeben?", "Das Spiel wird beendet und du verlierst.", pane = gamePanel.stack) {
            if (it) {
                game.aufgegeben()
            }
        }
    }
    else info("Anderer Spieler ist nicht online!", type = "error")
}

fun remisAnfordern(s: String) {
    if (client.allOnline()) {
        really("Willst du wirklich $s anbieten?", "Das Spiel wird beendet und es ist Gleichstand.", pane = gamePanel.stack) {
            if (it) {
                client.send(Info(client.username, "remie anfordern"))
            }
        }
    } else info("Anderer Spieler ist nicht online!", type = "error")
}

abstract class SimpleDialog(
    t: String, f: SimpleDialog.() -> Parent, tt: String,
    w: Double = -1.0, h: Double = -1.0
) : Dialog() {
    override lateinit var stage: Stage
    override val root = vbox {
        addClass("simple-dialog-container-${if (client is WizardClient) "wood" else "normal" }")
        spacing = 10.0
        alignment = Pos.CENTER
        label(t) { addClass("simple-dialog-label") }
        this += f()
    }
    init {
        Platform.runLater {
            openNewWindow(root, primaryStage, tt, w, h, dialog = true) {
                stage = this
                installed()
            }
        }
    }
    open fun installed() {  }
}

class NameLabelContainer(c: String = "upper-border-pane", val f: NameLabel.(WizardOnline) -> Unit) : Component() {
    val labels: ArrayList<NameLabel> = ArrayList()

    override val root = hbox {
        useMaxWidth = true
        useMaxHeight = false
        alignment = Pos.CENTER
        spacing = 5.0
        addClass(c)
    }

    init {
        neuladen()
    }

    override fun neuladen() {
        Platform.runLater {
            root.children.clear()
            labels.clear()
            for (i in wizardClient.onlines) {
                root += NameLabel(i.o) {
                    client.changeName(it)
                }.apply {
                    labels.add(this)
                    f(i)
                    root.minHeight = height
                    useMaxWidth = false
                }
            }
        }
    }

    override fun nameReload(i: Serializable) {
        if (i is InfoValidID) {
            f@ for (o in labels) {
                if (o.changeing) {
                    o.changeBack()
                }
                break@f
            }
        } else if (i is Info) info(i.text)
    }
}

fun <T> selectComboValue(a: Array<T>, t: String, f: (T) -> Unit = {}) {
    client.enableAll(false)
    OwnDialog(gamePanel.stack, true, t, p = 7, s = 4, thinkingOf = true, thinkingOfOnlyClose = true) {
        hbox {
            alignment = Pos.CENTER
            spacing = 10.0
            val c = combobox(values = a.toList()).apply { comboStyle(); value = items[0] }
            this += ownButton(
                false, t = "Okey",
                c = "green-button", s = 13.0
            ) {
                f(c.value)
                it.schliessen()
            }.apply { vboxConstraints { marginTop = 5.0 } }
        }
    }
}

fun inviteCyberTeams(f: (String) -> Unit = {}) {
    Platform.runLater {
        OwnDialog(gamePanel.stack, true, "Cyber-Team außwählen", p = 0, thinkingOf = true, thinkingOfOnlyClose = true) {
            vbox {
                spacing = 10.0
                alignment = Pos.CENTER
                lateinit var teamLabel: VBox
                lateinit var sLabel: Label
                lateinit var combo: ComboBox<String>

                style { padding = box(20.px) }

                imageview (Start.images["ki-symbol"]!!) {
                    fitWidth = 200.0
                    fitHeight = 200.0
                    vboxConstraints { marginBottom = 10.0 }
                }

                combo = combobox(values = hoverballTeams.keys.toList()) {
                    fun a(b: Boolean = true) {
                        Platform.runLater {
                            teamLabel.children.clear()
                            for (i in hoverballTeams[selectedItem]!!.first) {
                                teamLabel.children.add(label(i) {
                                    addClass("small-heading-label")
                                    style { fontSize = 16.px; textFill = Color.LIGHTBLUE }
                                })
                            }
                            sLabel.text = stars(hoverballTeams[selectedItem]!!.second)
                        }
                    }

                    setOnAction { a() }
                    addClass("my-chombobox")
                    style { fontSize = 15.px }
                }
                vbox {
                    alignment = Pos.CENTER
                    spacing = 10.0
                    label("Team von:") {
                        addClass("small-heading-label")
                        style { fontSize = 16.px }
                    }
                    teamLabel = vbox {
                        alignment = Pos.CENTER
                        spacing = 5.0
                    }
                }

                sLabel = label {
                    addClass("small-heading-label")
                    style { fontSize = 24.px; textFill = Color.YELLOW }
                }

                button("OK") {
                    addClass("message-button")
                    action {
                        combo.selectedItem?.let { f(it) }
                            ?: info("Du hast kein Team angegeben.") { hbClient.enableAll(true) }
                        it.schliessen()
                    }
                }
            }
        }
    }
}

class OnlinePanes(val roles: Map<String, Pair<String, Color>>) {
    var rolePanes = arrayListOf<Pair<String, Pair<VBox, () -> Boolean>>>()
    fun pane(r: String, reloading: () -> Boolean) = VBox().apply {
        val c = roles[r]!!
        alignment = Pos.BASELINE_LEFT
        spacing = 5.0
        label(c.first) {
            style {
                fontSize = 14.px
                textFill = Color.rgb(230,230,230)
            }
        }
        rolePanes.add(r to (vbox {
            spacing = 3.0
            alignment = Pos.BASELINE_LEFT
        } to reloading))
        style {
            padding = box(8.px, 8.px, 12.px, 8.px)
            backgroundRadius += box(5.px)
            backgroundColor += c.second
        }
    }
    fun allPanes(or: Orientation, reloading: () -> Boolean) = FlowPane().apply {
        orientation = or
        alignment = Pos.CENTER
        hgap = 5.0
        vgap = 5.0
        roles.forEach { (r, _) -> this += pane(r, reloading) }
    }
    fun reload() {
        println("List reload...")
        Platform.runLater {
            println("Size: ${rolePanes.size}")
            rolePanes.forEach { (s, p) ->
                if (p.second()) {
                    println("in...")
                    p.first.children.clear()
                    onlines.filter { it.o().role == s }.apply {
                        if (isEmpty()) {
                            p.first.label ("Diese Gruppe ist leer.") {
                                style {
                                    fontSize = 12.px
                                    textFill = Color.LIGHTYELLOW
                                    fontStyle = FontPosture.ITALIC
                                }
                            }
                        }
                    }.forEach {
                        p.first.label(it.o().name) {
                            style {
                                fontSize = 15.px
                                textFill = Color.LIGHTYELLOW
                                padding = box(4.px, 7.px)
                                backgroundColor += Color.rgb(0,0,0,0.3)
                                backgroundRadius += box(5.px)
                            }
                        }
                    }
                }
            }
        }
    }
}

class StartDialog(val type: String) {
    lateinit var dialog: OwnDialog
    val root = VBox().apply {
        alignment = Pos.CENTER
        spacing = 15.0
        imageview (Start.images[when(type) {
            "vgw" -> "vgw-icon"
            else -> "nothing"
        }]!!) {
            fitWidth = 120.0
            fitHeight = 120.0
            rotate = 5.0
        }
        vbox {
            alignment = Pos.CENTER
            spacing = 5.0
            button("Spiel Starten") {
                addClass("center-component-button")
                style { fontSize = 16.px }
            }
            button("Gegen Computer Spielen") {
                addClass("center-component-button")
                style { fontSize = 14.px }
            }
        }
        this += onlinesPane[type]!!.allPanes (Orientation.HORIZONTAL) { true }
    }
    fun show(pane: StackPane) {
        dialog = OwnDialog(
            pane = pane, close = false,
            titel = null, p = 25,
            style = "vgw-start"
        ) { this += root }
    }
}