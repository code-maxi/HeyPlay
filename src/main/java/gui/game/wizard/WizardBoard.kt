package gui.game.wizard

import adds.*
import game.*
import main.Component
import main.onlines
import gui.gamePanel
import javafx.animation.Animation
import javafx.animation.ScaleTransition
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Duration
import main.*
import network.data.Info
import network.data.wizard.WIZARD_CARDS
import network.data.wizard.WizardCard
import network.data.wizard.WizardOnline
import tornadofx.*
import java.io.Serializable
import java.lang.Exception
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.math.roundToInt

fun info(s: String, i: Int = 3) { wizard.info.info(s, i) }
val wizardBC = Color(216.0/255.0,143.0/255.0,0.0, 0.3)
class WizardBoard : Game() {
    var trumpf: WizardCard? = null
    var possibleDragOver: Boolean? = null
    var ownCards: ArrayList<WizardCard> = ArrayList()
    var tableCards: ArrayList<WizardCard> = ArrayList()
    lateinit var cardTable: CardTable
    lateinit var placement: Placement
    lateinit var centerComponent: CenterComponent
    lateinit var nameLabels: NameLabelContainer
    lateinit var info: Info
    fun ipi() = this::placement.isInitialized

    var actualDialog: IDialog? = null

    var beforeStart = false
        set(value) {
            field = value
            paint()
        }

    fun myMove() = wizardClient.move.move == client.id && !beforeStart

    fun Canvas.bindCanvas() {
        if (this@WizardBoard::centerComponent.isInitialized) {
            widthProperty().bind(centerComponent.root.widthProperty())
            heightProperty().bind(centerComponent.root.heightProperty())
        }
    }

    override fun Canvas.installCanvas() {
        bindCanvas()

        setOnDragEntered {  e ->
            if (myMove()) {
                firstcolor().let {
                    val wc = WizardCard.parse(e.dragboard.string)
                    var b = false
                    ownCards.forEach { w -> if (it == w.color && w.type != "n" && w.type != "z") b = true }
                    possibleDragOver = (it == wc.color || it == null || wc.type == "z" || wc.type == "n" || (it != wc.color && !b)).apply {
                        if (!this) info("Du musst bedienen.")
                    }
                    paint()
                }
            }
        }
        setOnDragExited { possibleDragOver = null; paint() }
        setOnDragOver {
            if (myMove() && possibleDragOver == true && it.dragboard.hasString()) it.acceptTransferModes(*TransferMode.ANY)
            it.consume()
        }
        setOnDragDropped {
            val w = WizardCard.parse(it.dragboard.string)
            if (send) client.send(w.copy())
            else tableCards.add(w)
            var i = 0
            var exit = false
            while (i < ownCards.size && !exit) {
                if (ownCards[i] == w) {
                    ownCards.removeAt(i)
                    cardTable.reloadCards()
                    exit = true
                }
                i ++
            }
            paint()
        }
    }

    private fun drawDragOver(g2: GraphicsContext) {
        if (possibleDragOver != null) {
            val b = possibleDragOver!!
            val i = Start.images["drag-over-" + (if (b) "possible" else "impossible")]!!
            di(g2, i, Color(if (b) 0.0 else 1.0, 0.0, 0.0, 0.3))
        }

        if (beforeStart) di(g2, Start.images["before-start-image"]!!, Color(0.0, 0.0, 0.0, 0.2), 2.7)
    }

    @Synchronized
    override fun paint() {
        val gc = canvas.graphicsContext2D
        gc.drawImage(Start.images["wood-background"]!!, 0.0, 0.0, canvas.width, canvas.height)
        drawCards(gc)
        drawDragOver(gc)
    }

    fun drawCards(g2: GraphicsContext) {
        val between = 60.0
        val count = tableCards.size
        val f = 4.0
        val w = Start.W_WIDTH / f
        val h = Start.W_HEIGHT / f
        val startx = canvas.width/2.0 - (between * count + (w-between))/2.0
        val range = 30.0
        val y2 = canvas.height/2.0 - h/2.0
        fun y(b: Boolean = true) = (if (b) (range*Math.random() - range/2.0) else 0.0)
        var x = startx

        trumpf?.let {
            if (it.ay == null) it.ay = y()
            trumpf?.draw(g2, 20.0, y2 + it.y(), f)
        }

        val x2 = 20.0 + w/2.0
        if (trumpf != null) g2.translate(x2, 0.0)
        for (ind in tableCards.indices) {
            val i = tableCards[ind]
            if (i.ay == null) i.ay = y()
            i.draw(g2, x, y2 + i.y(), f)
            x += between
        }
        if (trumpf != null) g2.translate(-x2, 0.0)
    }

