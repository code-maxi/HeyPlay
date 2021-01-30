package game.chess

import game.*
import game.chess.chessPieces.*
import gui.dialog.ChoosePiece
import adds.Information
import adds.Really
import adds.di
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import main.*
import network.data.Info
import network.data.chess.ChessData
import network.data.chess.ChessFieldData
import network.data.chess.GridPoint
import network.data.chess.Point
import tornadofx.*
import java.io.Serializable
import java.lang.Math.random


val fieldSize = 70.0
val black = Color(80.0/255.0, 80.0/255.0, 80.0/255.0, 1.0)
val white = Color(220.0/255.0, 220.0/255.0, 220.0/255.0, 1.0)

val W = 8
val H = 8

class ChessBoard : Game() {
    var fields: Array<Array<ChessField>> = arrayOf()
    val borderSize = 10

    val dborder: DownerBorder
    get() = downerComponent as DownerBorder

    override var move = "black"

    var started = false
        set(value) {
            field = value
            paint()
        }

    override fun gewonnen() { if (send) send(Info(client.username, "gewonnen")) }
    override fun unentschieden() { if (send) send(Info(client.username, "unentschieden")) }
    override fun aufgegeben() { if (send) send(Info(client.username, "aufgegeben")) }
    fun remis() { send(Info(client.username, "remis")) }

    fun data(): ChessData {
        var f: Array<Array<ChessFieldData>> = arrayOf()
        for (a in fields) {
            var l: Array<ChessFieldData> = arrayOf()
            for (b in a) l += b.data()
            f += l
        }
        return ChessData(
            (random() * 10000.0).toInt(),
            move,
            f
        )
    }

    fun set(c: ChessData) {
        move = c.move
        for (a in fields.indices) {
            for (b in fields[a].indices) fields[a][b].set(c.fields[a][b])
        }
        upperComponent.neuladen()
    }

    var bottom = ""

    override fun paint() {
        val g2 = canvas.graphicsContext2D

        g2.fill = Color.BLACK
        g2.lineWidth = borderSize.toDouble()
        val b2 = (borderSize / 2).toDouble()
        g2.strokeRect(b2, b2, getW()-borderSize, getH()-borderSize)

        val l1 = { e: ChessField ->
            e.draw(g2, bottom)
        }
        val l2 = { it: ChessField ->
            it.drawAfter(g2, bottom)
            if (it.piece?.id ?: -1 != touchedField?.piece?.id ?: -1) {
                it.piece?.draw(g2)
            }
        }
        val l3 = { it: ChessField ->
            if (it.piece?.id ?: -1 == touchedField?.piece?.id ?: -1) {
                it.piece?.draw(g2)
            }
        }

        fields(l1)
        fields(l2)
        fields(l3)

        if (!started) di(g2, Start.images["before-start-image2"]!!, f = 2.7)
    }

    fun installFields() {
        var black1 = false
        var black2: Boolean
        for (x in 0 until W) {
            black2 = black1
            var a: Array<ChessField> = arrayOf()
            for (y in 0 until H) {
                a += ChessField(if (black2) "black" else "white", GridPoint(x, y))
                black2 = !black2
            }
            fields += a
            black1 = !black1
        }
    }

    fun getW() = W * fieldSize + borderSize * 2
    fun getH() = H * fieldSize + borderSize * 2

    fun getW2() = (W -1) * fieldSize + borderSize * 2
    fun getH2() = (H -1) * fieldSize + borderSize * 2

    override fun Canvas.installCanvas() {
        this@ChessBoard.width = getW()
        this@ChessBoard.height = getH()
        setOnMousePressed { mousePressed(it) }
        setOnMouseDragged { mouseDragged(it) }
        setOnMouseReleased { mouseReleased(it) }
    }

    override fun install() {
        if (send) upperComponent = UpperBorder()
        if (send) downerComponent = DownerBorder()

        bottom = if (send) {
            if (client.userrole != "viewer") client.userrole else "white"
        } else "white"

        canvas.installCanvas()
        installFields()

        started = client.allOnline()

        if ((send && client.zwischenNewPartie) || !send) newPartie()
    }

