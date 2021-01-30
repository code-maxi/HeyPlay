package adds

import gui.chat.chat
import gui.ili
import gui.mainPanel
import gui.mainPanelBox
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import main.Start
import main.Start.Companion.stylesheet
import main.game
import main.igi
import tornadofx.*
import javax.swing.JDialog
import javax.swing.JFrame
import kotlin.concurrent.thread

var backColor = Color.rgb(40, 40, 40)
var dialogs = 0
fun openNewWindow(
        view: Parent,
        stage: Stage? = mainPanel().primaryStage,
        title: String = "",
        w: Double? = null,
        h: Double? = null,
        dialog: Boolean = false,
        jf: JFrame? = null,
        pin: Boolean = false,
        f: (() -> Unit)? = null,
        sf: Stage.() -> Unit = {}
) {
    Platform.runLater {
        val s = Stage()
        s.scene = Scene(view).apply { stylesheets.add(stylesheet) }

        if (stage != null) {
            s.initOwner(stage)
        } else s.centerOnScreen()

        if (w != null && w >= 0) s.minWidth = w
        if (h != null && h >= 0) s.minHeight = h

        if (f != null) s.setOnCloseRequest { Platform.runLater { f() } }
        if (stage != null) s.initModality(Modality.WINDOW_MODAL)
        s.initStyle(StageStyle.UTILITY)
        s.title = title
        s.pack()

        if (stage != null) {
            mainPanelBox().isVisible = true
            if (dialog) s.showingProperty().onChange { b ->
                if (b) dialogs ++
                else dialogs --
                when (dialogs) {
                    1 -> true
                    0 -> false
                    else -> null
                }?.let { mainPanelBox().isVisible = it }
            }
        }

        if (stage != null || jf != null) {
            s.x = (stage?.x ?: jf!!.location.getX()) + (stage?.width ?: jf!!.size.width.toDouble()) / 2.0 - s.width/2.0
            s.y = (stage?.y ?: jf!!.location.getY()) + (stage?.height ?: jf!!.size.height.toDouble()) / 2.0 - s.height/2.0
        } else s.centerOnScreen()

        s.show()
        s.sf()
        if (pin) s.isAlwaysOnTop = true
        s.toFront()
    }
}

fun openNewSwingDialog(
    view: Parent,
    p: JFrame,
    t: String
) = JDialog(p).apply {
    title = t
    add(JFXPanel().apply {
        Platform.runLater {
            scene = Scene(view).apply {
                stylesheets.add(stylesheet)
            }
        }
    })
    pack()
    isResizable = false
}

fun TextInputControl.enter(f: () -> Unit) {
    setOnKeyPressed {
        if (it.code == KeyCode.ENTER) f()
    }
}

fun TextInputControl.key(vararg keys: Pair<(KeyEvent) -> Boolean, () -> Unit>) {
    setOnKeyPressed {
        for (k in keys) {
            if (k.first(it)) k.second()
        }
    }
}

fun wordwrap(t: String, length: Int): String {
    var text = t
    text += ' ' // soll immer mit ' ' enden (Stopper)
    var pos = 0
    var result = ""
    var line = ""
    var word = ""
    while (pos < text.length) {
        if (text[pos] == ' ') {
            if (line.length + 1 + word.length > length) {
                result += "$line\n"
                while (word.length > length) {
                    result += "${word.substring(0, length)}\n"
                    word = word.substring(length)
                }
                line = word
                word = ""
            } else {
                line += " $word"
                word = ""
            }
        } else {
            word += text[pos]
        }
        ++pos
    }
    return "$result$line $word".trim { it <= ' ' }
}

fun Stage.pack(center: Boolean = false) {
    if (!isMaximized && !isFullScreen) Platform.runLater {
        fun s() {
            sizeToScene()
            if (center) centerOnScreen()
        }
        if (igi()) {
            chat.chatPanel.clearChildren()
            s()
            chat.chatPanel.setChildren()
        } else s()
    }
}
fun Node.skalieren() {
    /*SCREEN_W.let { if (it != 1.0) scaleX = it }
    SCREEN_H.let { if (it != 1.0) scaleY = it }*/
}

operator fun <T> ArrayList<T>.plusAssign(e: T) { add(e) }
operator fun <T> ArrayList<T>.minusAssign(e: T) { remove(e) }

