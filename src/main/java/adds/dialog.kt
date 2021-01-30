package adds

import adds.Information.Companion.ERROR
import adds.Information.Companion.INFO
import game.*
import gui.game.mtm.mtmBC
import gui.game.wizard.wizardBC
import gui.gamePanel
import gui.login
import javafx.application.Platform
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.effect.DropShadow
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import main.*
import network.HoverballClient
import tornadofx.*
import javax.swing.JFrame

interface IDialog {
    fun schliessen()
}

fun info(
    vararg text: String,
    type: String = "info",
    width: Int? = null,
    pane: StackPane? = if (client !is HoverballClient || !hoverballVisible) stack() else null,
    f: () -> Unit = {}
): IDialog {
    return if (pane != null) {
        OwnDialog(pane, false, p = 15, style = "normal") {
            this += newInfo(
                text.toList().toTypedArray(), type = type,
                width = width
            ) { f(); it.schliessen() }
        }
    } else Information(*text, type = when (type) {
        "info" -> INFO; "error" -> ERROR
        else -> null
    }!!,  width = width) { f() }
}

fun really(
    text: String,
    under: String? = null,
    pane: StackPane? = if (client !is HoverballClient || !hoverballVisible || !igi()) stack() else null,
    jesText: String = "Ja",
    noText: String = "Nein",
    fs: Double = 14.0,
    thinkingOf: Boolean = false,
    onClose: (() -> Unit)? = null,
    f: (Boolean) -> Unit
): IDialog {
    if (thinkingOf) client.enableAll(false)
    return if (pane != null) {
        OwnDialog(pane, false, p = 20, style = "normal") {
            this += newReally(text, under, jesText, noText, fs) { b ->
                f(b); if (thinkingOf && !b) client.enableAll(true)
                it.schliessen()
            }
        }
    } else Really(text, under, jesText = jesText, noText = noText, fs = fs, onClose = onClose) {
        f(it); if (thinkingOf) client.enableAll(true)
    }
}

fun newInfo(
    text: Array<String>,
    type: String = "info",
    width: Int? = null,
    f: () -> Unit = {}
) = GridPane().apply {
    alignment = Pos.CENTER
    useMaxSize = true
    hgap = 10.0
    vgap = if (text.size == 1) 5.0 else 15.0
    row {
        imageview(Start.images[type]!!) {
            (if (type == "error") 38.0 else 32.0).let {
                fitHeight = it
                fitWidth = it
            }
            hboxConstraints { marginRight = 20.0 }
        }
        vbox {
            alignment = Pos.BASELINE_LEFT
            spacing = 5.0
            for (a in text) {
                for (i in a.split('\n')) {
                    label(width?.let { wordwrap(i, it) } ?: i) {
                        addClass("message-label")
                        println("$text")
                    }
                }
            }
        }
    }
    row {
        button("OK") {
            addClass("message-button")
            gridpaneConstraints {
                columnSpan = 2
                hAlignment = HPos.RIGHT
                marginTop = 5.0
            }
            action { f() }
        }
    }
}

fun newReally(
    text: String,
    under: String? = null,
    jesText: String = "Ja",
    noText: String = "Nein",
    fs: Double = 14.0,
    f: (Boolean) -> Unit
) = VBox().apply {
    alignment = Pos.CENTER_LEFT
    label(text) {
        addClass("really-text")
        vboxConstraints { marginBottom = 5.0 }
    }
    if (under != null) label("→ $under") {
        addClass("really-under")
        vboxConstraints { marginBottom = 10.0 }
    }
    hbox {
        alignment = Pos.BASELINE_RIGHT
        button(jesText) {
            addClass("really-nein")
            style { fontSize = fs.px }
            hboxConstraints { marginRight = 10.0 }
            action { f(true) }
        }
        button(noText) {
            addClass("really-ja")
            style { fontSize = fs.px }
            action { f(false) }
        }
    }
}

