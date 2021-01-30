package gui.game.mtm

import adds.info
import adds.maximal
import adds.ownButton
import game.*
import gui.dialog.AnfragenDialog
import gui.gamePanel
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.input.*
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.stage.DirectoryChooser
import main.*
import main.Component
import main.Start.Companion.stylesheet
import network.data.Info
import network.data.InfoValidID
import network.data.chess.Point
import network.data.mtm.*
import tornadofx.*
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import kotlin.error

val mtmBC = Color.rgb(0, 65, 124, 0.5)
class MTMBoard : main.Game() {
    var shapes = LinkedList<MTMShape>()
    var shapesRemoves = LinkedList<MTMShape>()
    var mouse = MTMouse()
    var offset = Point()
    var oldOffset = Point()
    var scale = 1.0

    fun iai() = this::anfragen.isInitialized

    lateinit var standart: DownerBorder
    lateinit var settings: MTMSettings
    lateinit var anfragen: Anfragen
    lateinit var center: StackPane

    override fun dialogStack() = wizardCenter
    var path = false

    private val genauichkeit = 3
    var type = PENCIL
        set(value) {
            field = value
            canvas.cursor = if (field == HAND) Cursor.OPEN_HAND else Cursor.DEFAULT
            if (this::settings.isInitialized) settings.s[field]!!.let { if (!it.isSelected) it.isSelected = true }
        }
    var add = VORNE

    override var move = ""
        set(value) {
            field = value
            uisOn = field.toInt() == client.id
            if (uinstallt()) upperComponent.neuladen()
            resetOffsets()
        }

    var uisOn = false
        set(value) {
            if (value != field) setUi(value)
            if (!value) {
                knownType = null
                type = HAND
            }
            field = value
        }

    private fun setUi(b: Boolean) {
        Platform.runLater {
            if (b) {
                downerComponent.replaceWith(settings.apply { downerComponent = this }, ViewTransition.Fade(0.5.seconds))
                anfragen.leerLabel.text = "Anfragen"
                anfragen.pane.children.clear()
                clearPartie()
            }
            else {
                standart.anfragen.isDisable = false
                downerComponent.replaceWith(standart.apply { downerComponent = this }, ViewTransition.Fade(0.5.seconds))
                anfragen.leerLabel.text = "Falsche Anfragen"
                anfragen.pane.children.clear()
            }
            //gamePanel.primaryStage.pack()
        }
    }

    fun resetOffsets() {
        offset = Point(0.0,0.0)
        scale = 1.0
        paint()
    }

    var knownType: Int? = null
    override fun Canvas.installCanvas() {
        setOnMouseMoved { mouseMoved(it) }
        setOnMousePressed { mousePressed(it) }
        setOnMouseDragged { mouseDragged(it) }
        setOnMouseReleased { mouseReleased(it) }
        setOnScroll { mouseScroll(it) }
        if (this@MTMBoard::center.isInitialized) {
            center.children.remove(canvas)
            center.children.add(this)
            bindIt()
        }
    }
    fun Canvas.bindIt() {
        widthProperty().bind(center.widthProperty())
        heightProperty().bind(center.heightProperty())
    }

    override fun install() {
        if (send) {
            if (client.zwischenMove != "-1") {
                move = client.zwischenMove
            }
            mtmClient.zwischenData?.let { set(it) }
            upperComponent = UpperBorder()
        }

        canvas.installCanvas()
        center = StackPane().apply {
            maximal()
            prefWidth = -1.0
            prefHeight = 500.0
            this += canvas
        }
        canvas.bindIt()

        settings = MTMSettings()
        standart = DownerBorder()
        anfragen = Anfragen()

        downerComponent = if (send) standart else settings

        paint(true)
    }

    fun last() = if (shapes.isNotEmpty()) (if (add == VORNE) shapes.last() else shapes.first()) else null

    fun removeLast() {
        shapes.remove(shapesRemoves.last())
        shapesRemoves.removeLast()
    }

    fun data(): MTMData {
        var l: Array<MTMShape> = arrayOf()
        shapes.forEach { l += it.kopieren() }
        return MTMData(l)
    }
    fun set(d: MTMData) {
        shapes.clear()
        d.shapes.forEach { shapes.add(it.kopieren()) }
    }

