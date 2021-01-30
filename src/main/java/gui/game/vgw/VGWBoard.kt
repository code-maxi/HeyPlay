package gui.game.vgw

import game.*
import adds.Really
import adds.bindVisible
import adds.di
import adds.info
import gui.dialog.endDialog
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import main.*
import network.data.Dialog
import network.data.Info
import network.data.chess.GridPoint
import network.data.chess.Point
import network.data.vgw.VGWData
import network.data.vgw.VGWMove
import network.data.vgw.VGWPieceData
import network.data.vgw.VGWon
import tornadofx.*
import java.io.Serializable
import kotlin.concurrent.thread

val borderSize = arrayOf(30.0, 55.0, 30.0, 55.0)
class VGWBoard : Game() {
    var mouse = Point(0.0, 0.0)
    var test = 0

    var vgwW = -1.0
    private var vgwH = -1.0

    fun send(s: Serializable) {
        if (send) client.send(s)
    }

    fun setHover(i: Int) {
        for (i in columns) i.hover = false
        columns[i].hover = true
        paint()
    }

    lateinit var top: Rect
    lateinit var right: Rect
    lateinit var left: Rect
    lateinit var bottom: Rect
    var started: Boolean = false
        set(value) {
            field = value
            paint()
        }
    var finished = false

    override var move = "yellow"

    @Synchronized
    override fun paint() {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        paintBorder(gc)
        columns.forEach { it.paint(gc, Point(mouse.x.transX(), mouse.y.transY())) }
        gc.translate(-borderSize[1], -borderSize[0])
        if (!started) di(
            gc, Start.images["before-start-image2"]!!,
            Color(0.0, 0.0, 0.8, 0.5), 2.7
        )
    }
    fun paintBorder(gc: GraphicsContext) {
        val c = Color(0.0, 0.0, 1.0, 1.0)
        gc.fill = c

        left.paint(gc, "image", Start.images["vgw-left-side"]!!)
        right.paint(gc, "image", Start.images["vgw-right-side"]!!)

        gc.translate(borderSize[1], borderSize[0])
    }

    fun installBorders() {
        top = Rect(borderSize[1], 0.0, vgwW, borderSize[0])
        left = Rect(0.0, 0.0, borderSize[1], canvas.height)
        bottom = Rect(borderSize[1], vgwH + borderSize[0], vgwW, borderSize[2])
        right = Rect(vgwW + borderSize[1], 0.0, borderSize[3], canvas.height)
    }

    inner class UpperBorder : UpperComponentExample(arrayOf(
            "yellowLabel" to NameLabel(client.online(), client.userrole == "yellow") {
                client.changeName(it)
            }.apply { addClass("yellow-player") }
    ), arrayOf(
            "redLabel" to NameLabel(client.online(), client.userrole == "red") {
                client.changeName(it)
            }.apply { addClass("red-player") }
    )) {
        override fun neuladen() {
            Platform.runLater {
                m("yellowLabel").text = yellowUser?.o()?.name ?: "Warte..."
                m("redLabel").text = redUser?.o()?.name ?: "Warte..."

                if ((client.allOnline() && send) || !send) { if (!existPartie) newPartie() } // Parameter false wegen StackOverflowError!
                else { clearPartie() } // Parameter false wegen StackOverflowError!

                when (move) {
                    "yellow" -> { m("yellowLabel").addClass("move-player"); m("redLabel").removeClass("move-player") }
                    "red" -> { m("yellowLabel").removeClass("move-player"); m("redLabel").addClass("move-player") }
                }
            }
        }
    }
    inner class DownerBorder : View() {
        var really: Really? = null

        fun wrong(t: String) {
            info(t)
            really?.jes?.isDisable = false
            really?.cancel?.isDisable = false
        }

        override val root = hbox {
            bindVisible { primaryStage }
            isVisible = client.online().role != "viewer"
            useMaxSize = true
            alignment = Pos.CENTER
            spacing = 5.0
            button("Aufgeben") {
                addClass("blue-button")
                style { fontSize = 14.px }
                action { askAufgeben() }
            }
            button("Gleichstand anbieten") {
                addClass("blue-button")
                style { fontSize = 14.px }
                action { remisAnfordern("Gleichstand") }
            }
        }
    }

