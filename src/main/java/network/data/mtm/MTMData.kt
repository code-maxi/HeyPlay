package network.data.mtm

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import adds.copy
import network.data.Online
import network.data.chess.Point
import java.io.Serializable
import java.util.*

interface MTMShape : Serializable {
    fun oval() = if (this is MTMOval) this else null
    fun path() = if (this is MTMPath) this else null

    fun kopieren(): MTMShape
    fun paint(gc: GraphicsContext)
}
data class MTMColor(var r: Double, var g: Double, var b: Double, var a: Double = 1.0) : Serializable {
    fun color() = Color(r,g,b,a)
}
data class MTMData(val shapes: Array<MTMShape> = arrayOf()) : Serializable

data class MTMPaintData(var color: MTMColor, var width: Double) : Serializable {
    fun setTo(gc: GraphicsContext) {
        gc.stroke = color.color()
        gc.fill = color.color()
        gc.lineWidth = width
    }
}
data class MTMOval(val m: MTMPaintData, val fill: Boolean, var p1: Point, var p2: Point = p1.copy()) : MTMShape {
    override fun paint(gc: GraphicsContext) {
        m.setTo(gc)
        if (!fill) gc.strokeOval(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y)
        else gc.fillOval(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y)
    }

    override fun kopieren() = copy()
}
data class MTMPath(val m: MTMPaintData, val fill: Boolean, var points: Array<Point> = arrayOf()) : MTMShape {
    override fun paint(gc: GraphicsContext) {
        m.setTo(gc)
        gc.beginPath()
        for (i in points) {
            if (i === points.first()) gc.moveTo(i.x, i.y)
            gc.lineTo(i.x, i.y)
        }
        if (fill) {
            gc.lineWidth = 2.0
            gc.closePath()
            gc.fill()
        }
        gc.stroke()
    }
    override fun kopieren() = MTMPath(m.copy(), fill, points.clone())
}
data class MTMouse(
        var m: MTMPaintData = MTMPaintData(Color.GREEN.data(), 20.0),
        var fill: Boolean = false,
        var p: Point = Point(0.0, 0.0)
) : Serializable {
    fun clone() = MTMouse(m.copy(), fill, p.copy())
    fun paint(gc: GraphicsContext) {
        if (!fill) {
            gc.stroke = m.color.color()
            gc.lineWidth = 3.0
            gc.strokeOval(p.x - m.width/2.0, p.y - m.width/2.0, m.width, m.width)
        }
        else {
            gc.fill = m.color.color()
            val s = 10.0
            gc.stroke = Color.BLACK
            gc.lineWidth = 1.0
            gc.fillOval(p.x - s/2.0, p.y - s/2.0, s, s)
            gc.strokeOval(p.x - s/2.0, p.y - s/2.0, s, s)
        }
    }
}
data class MTMOnlines(val onlines: Array<Online>) : Serializable
data class MTMShapeSet(val shape: MTMShape, val add: Int) : Serializable {
    fun clone() = MTMShapeSet(shape.kopieren(), add)
}
data class Anfrage(val id: Int, val text: String, val name: String) : Serializable
data class AnfrageBack(val a: Anfrage, val bewertung: String) : Serializable
data class AnfragenWrong(var list: ArrayList<AnfrageBack> = arrayListOf()): Serializable {
    fun clone() = AnfragenWrong(list.copy { it.copy() })
}
fun Color.data() = MTMColor(red, green, blue, opacity)