val dialogBC = Color.rgb(0, 0, 0, 0.3)
open class OwnDialog(
    val pane: StackPane = stack(),
    val close: Boolean = true,
    val titel: String? = null,
    val s: Int = 0,
    val p: Int = if (close || titel != null) 12 else 8,
    val style: String = if (wcf()) "wizard" else if (mcf()) "mtm" else "normal",
    val onClose: () -> Unit = {},
    val thinkingOf: Boolean = false,
    val thinkingOfOnlyClose: Boolean = false,
    val install: OwnDialog.() -> Unit = {},
    f: HBox.(OwnDialog) -> Unit
) : View(), IDialog {
    lateinit var bc: Color
    lateinit var dshaddow: Pair<Color, Int>
    lateinit var topBc: Color
    lateinit var centerBc: Color
    var c = arrayOf<String>()
    var tr = false

    fun installStyle() {
        topBc = Color(0.0, 0.0, 0.0, 0.4)
        centerBc = topBc
        if (style == "wizard" || style == "mtm") {
            tr = true
            dshaddow = Color(0.0, 0.0, 0.0, 1.0) to 20
        }
        when (style) {
            "wizard" -> {
                bc = wizardBC
                c += "wood-container"
            }
            "mtm" -> {
                bc = mtmBC
                c += "blue-background"
            }
            "normal" -> {
                bc = dialogBC
                dshaddow = Color(1.0,1.0,1.0,0.5) to 15
            }
            "vgw-start" -> {
                bc = Color.rgb(0, 0, 0, 0.9)
                Color.rgb(0,0,95).let { centerBc = it; topBc = it }
                dshaddow = Color(0.7,0.7,0.7,0.5) to 15
            }
        }
        install()
    }

    override val root = vbox {
        installStyle()

        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        alignment = Pos.CENTER
        style { backgroundColor += bc }
        hbox {
            alignment = Pos.CENTER
            vbox {
                addClass("dialog-container")
                addClass(*c)
                style {
                    effect = DropShadow(dshaddow.second.toDouble(), dshaddow.first)
                }
                vgrow = Priority.NEVER
                hgrow = Priority.NEVER
                alignment = Pos.CENTER
                spacing = s.toDouble()
                if (titel != null || close) {
                    borderpane {
                        style {
                            backgroundColor += topBc
                            padding = box(5.px, 8.px)
                            backgroundRadius = multi(if (tr) box(0.px) else box(7.px))
                        }
                        hgrow = Priority.ALWAYS
                        titel?.let { left {
                            alignment = Pos.CENTER
                            label(it) {
                                style {
                                    fontSize = 14.px
                                    fontWeight = FontWeight.BOLD
                                    textFill = Color.WHITE
                                }
                            }
                        } }
                        center { pane {
                            minWidth = 10.0; minHeight = 0.0
                        } }
                        if (close) right {
                            this += ownButton(
                                false,
                                t = "×", s = 16.0,
                                tt = "Dialog schließen",
                                c = "close-button"
                            ) {
                                schliessen()
                                if (thinkingOf && thinkingOfOnlyClose) client.enableAll(true)
                            }
                        }
                    }
                }
                hbox {
                    alignment = Pos.CENTER
                    style { padding = box((p).px); backgroundColor += centerBc }
                    f(this@OwnDialog)
                }
            }
        }
    }

    override fun schliessen() { schliessen(true) }
    fun schliessen(to: Boolean = true) {
        onClose()
        Platform.runLater { pane.children.remove(root) }
        if (thinkingOf && to && !thinkingOfOnlyClose) client.enableAll(true)
    }
    init {
        if (thinkingOf) client.enableAll(false)
        Platform.runLater { pane.children.add(root) }
    }
}

class Really(
        val text: String,
        under: String? = null,
        val close: Boolean = true,
        val jesText: String = "Ja",
        val noText: String = "Nein",
        val fs: Double = 14.0,
        pin: Boolean = false,
        s: Stage? = if (igi()) gamePanel.primaryStage else login.primaryStage,
        onClose: (() -> Unit)? = null,
        val f: Really.(Boolean) -> Unit
) : Dialog() {
    override lateinit var stage: Stage
    lateinit var jes: Button
    lateinit var cancel: Button

    override val root = newReally(text, under, jesText, noText, fs) { f(it); schliessen() }.apply {
        style {
            backgroundColor += backColor
            padding = box(15.px)
        }
    }

    override fun schliessen() { if (close) close() }
    init {
        openNewWindow(
            view = this.root,
            stage = s,
            dialog = true,
            title = "Bestätigung",
            pin = pin,
            f = { if (onClose != null) onClose() else f(false); schliessen() }
        ) { stage = this }
    }
}

class Information(
    vararg text: String,
    val type: Int = INFO,
    openSofort: Boolean = true,
    s: Stage? = if (igi()) gamePanel.primaryStage else login.primaryStage,
    jf: JFrame? = null,
    width: Int? = null,
    f: () -> Unit = {}
) : Dialog() {
    override lateinit var stage: Stage
    override val root = newInfo(text.toList().toTypedArray(), when (type) {
        ERROR -> "error"; INFO -> "info"
        else -> null
    }!!, width) { f(); close() }.apply {
        style {
            backgroundColor += backColor
            padding = box(15.px)
        }
    }
    override fun schliessen() { stage.close() }
    init {
        if (openSofort) {
            Platform.runLater {
                openNewWindow(
                    view = this.root,
                    stage = s,
                    jf = jf,
                    dialog = true, f = { f(); close() }
                ) { stage = this }
            }
        }
    }

    companion object {
        const val INFO = 0
        const val ERROR = 1
    }
}
fun sl(s: String, ok: (() -> Unit)? = null, f: VBox.() -> Unit) = VBox().apply {
    spacing = 7.0
    alignment = Pos.CENTER
    label(s) { addClass(if (wcf()) "small-label" else "small-heading-label") }
    f()
    ok?.let {
        this += ownButton(
            false, t = "Okey",
            c = "green-button", s = 13.0
        ) { it() }.apply { vboxConstraints { marginTop = 5.0 } }
    }
}

abstract class Dialog : View(), IDialog {
    abstract var stage: Stage
    override fun schliessen() { stage.close() }
}