    fun validMove() = !send || (send && move != "" && client.id == move.toInt())

    private fun sendLast() {
        last()?.let { send(MTMShapeSet(it.kopieren(), add)) }
    }
    private fun addPoint() {
        last()?.path()?.let { it.points += mouse.p.copy() }
    }
    private fun addShape(d: MTMShape) {
        if (add == VORNE) shapes.addLast(d) else shapes.addFirst(d)
    }
    private fun addPath() {
        addShape(MTMPath(mouse.m.copy(), mouse.fill))
    }
    private fun addOval() {
        addShape(MTMOval(mouse.m.copy(), mouse.fill, mouse.p.copy()))
    }
    fun mousePos(x: Double, y: Double) = Point((x - tx()) * (1.0/scale), (y - ty()) * (1.0/scale))
    private fun mouse(evt: MouseEvent) {
        if (validMove()) {
            mouse.p = mousePos(evt.x, evt.y)
            mouse.fill = type == PATH_FILL || type == OVAL_FILL
            send(mouse.clone())
        }
    }
    private fun setLast() {
        if (shapes.isNotEmpty()) {
            last()!!.path()?.let { it.points.let { it[it.size-1] = mouse.p.copy() } }
            send(last()!!.kopieren())
        }
    }
    fun send(o: Serializable) { if (send) client.send(o) }

    var oldX = 0.0
    var oldY = 0.0

    fun save() {
        Platform.runLater {
            val directoryChooser = DirectoryChooser().apply { title = "Ortner zum Speichern auswählen" }
            val selectedDirectory = directoryChooser.showDialog(gamePanel.primaryStage)
            val bImage = SwingFXUtils.fromFXImage(canvas.snapshot(SnapshotParameters(), null), null)
            try {
                ImageIO.write(
                    bImage, "png",
                    File("${selectedDirectory.toPath().toString().apply { println(this) }}/Montagsmaler-Image-${Math.random()}.png")
                )
            } catch (e: IOException) { e.printStackTrace() }
            info("Das Bild wurde gespeichert!")
        }
    }

    private fun mousePressed(evt: MouseEvent) {
        mouse(evt)
        if (evt.isShiftDown) {
            knownType = type
            type = HAND
        }
        when (type) {
            OVAL_STROKE, OVAL_FILL -> {
                addOval()
            }
            PENCIL -> {
                addPath()
                addPoint()
            }
            LINE -> {
                addPath()
                addPoint()
                addPoint()
            }
            PATH_STROKE, PATH_FILL -> {
                if (!path) addPath()
                addPoint()
                addPoint()
                when (evt.clickCount) {
                    1 -> path = true
                    2 -> path = false
                }
            }
            HAND -> {
                canvas.cursor = Cursor.MOVE
                oldX = evt.x; oldY = evt.y
                oldOffset = offset.copy()
            }
        }
        if (type != HAND) shapesRemoves.add(if (add == VORNE) shapes.last() else shapes.first())
        paint(true)
    }
    private var g = 0
    private fun mouseDragged(evt: MouseEvent) {
        mouse(evt)
        when (type) {
            OVAL_STROKE, OVAL_FILL -> {
                last()?.oval()?.let { it.p2 = mousePos(evt.x, evt.y) }
            }
            PENCIL -> {
                if (g == genauichkeit) {
                    addPoint()
                    g = 0
                } else g ++
            }
            LINE -> setLast()
            HAND -> {
                offset = Point(
                    oldOffset.x + (evt.x - oldX),
                    oldOffset.y + (evt.y - oldY)
                )
            }
        }
        if (type != HAND) sendLast()
        paint()
    }
    private fun mouseReleased(evt: MouseEvent) {
        paint(true)
        if (type == HAND) canvas.cursor = Cursor.OPEN_HAND
        send("reload canvas")
    }
    private fun mouseScroll(evt: ScrollEvent) {
        if (scale >= 0.2) {
            scale -= evt.deltaY * scale / 500.0
            paint()
        } else scale = 0.2
    }
    private fun mouseMoved(evt: MouseEvent) {
        mouse(evt)
        knownType?.let { type = it; knownType = null }
        when (type) {
            PATH_STROKE, PATH_FILL -> {
                if (path) {
                    setLast()
                    sendLast()
                }
            }
        }
        paint()
    }
    override fun paint() { paint(false) }