    override fun install() {
        canvas.installCanvas()

        cardTable = CardTable()
        if (send) placement = Placement()

        downerComponent = cardTable
        if (send) upperComponent = UpperBorder()
        if (send) centerComponent = CenterComponent()
        else {
            WIZARD_CARDS.sortedBy { Math.random() }.forEachIndexed { i, w -> if (i < 10) ownCards.add(w.copy()) }
        }
        paint()
    }

    inner class DownerBorder : View() {
        override val root = vbox {
            maximal(h = false)

            this += cardTable
            this += placement
        }
    }

    fun ici() = this::centerComponent.isInitialized && this::placement.isInitialized && this::cardTable.isInitialized
    fun cComponent() = if (ici()) centerComponent else null

    inner class CenterComponent : View() {
        lateinit var box: VBox
        lateinit var welcomeBox: VBox
        lateinit var sticheVoraussagenBox: HBox
        lateinit var runden: Label
        lateinit var rundenB: Button
        lateinit var startB: Button
        lateinit var sticheVoraussagenB: Button
        lateinit var trumpfB: Button
        lateinit var boxNow: Parent
        var sticheVoraussagenF: CenterComponent.() -> Unit = {}

        fun setLabel() { runden.text = wizardClient.runden.toString() }
        override val root = stackpane {
            maximal()

            addClass("wood-container")
            hbox {
                maximal()
                this += this@WizardBoard.canvas().apply { bindCanvas() }
            }

            box = vbox {
                alignment = Pos.CENTER
                spacing = 10.0
                style { backgroundColor = multi(wizardBC) }
                maximal()

                welcomeBox = VBox().apply {
                    alignment = Pos.CENTER
                    maximal(false)
                    spacing = 10.0
                    label("Willkommen zu Wizard!") {
                        addClass("welcome-label", "welcome-label2")
                        if (game.animationen) ScaleTransition(Duration.millis(500.0), this).apply {
                            byX = 0.1; byY = 0.1;
                            cycleCount = Animation.INDEFINITE
                            isAutoReverse = true
                            play()
                        }
                    }
                    hbox {
                        maximal(false)
                        alignment = Pos.CENTER
                        vbox {
                            alignment = Pos.CENTER
                            addClass("center-component-box2")
                            spacing = 5.0
                            startB = button("Spiel Starten") {
                                addClass("center-component-button")
                                action {
                                    really("Willst du wirklich das Spiel starten?", pane = gamePanel.stack, thinkingOf = true) {
                                        if (it) {
                                            client.vorschlagen(
                                                "-", "${client.username} will das Spiel jetzt starten.",
                                                "wizard-starten", "Willige erst ein, wenn schon alle da sind, die Rundenanzahl OK ist, ..."
                                            )
                                        }
                                    }
                                }
                            }
                            hbox {
                                alignment = Pos.CENTER
                                spacing = 5.0
                                runden = label(wizardClient.runden.toString()) { addClass("rundenanzahl-label") }
                                rundenB = button("Rundenanzahl auswählen") {
                                    addClass("center-component-button")
                                    action {
                                        selectRunden((1..(60.0/ onlines.size.toDouble()).toInt()).toList()) {
                                            client.vorschlagen(
                                                it, "${client.username} will die Rundenanzahl auf $it setzen.",
                                                "wizard-runden"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                sticheVoraussagenBox = HBox().apply {
                    alignment = Pos.CENTER
                    sticheVoraussagenB = button("Stiche voraussagen") {
                        addClass("center-component-button")
                        action { sticheVoraussagenF() }
                    }
                    trumpfB = Button("Trumpf wählen").apply {
                        addClass("center-component-button")
                        action {
                            selectTrumpf { t ->
                                client.send(Info(t.format(), "set trumpf"))
                                replaceWith(sticheVoraussagenB)
                            }
                        }
                    }
                }
                this += welcomeBox.apply { boxNow = this }
            }
        }

        fun welcome() {
            boxVisible(true)
            boxNow.replaceWith(welcomeBox.apply { boxNow = this })
        }
        fun vorraussagen(s: String, f: CenterComponent.() -> Unit = {}) {
            println("type: $s")
            Platform.runLater {
                wizard.beforeStart = false
                sticheVoraussagenF = f
                boxVisible(true)
                boxNow.replaceWith(sticheVoraussagenBox.apply { boxNow = this })
                sticheVoraussagenBox.children[0].replaceWith(when(s) {
                    "stiche" -> sticheVoraussagenB
                    "trumpf" -> trumpfB
                    else -> null
                }!!)
            }
        }

        fun boxVisible(b: Boolean) { box.isVisible = b }

        fun start() {
            boxVisible(false)
            Platform.runLater {
                placement.root.isVisible = true
                cardTable.root.isVisible = true
                root.replaceWith(SplitPane().apply {
                    orientation = Orientation.VERTICAL
                    maximal()
                    this += this@CenterComponent.root
                    this += wizard.downerComponent.root
                })
                gamePanel.rundenVerringern.isVisible = true
                gamePanel.boxVisible.isVisible = true
                primaryStage.isMaximized = true
            }
            canvas.bindCanvas()
        }

        init { reload() }

        fun reload() {
            startB.isDisable = onlines.size < 2
        }
    }
    inner class UpperBorder : Component() {
        override fun neuladen() { nameLabels.neuladen() }
        override fun nameReload(i: Serializable) { nameLabels.nameReload(i) }

        override val root = vbox {
            alignment = Pos.CENTER
            addClass("wizard-upperborder")
            maximal(h = false)
            spacing = 2.0
            info = Info()
            this += NameLabelContainer("upper-border-pane-wizard") {
                fun f(b: Boolean, c: String) { if (b) addClass(c) else removeClass(c) }
                f(wizardClient.move.move == it.o.id && wizardClient.moveUmgehen == null, "wizard-move-player")
                f(wizardClient.move.dealer == it.o.id, "dealer-player")
                f(wizardClient.moveUmgehen == it.o.id, "move-umgehen-player")
                if (wizardClient.id == it.o.id) f(true, "own-player")
                else f(true, "viewer-player")
            }.apply { nameLabels = this }
            this += info
        }
    }
    class Info : View() {
        var thread: Thread? = null
        override val root = label { addClass("wizard-info") }
        init { root.isVisible = false }
        fun info(s: String, t: Int = 3) {
            root.isVisible = true
            thread?.interrupt()
            Platform.runLater {
                root.text = s
                root.isVisible = true
            }
            thread = thread {
                try { Thread.sleep((t*1000).toLong()) } catch (e: Exception) {}
                root.isVisible = false
                thread = null
            }
        }
    }

    inner class CardTable : View() {
        val CARD_WIDTH = Start.W_WIDTH/4.0
        val CARD_HEIGHT = Start.W_HEIGHT/4.0
        lateinit var pane: FlowPane
        override val root = stackpane {
            maximal()
            bindVisible()
            isVisible = false
            prefHeight = CARD_WIDTH+ + 4.0

            scrollpane {
                addClass("cards-container", "brown-scroll-pane")
                maximal(h = false)
                isFitToWidth = true

                pane = flowpane {
                    hgap = 5.0
                    vgap = 5.0
                    alignment = Pos.CENTER
                }
            }
        }
        init {
            reloadCards()
        }
        fun button(t: WizardCard) = ImageView(if (send && wizardClient.runde == 0) t.back() else t.image()).apply {
            addClass("image-button")
            fitWidth = CARD_WIDTH
            fitHeight = CARD_HEIGHT
            setOnDragDetected {
                val db = startDragAndDrop(*TransferMode.ANY)

                val content = ClipboardContent()
                content.putString(t.format())
                content.putImage(image)
                db.setContent(content)

                it.consume()
            }
        }
        fun reloadCards() {
            Platform.runLater {
                pane.children.clear()
                fun sortList(s: String) = ownCards.filter { it.color == s }.sortedBy { if (it.type.isInt()) it.type.toInt() else when (it.type) {
                    "n" -> 0
                    "z" -> 14
                    else -> null
                }!! }
                fun f(s: String) { for (i in sortList(s)) pane += button(i) }
                f("r"); f("b"); f("y"); f("g")
            }
        }
    }

    override fun dialogStack() = wizardBorder

    fun firstcolor(): String? {
        if (tableCards.isEmpty()) return null

        for (c in tableCards) { if (c.type == "z") return null }

        var firstColor = if (tableCards[0].type != "n") tableCards[0].color else "-"
        if (firstColor == "-") {
            loop@ for (i in tableCards) {
                if (i.type != "n") {
                    firstColor = i.color
                    break@loop
                }
            }
            if (firstColor == "-") return null
        }
        return firstColor
    }

    companion object {
        const val CARD_BORDER = 30.0;

        fun installWizardCards() {
            installCards { c, t -> WIZARD_CARDS.add(WizardCard.parse("$c-$t-n")) }
        }
        fun installCards(f: (c: String, t: String) -> Unit) {
            val colors = arrayOf("r", "g", "y", "b")
            for (c in colors) {
                for (i in 0..14) {
                    val t = when (i) {
                        13 -> "n"
                        14 -> "z"
                        else -> "${i+1}"
                    }
                    f(c, t)
                }
            }
        }
        fun getBorderImage(i: Image): Image {
            val w = i.width
            val h = i.height
            val c = Canvas(w + CARD_BORDER * 2, h + CARD_BORDER * 2)
            val g = c.graphicsContext2D
            g.drawImage(i, CARD_BORDER, CARD_BORDER, w, h)
            g.stroke = Color.BLACK
            g.lineWidth = CARD_BORDER;
            g.strokeRoundRect(CARD_BORDER / 2.0, CARD_BORDER / 2.0, w + CARD_BORDER, h + CARD_BORDER, CARD_BORDER * 2, CARD_BORDER * 2)

            return c.snapshot(null, null)
        }
    }
}

class Placement : View() {
    val items = ArrayList<PlacementItem>()
    val placementBottom = PlacementBottom()
    lateinit var box: VBox
    override val root = borderpane {
        prefWidth = 400.0
        maximal(w = false)
        managedProperty().bind(visibleProperty())
        isVisible = false
        addClass("placement-container")
        top {
            box = vbox {
                maximal(h = false)
                alignment = Pos.TOP_CENTER
                spacing = 5.0
            }
        }
        bottom {
            this += placementBottom
        }
    }
    fun reloadList() {
        Platform.runLater {
            box.children.clear()
            try {
                for (i in items.indices) box += items[i]
            } catch (e: Exception) { }
            //primaryStage.pack()
        }
    }
    fun reload() {
        items.clear()
        wizardClient.onlines.forEach { items += PlacementItem(it) }
        items.sortBy { -it.w.points }
        items.forEachIndexed { i, e -> e.place = i+1; e.reload() }
        placementBottom.reload()
        reloadList()
    }

    class PlacementBottom : View()  {
        lateinit var rundeL: Label
        lateinit var rundenL: Label
        lateinit var roundPL: Label
        lateinit var mittelwertL: Label

        override val root = borderpane {
            addClass("placement-pane-bottom")
            left {
                hbox {
                    spacing = 5.0
                    rundeL = label { addClass("placement-a-stiche") }
                    rundenL = label { addClass("placement-v-stiche") }
                }
            }
            right {
                hbox {
                    spacing = 5.0
                    mittelwertL = label { addClass("placement-name") }
                    roundPL = label { addClass("placement-last-plus") }
                }
            }
        }

        fun reload() {
            Platform.runLater {
                rundeL.text = (wizardClient.runde+1).toString()
                rundenL.text = wizardClient.runden.toString()
                mittelwertL.text = (wizardClient.onlines.sumBy { it.points }.toDouble() / wizardClient.onlines.size.toDouble()).roundToInt().toString()
                val i = (wizardClient.onlines.sumBy { it.vstiche } - (wizardClient.runde+1))
                roundPL.text = if (!wizard.beforeStart) (if (i < 0) "" else "+") + i else "–"
            }
        }
    }

    class PlacementItem(var w: WizardOnline, var place: Int = 0) : View() {
        lateinit var name: Label
        lateinit var points: Label
        lateinit var lastPlus: Label
        lateinit var vstiche: Label
        lateinit var astiche: Label
        lateinit var placeL: Label
        lateinit var main: BorderPane

        override val root = borderpane {
            maximal(h = false)

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
                    maximal(h = false)
                    left {
                        hbox {
                            spacing = 5.0
                            name = label { addClass("placement-name") }
                            points = label { addClass("placement-points") }
                            lastPlus = label {
                                addClass("placement-last-plus")
                                hboxConstraints { marginRight = 10.0 }
                            }
                        }
                    }
                    right {
                        hbox {
                            spacing = 5.0
                            astiche = label { addClass("placement-a-stiche") }
                            vstiche = label { addClass("placement-v-stiche") }
                        }
                    }
                }
            }
        }
        fun reload() {
            Platform.runLater {
                name.text = w.o.name
                points.text = w.points.toString()
                lastPlus.text = (if (w.lastPlus < 0) "" else "+") + w.lastPlus
                astiche.text = w.astiche.toString()
                vstiche.text = w.vstiche.toString()
                placeL.text = "$place."
            }
        }
        init {
            reload()
        }
    }
}
fun selectRunden(u: List<Int> = (0..20).toList(), f: (Int) -> Unit = {}) {
    OwnDialog(wizard.centerComponent.root, true, "Rundenanzahl einstellen", p = 15, thinkingOf = true, style = "normal") {
        hbox {
            alignment = Pos.CENTER
            var c = combobox(values = u) { comboStyle(); value = items[0] }
            spacing = 10.0
            button("OK") {
                addClass("message-button")
                action {
                    f(c.value)
                    it.schliessen(false)
                }
            }
        }
    }
}

fun selectTrumpf(f: (WizardCard) -> Unit) {
    OwnDialog(close = true, titel = "Trumpf wählen", p = 15) {
        hbox {
            spacing = 10.0
            alignment = Pos.CENTER

            fun cb(c: String) {
                lateinit var t: WizardCard
                val i = (Math.random() * 12.0 + 1.0).toInt()
                for (k in WIZARD_CARDS) {
                    if (c == k.color && k.type == i.toString())  t = k
                }

                button {
                    addClass("select-trumpf-button")
                    graphic = imageview(t.image()) {
                        fitWidth = Start.W_WIDTH / 4.5
                        fitHeight = Start.W_HEIGHT / 4.5
                    }
                    action {
                        f(t)
                        it.schliessen()
                    }
                }
            }

            cb("r")
            cb("g")
            cb("b")
            cb("y")
        }
    }
}

fun hellsehen(a: ArrayList<WizardCard>) = FlowPane().apply {
    hgap = 10.0
    vgap = 10.0
    alignment = Pos.CENTER
    //maxWidth = (Start.W_WIDTH/4.0 + 70) * 3
    for (c in a) {
        if (wizardClient.id != c.owner.toInt()) vbox {
            addClass("hellsehen-container")
            alignment = Pos.CENTER
            spacing = 5.0;

            label(wizardClient.client(c.owner.toInt()).o.name) { addClass("hellsehen-label-owner") }
            imageview(c.image()) {
                fitWidth = Start.W_WIDTH / 4.0
                fitHeight = Start.W_HEIGHT / 4.0
            }
        }
    }
}

fun sticheVoraussagen(u: Int = wizardClient.runde+1, ip: Int? = null, f: (Int) -> Unit = {}) {
    val h = wizardClient.runde == 0
    var hd = if (h) hellsehen(wizardClient.hellsehenData.list) else null
    OwnDialog(
        if (h) wizardBorder else wizard.centerComponent.root,
        true, "Stiche vorraussagen", p = 20
    ) {
        vbox {
            alignment = Pos.CENTER
            spacing = 15.0
            if (hd != null) this += hd
            hbox {
                alignment = Pos.CENTER
                val c = combobox(values = (0..u).toList().filter { ip == null || it != ip }) {
                    value = items[0]
                }
                spacing = 10.0
                button("OK") {
                    addClass("message-button")
                    action {
                        wizard.beforeStart = true
                        wizard.paint()
                        client.send(Info(c.value.toString(), "stiche vorraussagen"))
                        f(c.value)
                        it.schliessen()
                    }
                }
            }
        }
    }
}

class StartWizard : View() {
    override val root = pane {
        Start.installImages()
        game = WizardBoard()
        game.send = false
        game.install()
        borderpane {
            center {
                pane {
                    maximal()
                    this += wizard.canvas()
                }
            }
            bottom { this += wizard.cardTable }
        }
    }
    init {
        importStylesheet(Paths.get("src/main/resources/" + Start.stylesheet))
    }
}

lateinit var wizardBorder: StackPane
lateinit var wizardSplit: SplitPane

class WizardView : View() {
    override val root = hbox {
        game = WizardBoard()
        game.install()
        game.paint()
        maximal()

        hbox {
            useMaxSize = true
            this += wizard.placement
            spacing = 3.0
            maximal()

            wizardBorder = stackpane {
                maximal()
                vbox {
                    maximal()
                    prefWidth = 700.0
                    prefHeight = 600.0

                    this += wizard.upperComponent.root
                    this += wizard.centerComponent.root
                }
            }
        }
    }
}

// hallo!
// wewerwerwrewer