    fun data(): VGWData {
        var l: Array<Array<VGWPieceData?>> = arrayOf()
        for (c in columns) {
            l += c.data()
        }
        return VGWData(l)
    }
    fun set(vgw: VGWData) {
        existPartie = true
        for (x in vgw.pieces.indices) {
            columns[x].set(vgw.pieces[x])
        }
        printData()
    }

    @Synchronized
    fun pruefe(): Pair<String, Array<GridPoint>>? {
        fun pruefeColor(type: String): Pair<Boolean, Array<GridPoint>> {
            var arr: Array<GridPoint> = arrayOf()
            fun pruefePoint(x: Int, y: Int): Array<GridPoint>? {
                if (fieldOn(x, y).piece?.type == type && !arr.contains(GridPoint(x, y))) arr += GridPoint(x, y)
                else arr = arrayOf()
                if (arr.size == 4) return arr
                return null
            }
            arr = arrayOf()
            for (y in 0 until H) {
                for (x in 0 until W) pruefePoint(x, y)?.let { return Pair(true, it) }
            }
            arr = arrayOf()
            for (x in 0 until W) {
                for (y in 0 until H) pruefePoint(x, y)?.let { return Pair(true, it) }
            }
            arr = arrayOf()
            var y = 0
            var x = 0
            while (x < W) {
                var a = y
                var b = x
                arr = arrayOf()
                while (a >= 0 && b < W) {
                    pruefePoint(b, a)?.let { return Pair(true, it) }
                    b ++
                    a --
                }
                if (y < H-1) y ++
                else x ++
            }
            arr = arrayOf()
            y = H-1
            x = 0
            while (x < W) {
                var x2 = x
                var y2 = y
                arr = arrayOf()
                while (x2 < W && y2 < H) {
                    pruefePoint(x2, y2)?.let { return Pair(true, it) }
                    y2 ++
                    x2 ++
                }
                if (y > 0) y --
                else x ++
            }
            return Pair(false, arrayOf())
        }

        pruefeColor("red").let { if (it.first) return Pair("red", it.second) }
        pruefeColor("yellow").let { if (it.first) return Pair("yellow", it.second) }

        return null
    }
    fun setWonFields(a: Array<GridPoint>) {
        for (f in a) fieldOn(f.x, f.y).win = true
        for (c in columns) c.hover = false
        paint()
    }
    fun finishedMove() {
        thread {
            pruefe()?.let {
                thread {
                    finished = true
                    if (send) send(VGWon(it.second.clone()))
                    else setWonFields(it.second)
                    Thread.sleep(2500)
                    if (send)  gewonnen(when(it.first) {
                        "red" -> redUser!!.o().name
                        "yellow" -> yellowUser!!.o().name
                        else -> "ERROR"
                    })
                    else {
                        println("Gewonnen: ${it.first}")
                        newPartie()
                    }
                }
            }
            changeMove()
        }
    }
    fun hover(): Column? {
        for (i in columns) {
            if (i.hover) return i
        }
        return null
    }
    fun validMove() = started && (!send || (send && client.allOnline() && client.userrole == move)) && !finished
    fun changedMove() = when (move) {
        "red" ->  "yellow"
        "yellow" -> "red"
        else -> "ERROR"
    }
    fun changeMove() {
        if (send) send("CM${changedMove()}") else move = changedMove()
    }
    fun addPiece(i: Int) {
        columns[i].addPiece(move, validMove())
    }
    fun mousePressed(evt: MouseEvent) {
        if (validMove()) {
            val h = hover()
            if (h != null && !isFalling) {
                send(VGWMove(h.x, move))
                h.addPiece(move, validMove())
                h.hover = false
                send(data())
                paint()
            }
        }
    }
    fun mouseMoved(evt: MouseEvent) {
        if (validMove()) {
            mouse = Point(evt.x, evt.y)
            if (!isFalling) paint()
        }
    }