    fun getTouchedField(evt: MouseEvent): ChessField? {
        val e = OwnEvent(evt)
        loop@ for (x in fields) {
            for (y in x) {
                if (y.contains(Point(e.x, e.y))) {
                    return y
                }
            }
        }
        return null
    }

    lateinit var oldEvent: MouseEvent
    var touchedField: ChessField? = null
    var hoverField: ChessField? = null
    lateinit var king: King

    fun setKing(check: (King) -> Boolean = { it.color == move }) {
        fields {
            if (it.piece is King && check(it.piece as King)) king = it.piece!! as King
        }
    }

    fun sendData() {
        if ((send && client.userrole != "viewer") || !send) {
            send(data())
        }
    }

    fun enPassant(p: Pawn, field: ChessField) {
        val g = if (p.color == "white") 1 else -1
        if (field.piece == null && field.pp.y == p.pp.y-g) {
            when (field.pp.x) {
                p.pp.x+1 -> fields[p.pp.x+1][p.pp.y].piece = null
                p.pp.x-1 -> fields[p.pp.x-1][p.pp.y].piece = null
            }
        }
    }

    fun changePiece(p: Pawn, field: ChessField) {
        if ((p.color == "white" && p.pp.y == 0) || (p.color == "black" && p.pp.y == H-1)) {
            ChoosePiece(p.color) {
                val np =
                        if (it == "rook") Rook(p.color)
                        else if (it == "queen") Queen(p.color)
                        else if (it == "knite") Knite(p.color)
                        else Bishop(p.color)

                field.piece = np
                king.schach()
                sendData()
                paint()
            }
        }
    }

    fun mousePressed(evt: MouseEvent) {
        if (existPartie) {
            canvas.cursor = Cursor.MOVE

            setKing()
            king.schach()

            touchedField = getTouchedField(evt)
            if (touchedField != null && touchedField!!.piece != null && validMove(touchedField!!.piece!!)) {
                oldEvent = evt
                fields {
                    it.possible = false
                    it.hover = false
                    it.rochade = false
                    it.lastmove = false
                }
                touchedField?.apply {
                    piece?.possible { it.possible = true }
                    piece?.moving = true
                    if (piece is King) {
                        val p = (piece as King)
                        p.rochiere()
                    }
                    from = true
                }
            }
            sendData()
        }
        paint()
    }

    fun mouseReleased(evt: MouseEvent) {
        if (existPartie) {
            canvas.cursor = Cursor.DEFAULT

            touchedField?.piece?.moving = false
            val field = getTouchedField(evt)

            if (
                field != null &&
                touchedField?.piece != null &&
                validMove(touchedField!!.piece!!) &&
                (field.possible || field.rochade) &&
                field.pp != touchedField!!.pp
            ) {
                touchedField!!.lastmove = true
                field.lastmove = true

                if (touchedField!!.piece!! is Pawn) { enPassant((touchedField!!.piece as Pawn), field) }

                field.piece = touchedField!!.piece
                field.piece!!.moved ++

                if (field.rochade) {
                    val p = touchedField!!.piece as King
                    fun r(r: King.RochadeData) {
                        if (r.p != null && r.p!!.contains(field.piece!!.p)) {
                            r.rp!!.piece = r.r!!.piece
                            r.r!!.piece = null
                        }
                    }
                    r(p.rochadeRight)
                    r(p.rochadeLeft)
                }

                if (field.piece!! is Pawn) { changePiece((field.piece as Pawn), field) }

                touchedField!!.piece = null
                touchedField = field

                if (move == "white") move = "black"
                else if (move == "black") move = "white"


                if (send) upperComponent.neuladen()

                fields { it.piece?.schachPossble = false }


                setKing()
                king.schach()

            }
            else {
                touchedField?.piece = touchedField?.piece
            }

            hoverField = null
            fields {
                it.possible = false
                it.from = false
                it.hover = false
                it.rochade = false
            }

            sendData()
            paint()
            send("reload canvas")
        }
    }

    inner class OwnEvent(evt: MouseEvent) {
        val x: Double = if (bottom == "black") (getW() - evt.x) else evt.x.toDouble()
        val y: Double = if (bottom == "black") (getH() - evt.y) else evt.y.toDouble()
    }