fun <T : Any> ArrayList<T>.log(t: String = "", f: (T) -> Any = { it }) {
    print(t)
    if (isNotEmpty()) forEach { print(" ${f(it)}${ if(it === last()) "" else "|" }") }
    else print(" keine Elemene")
    println()
}
fun <T : Any> ArrayList<T>.copy(co: (T) -> T = { it }) = copyAs { co(it) }
fun <T : Any, R : Any> ArrayList<T>.copyAs(co: (T) -> R): ArrayList<R> {
    val a = ArrayList<R>()
    forEach { a += co(it) }
    return a
}
fun <K : Any, V : Any> HashMap<K, V>.copy(co: (K, V) -> Pair<K, V>) = copyAs { k, v -> k to v }
fun <K1 : Any, V1 : Any, K2 : Any, V2 : Any> HashMap<K1, V1>.copyAs(
    co: (K1, V1) -> Pair<K2, V2>
): HashMap<K2, V2> {
    val a = hashMapOf<K2, V2>()
    forEach { (k, v) -> a += co(k, v) }
    return a
}
fun <K, V> Map<K, V>.toHashMap(): HashMap<K, V> {
    val a = kotlin.collections.HashMap<K, V>()
    forEach { (k, v) -> a += k to v }
    return a
}
fun <T> Array<T>.arrayList(): ArrayList<T> {
    val a = ArrayList<T>()
    forEach { a plus it }
    return a
}

const val SERVER_DEBUG = false

/*
fun sendImage(i: Image) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(i, "jpg", byteArrayOutputStream)

        val size: ByteArray = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array()
        output.write(size)
        output.write(byteArrayOutputStream.toByteArray())
    }

    fun recieveImage() {
        val sizeAr = ByteArray(4)
        input.read(sizeAr)
        val size = ByteBuffer.wrap(sizeAr).asIntBuffer().get()

        val imageAr = ByteArray(size)
        input.read(imageAr)

        val image = ImageIO.read(ByteArrayInputStream(imageAr))
    }
 */

fun di(g2: GraphicsContext, i: Image, c: Color = Color(0.0, 0.0, 0.0, 0.3), f: Double = 2.5) {
    val w = i.width/f
    val h = i.height/f
    g2.fill = c
    g2.fillRect(0.0, 0.0, game.canvas.width, game.canvas.height)
    g2.drawImage(i, game.canvas.width/2.0 - w/2.0, game.canvas.height/2.0 - h/2.0, w, h)
}

infix fun <T> ArrayList<T>.plus(p: T) { add(p) }
infix fun <T> ArrayList<T>.kill(p: T) { remove(p) }

infix fun <T> ArrayList<T>.entfernen(f: (T) -> Boolean) {
    var i = 0
    while (i < size) {
        if (f(get(i))) removeAt(i)
        else i ++
    }
}

fun Color.lc(d: Double, a: Double = 1.0) = Color(red*d,green*d,blue*d,a)
fun Color.awt() = java.awt.Color((red * 255.0).toInt(), (green * 255.0).toInt(), (blue * 255.0).toInt())

fun stars(c: Int, b: Int = 5): String {
    var s = ""
    for (i in 1..b) s += if (i <= c) '★' else '☆'
    return s
}
fun dots(c: Int): String {
    var s = ""
    for (i in 1..c) s += "."
    return s
}
fun Node.bindVisible(s: () -> Stage? = { if (ili()) mainPanel().primaryStage else null }) {
    managedProperty().bind(visibleProperty())
    visibleProperty().onChange { Platform.runLater { s()?.pack() } }
}

fun disenableBox(f: Pane.() -> Unit) = Pane().apply {
    useMaxSize = true
    bindVisible { null }
    style { backgroundColor += Color.rgb(0,0,0, 0.55) }
    isVisible = false
    f()
}
fun ownButton(
    toggle: Boolean = false, i: String? = null,
    t: String? = null, s: Double = 20.0,
    tg: ToggleGroup? = null, tt: String? = null,
    p: Pair<Double, Double>? = null, hi: String? = null,
    c: String? = "small-button", f: ButtonBase.() -> Unit
) = (if (toggle) ToggleButton() else Button()).apply {
    fun im(p: String) = ImageView(Start.images[p]!!).apply { fitWidth = s; fitHeight = s }
    i?.let { graphic = im(i) }
    hi?.let { onHover { b -> graphic = im(if (b) it else i!!) } }
    tg?.let { if (this is ToggleButton) toggleGroup = tg }
    t?.let {
        text = it
        style { fontSize = s.px }
    }
    tt?.let { ownTip(tt) }
    c?.let { addClass(it) }
    if (p != null) style { padding = box(p.first.px, p.second.px) }
    bindVisible { null }
    if (this is ToggleButton) selectedProperty().onChange {
        if (it) addClass("selected")
        else removeClass("selected")
    }
    action { this.f() }
}
fun Node.ownTip(s: String) {
    tooltip {
        text = s
        style { fontSize = 12.px }
    }
}
fun <T> ComboBox<T>.comboStyle() {
    style { backgroundColor += Color.rgb(120,120,120) }
    value = items.first()
}
fun Node.maximal(always: Boolean = true, w: Boolean = true, h: Boolean = true) {
    hgrow = if (always && w) Priority.ALWAYS else Priority.NEVER
    vgrow = if (always && h) Priority.ALWAYS else Priority.NEVER
}
fun ScrollPane.bindScrollBottom() {
    (content as Region).heightProperty().onChange {
        Platform.runLater { vvalue = Double.MAX_VALUE }
    }
}