    fun tx() = canvas.width/2.0 + offset.x
    fun ty() = canvas.height/2.0 + offset.y

    @Synchronized
    fun paint(toAll: Boolean) {
        if (toAll && validMove()) {
            send(data())
            send("send data to all")
        }

        canvas.apply {
            val gc = graphicsContext2D
            gc.save()

            val tx = tx()
            val ty = ty()
            gc.clearRect(0.0, 0.0, width, height)
            gc.translate(tx, ty)
            gc.scale(scale, scale)
            gc.lineCap = StrokeLineCap.ROUND
            synchronized(this) { for (i in shapes) i.paint(gc) }
            if (type != HAND) mouse.paint(gc)

            gc.restore()
        }
    }
    override fun clearPartie() {
        shapes.clear()
        paint(true)
    }
    override fun newPartie() {
        clearPartie()
    }
    override fun gewonnen() { error("In Montagsmaler kann niemand gewinnen.") }
    override fun unentschieden() { error("In Montagsmaler kann es nicht unentschieden geben.")}
    override fun aufgegeben() { error("In Montagsmaler kann man nicht aufgeben.") }

    companion object {
        const val PENCIL = 0
        const val PATH_FILL = 1
        const val PATH_STROKE = 2
        const val LINE = 3
        const val OVAL_FILL = 4
        const val OVAL_STROKE = 5
        const val HAND = 7

        const val HINTEN = 6
        const val VORNE = 7
    }
}
class DownerBorder : View() {
    lateinit var anfragen: Button
    override val root = hbox {
        spacing = 5.0
        maximal(h = false)
        alignment = Pos.CENTER
        addClass("bowner-border-pane")
        button("Anfragen") {
            addClass("blue-button")
            style { fontSize = 14.px }
            anfragen = this
            action {
                AnfragenDialog {
                    if (it != null) {
                        client.send(Anfrage(client.id, it, client.username))
                        isDisable = true
                    }
                }
            }
        }
        this += ownButton(
            false, i = "reset-offsets", s = 20.0,
            tt = "Position und Skalierung zurücksetzen", c = "mtm-settings-trash-button2"
        ) { mtm.resetOffsets() }
    }
}
class UpperBorder : Component() {
    override val root = hbox {
        maximal(h = false)
        useMaxHeight = false
        alignment = Pos.CENTER
        spacing = 5.0
        addClass("upper-border-pane")
    }

    init { neuladen() }

    override fun neuladen() {
        Platform.runLater {
            root.children.clear()
            for (i in onlines) {
                root += main.NameLabel(i.o(), i.o().id == client.id) {
                    client.changeName(it)
                }.apply {
                    if (main.mtm.move == i.o().id.toString()) {
                        addClass("mtm-move-player")
                        removeClass("viewer-player")
                    }
                    else {
                        removeClass("mtm-move-player")
                        addClass("viewer-player")
                    }
                    root.minHeight = height
                }
            }
        }
    }

    override fun nameReload(i: Serializable) {
        if (i is InfoValidID) {
            for (i2 in root.children) {
                if (i2 is main.NameLabel && i2.changeing) i2.changeBack()
            }
        } else if (i is Info) info(i.text)
    }
}

lateinit var wizardCenter: StackPane
class MTMView : View() {
    override val root = splitpane(orientation = Orientation.HORIZONTAL) {
        main.game = MTMBoard()
        main.game.install()
        main.game.paint()
        maximal()

        this += main.mtm.anfragen.apply { prefWidth = 450.0; maximal(h = false) }
        wizardCenter = stackpane {
            vbox {
                spacing = 5.0
                maximal()
                this += main.game.upperComponent
                this += main.mtm.center.apply { prefWidth = -1.0 }
                this += main.game.downerComponent
            }
        }
    }
}

class StartMTM : View() {
    override val root = pane {
        main.game = MTMBoard()
        main.game.send = false
        main.game.install()
        borderpane {
            center { this += main.game.canvas() }
            bottom { this += main.game.downerComponent }
        }
    }
    init {
        importStylesheet(Paths.get(stylesheet))
    }
}