    fun rochade(p: King) {
        fun ff(f: King.RochadeData) {
            if (f.r?.piece != null) {
                if (f.p != null && f.p!!.contains(p.p)) {
                    f.r!!.piece!!.p = f.rp!!.center.copy()
                }
                else {
                    f.r!!.piece = f.r!!.piece
                }
                send(f.r!!.piece!!.data())
            }
        }
        ff(p.rochadeLeft)
        ff(p.rochadeRight)
    }

    fun validMove(piece: ChessPiece) =
            started &&
            piece.color == move &&
            ((send && client.userrole == move) || !send) &&
            ((piece.schachPossble == true && king.schach) || (piece.schachPossble == null && !king.schach))

    var ttt = 0
    fun mouseDragged(evt: MouseEvent) {
        if (existPartie) {
            touchedField?.piece?.let {
                if (validMove(it)) {
                    hoverField?.hover = false
                    hoverField = getTouchedField(evt)
                    hoverField?.hover = true

                    val o = OwnEvent(evt)
                    it.p = Point(o.x, o.y)
                    if (it is King) rochade(it)

                    send(it.data())
                    paint()
                }
            }
        }
    }

    inner class UpperBorder : UpperComponentExample(arrayOf(
            "whiteLabel" to NameLabel(client.online(), client.userrole == "white") {
                client.changeName(it)
            }.apply { addClass("white-player") }
    ), arrayOf(
            "blackLabel" to NameLabel(client.online(), client.userrole == "black") {
                client.changeName(it)
            }.apply { addClass("black-player") }
    )) {
        override fun neuladen() {
            Platform.runLater {
                m("blackLabel").text = blackUser?.o()?.name ?: "Warte..."
                m("whiteLabel").text = whiteUser?.o()?.name ?: "Warte..."

                /*if (client.allOnline()) { if (!existPartie) newPartie(false) } // Parameter false wegen StackOverflowError!
                else { clearPartie(false) } // Parameter false wegen StackOverflowError!*/

                when (move) {
                    "black" -> { m("blackLabel").addClass("move-player"); m("whiteLabel").removeClass("move-player") }
                    "white" -> { m("blackLabel").removeClass("move-player"); m("whiteLabel").addClass("move-player") }
                }
            }
        }
    }

    inner class DownerBorder : View() {
        var really: Really? = null

        fun wrong(t: String) {
            Information(t)
            really?.jes?.isDisable = false
            really?.cancel?.isDisable = false
        }

        private fun change() {
            when (bottom) {
                "white" -> bottom = "black"
                "black" -> bottom = "white"
            }
            this@ChessBoard.paint()
        }

        override val root = borderpane {
            useMaxSize = true
            left {
                button("Schachfeld umdrehen") {
                    style { fontSize = 14.px }
                    action { change() }
                }
            }
            right {
                hbox {
                    useMaxSize = true
                    alignment = Pos.CENTER
                    button("Aufgeben") {
                        addClass("blue-button")
                        style { fontSize = 14.px }
                        hboxConstraints {
                            marginRight = 5.0
                        }
                        action { askAufgeben() }
                    }
                    button("Remis anbieten") {
                        addClass("blue-button")
                        style { fontSize = 14.px }
                        action { remisAnfordern("Gleichstand") }
                    }
                }
            }
        }
    }

    fun fields(f: (ChessField) -> Unit) {
        for (i in fields) {
            for (i2 in i) {
                f(i2)
            }
        }
    }

    fun send(s: Serializable) {
        if (send) client.send(s)
    }

    override fun newPartie() {
        newPartie(send)
    }