    override fun newPartie() {
        existPartie = true
        for (i in columns) {
            i.clear()
            for (f in i.f) f.win = false
        }
        finished = false
        move = "yellow"
        endDialog?.dialog?.schliessen()
        endDialog = null
        paint()
    }

    override fun clearPartie() {
        for (i in columns) i.clear()
        paint()
    }

    fun printData(d: VGWData = data()) {
        println()
        for (i in d.pieces[0].indices) {
            for (b in d.pieces.indices) {
                val o = d.pieces[b][i]
                print(" ${ if (o != null) {
                    if (o.c == "red") "r" else "g"
                } else "-" } ")
            }
            println()
        }
        println()
    }

    override fun gewonnen() { error("Diese Funktion nicht aufrufen!") }
    fun gewonnen(s: String) { if (send) send(Info(s, "dialog")) }
    override fun unentschieden() { if (send) send(Dialog("", "patt")) }
    override fun aufgegeben() { if (send) send(Info(client.username, "aufgegeben")) }

    override fun Canvas.installCanvas() {
        installBorders()
        this@VGWBoard.width = vgwW + borderSize[1] + borderSize[3]
        this@VGWBoard.height = vgwH + borderSize[0] + borderSize[2]
        width = this@VGWBoard.width!!
        height = this@VGWBoard.height!!
        setOnMouseMoved { mouseMoved(it) }
        setOnMousePressed { mousePressed(it) }
        val c = canvas
        Platform.runLater { c.replaceWith(this) }
    }

    override fun install() {
        if (send) {
            move = client.zwischenMove
            upperComponent = UpperBorder()
            downerComponent = DownerBorder()
            startDialog = StartDialog("vgw")
        }

        for (i in 0..H) { columns += Column(i, H-1) }
        vgwW = W * fieldSize
        vgwH = H * fieldSize
        installBorders()

        canvas.installCanvas()

        if (send) {
            vgwClient.data?.let { set(it) } ?: newPartie()
            started = client.allOnline()
        }
        paint()
    }

    companion object {
        var isFalling = false
        const val W = 7
        const val H = 6
    }
}

var columns: Array<Column> = arrayOf()
const val fieldSize = 75.0
const val ovalSize = 55.0

fun Double.transX() = this - borderSize[1]
fun Double.transY() = this - borderSize[0]

fun fieldOn(x: Int, y: Int) = columns[x].f[y]
fun fields(f: VGWField.() -> Unit) {
    columns.forEach { it1 ->
        it1.f.forEach { it.f() }
    }
}

class Column(val x: Int, c: Int) {
    fun set(d: Array<VGWPieceData?>) {
        f = arrayOf()
        for (i in d.indices) {
            f += VGWField(GridPoint(x, i)).apply { set(d[i]) }
        }
    }
    fun data(): Array<VGWPieceData?> {
        var a: Array<VGWPieceData?> = arrayOf()
        for (i in f) a += i.data()
        return a
    }
    var f: Array<VGWField> = arrayOf()
    init {
        for (y in 0..c) f += VGWField(GridPoint(x, y))
    }
    fun clear() {
        for (i in f) i.piece = null
    }
    fun addPiece(type: String, cma: Boolean): Boolean {
        vgw.test ++
        for (i in f.size-1 downTo 0) {
            if (f[i].piece == null) {
                f[i].addPiece(type, cma)
                return true
            }
        }
        isFull = true
        return false
    }
    var hover = false
    var lastHover = false
    var isFull = false
    fun paint(gc: GraphicsContext, p: Point) {
        f.filter { it.piece?.finished == false }.forEach { it.piece?.paint(gc) }
        f.forEach { it.paint(gc) }
        f.filter { it.piece?.finished == true }.forEach { it.piece?.paint(gc) }

        if (!isFull) {
            val rect = Rect(f[0].p.x, f[0].p.y, fieldSize, f.size * fieldSize)
            if ((game.send && client.userrole == vgw.move) || !game.send) {
                hover = contains(p, rect)
                if (hover && hover != lastHover) vgw.send(Info(x.toString(), "hover"))
                lastHover = hover
            }

            if (hover) {
                gc.fill = Color(0.0, 129.0/255.0, 1.0, 0.4)
                rect.paint(gc, "fill")
            }
        }
    }
}