    fun newPartie(reload: Boolean) {
        ChessPiece.ID = 0
        ChessPiece.chessPieces = mapOf()
        fields { it.piece = null }
        move = "white"
        if (reload) upperComponent.neuladen()

        val w = W - 1
        val h = H - 1

        fun b(x: Int, y: Int, c: String) { fields[x][y].piece = Pawn(c) }
        fun t(x: Int, y: Int, c: String) { fields[x][y].piece = Rook(c) }
        fun s(x: Int, y: Int, c: String) { fields[x][y].piece = Knite(c) }
        fun l(x: Int, y: Int, c: String) { fields[x][y].piece = Bishop(c) }
        fun k(x: Int, y: Int, c: String) { fields[x][y].piece = King(c) }
        fun q(x: Int, y: Int, c: String) { fields[x][y].piece = Queen(c) }

        b(0, h-1, "white")
        b(1, h-1, "white")
        b(2, h-1, "white")
        b(3, h-1, "white")
        b(4, h-1, "white")
        b(5, h-1, "white")
        b(6, h-1, "white")
        b(7, h-1, "white")

        b(0, 1, "black")
        b(1, 1, "black")
        b(2, 1, "black")
        b(3, 1, "black")
        b(4, 1, "black")
        b(5, 1, "black")
        b(6, 1, "black")
        b(7, 1, "black")

        s(1, h, "white")
        s(w-1, h, "white")

        s(1, 0, "black")
        s(w-1, 0, "black")

        l(2, h, "white")
        l(w-2, h, "white")

        l(2, 0, "black")
        l(w-2, 0, "black")

        t(0, h, "white")
        t(w, h, "white")

        t(0, 0, "black")
        t(w, 0, "black")

        k(4, h, "white")
        q(3, h, "white")

        k(4, 0, "black")
        q(3, 0, "black")

        setKing()
        king.schach()

        existPartie = true

        paint()
    }

    override fun clearPartie() { clearPartie(true) }

    fun clearPartie(reload: Boolean) { // Parameter wegen StackOverflowError!
        client.zwischenNewPartie = false

        if (existPartie) {
            ChessPiece.ID = 0
            ChessPiece.chessPieces = mapOf()
            fields { it.piece = null }

            move = "white"
            if (reload) upperComponent.neuladen()

            existPartie = false
            paint()
        }
    }

    fun createPartie2(reload: Boolean) {
        ChessPiece.ID = 0
        ChessPiece.chessPieces = mapOf()
        fields { it.piece = null }
        move = "black"
        if (reload) upperComponent.neuladen()

        val w = W - 1
        val h = H - 1

        fun b(x: Int, y: Int, c: String) { fields[x][y].piece = Pawn(c) }
        fun t(x: Int, y: Int, c: String) { fields[x][y].piece = Rook(c) }
        fun s(x: Int, y: Int, c: String) { fields[x][y].piece = Knite(c) }
        fun l(x: Int, y: Int, c: String) { fields[x][y].piece = Bishop(c) }
        fun k(x: Int, y: Int, c: String) { fields[x][y].piece = King(c) }
        fun q(x: Int, y: Int, c: String) { fields[x][y].piece = Queen(c) }

        /*b(0, h-1, "white")
        b(1, h-1, "white")
        b(2, h-1, "white")
        b(3, h-1, "white")
        b(4, h-1, "white")
        b(5, h-1, "white")
        b(6, h-1, "white")
        b(7, h-1, "white")

        b(0, 1, "black")
        b(1, 1, "black")
        b(2, 1, "black")
        b(3, 1, "black")
        b(4, 1, "black")
        b(5, 1, "black")
        b(6, 1, "black")
        b(7, 1, "black")*/

        b(4, 4, "black")
        b(5, h-1, "white")


        s(1, h, "white")
        s(w-1, h, "white")

        s(1, 0, "black")
        s(w-1, 0, "black")

        l(2, h, "white")
        l(w-2, h, "white")

        l(2, 0, "black")
        l(w-2, 0, "black")

        t(w, h, "white")
        t(0, 0, "black")

        t(w, 0, "black")
        t(0, h, "white")

        k(4, h, "white")
        q(3, h, "white")

        k(4, 0, "black")
        q(3, 0, "black")

        setKing()
        king.schach()

        existPartie = true

        paint()
    }

    fun printData(c: ChessData = data()) {
        var s = ""
        c.apply {
            for (x in fields.indices) {
                var s2 = ""
                for (y in fields[x].indices) {
                    s2 += when (fields[y][x].piece?.type) {
                        "king" -> " k "
                        "queen" -> " q "
                        "rook" -> " t "
                        "bishop" -> " l "
                        "pawn" -> " b "
                        "knite" -> " s "
                        else -> "==="
                    }
                }
                s += "$s2\n"
            }
        }
    }
}

class ChessView : StandartGameView(ChessBoard())