class VGWField(val gp: GridPoint) {
    var win = false
    val p: Point
    fun p() = Point(gp.x * fieldSize, gp.y * fieldSize)
    var piece: VGWPiece? = null
    fun addPiece(type: String, cma: Boolean) {
        val x = p.x + fieldSize/2.0
        piece = VGWPiece(type, gp, cma)
        piece!!.p = Point(x, fieldSize/2.0)
    }

    init { p = p() }

    fun paint(gc: GraphicsContext) {
        gc.drawImage(image, p.x, p.y, fieldSize, fieldSize)
        if (win) {
            gc.fill = Color(1.0,0.0,0.0, 0.4)
            gc.fillRoundRect(p.x, p.y, fieldSize, fieldSize, 40.0, 40.0)
        }
    }
    fun set(g: VGWPieceData?) {
        if (g != null) {
            piece = VGWPiece(g.c, g.pp, false)
            piece!!.finished = true
        } else piece = null
    }
    fun data() = piece?.data()
    companion object {
        val image = Image("image/vgwpart.png")
    }
}
class VGWPiece(val type: String, val gp: GridPoint, val cma: Boolean) {
    var p = Point(gp.x * fieldSize + fieldSize/2.0, gp.y * fieldSize + fieldSize/2.0)
    var pp = p
    var fallSpeed = 1.5
    var slidingx = true
    var slidingy = true
    var finished = false
    fun slide(z1: Double, z2: Double): Double? {
        fallSpeed *= 1.01
        if (!inRange(z1, z2, fallSpeed)) {
            if (z1 > z2) return z2 + fallSpeed
            if (z1 < z2) return z2 - fallSpeed
        }
        return null
    }
    init {
        repaintTimer.start()
        VGWBoard.isFalling = true
    }
    fun paint(gc: GraphicsContext) {
        if (!finished) {
            p.x = slide(pp.x, p.x) ?: run { slidingx = false; p.x }
            p.y = slide(pp.y, p.y) ?: run { slidingy = false; p.y }

            if (!slidingx && !slidingy) {
                p = pp.copy()
                VGWBoard.isFalling = false
                repaintTimer.stop()
                if (cma) vgw.finishedMove()
                vgw.send("reload canvas")
                finished = true
            }
        }

        gc.fill = if (type == "red") Color.RED else Color.YELLOW
        gc.stroke = Color.BLACK
        gc.fillOval(p.x - ovalSize/2.0, p.y - ovalSize/2.0, ovalSize, ovalSize)
        gc.strokeOval(p.x - ovalSize/2.0, p.y - ovalSize/2.0, ovalSize, ovalSize)
    }
    fun data() = VGWPieceData(gp, type)
}
fun inRange(z1: Double, z2: Double, f: Double) = z1 in (z2-f)..(z2+f)
fun contains(p: Point, r: Rect) = p.x > r.x && p.x < r.x+r.w && p.y > r.y && p.y < r.y+r.h
data class Rect(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun paint(gc: GraphicsContext, s: String, image: Image? = null) {
        when (s) {
            "fill" -> gc.fillRect(x, y, w, h)
            "draw" -> gc.strokeRect(x, y, w, h)
            "image" -> gc.drawImage(image!!, x, y, w, h)
        }
    }
}

class VGWView : StandartGameView(VGWBoard())
class StartVGW : View() {
    override val root = vbox {
        game = VGWBoard()
        game.send = false
        game.install()
        spacing = 3.0

        this += game.canvas()
        button("neu laden") {
            action {
                game.reloadCanvas()
            }
        }
    }
}
