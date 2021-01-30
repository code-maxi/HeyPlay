package network.data.wizard

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import main.Start
import network.data.Online
import network.data.OnlineI
import java.io.Serializable
import java.lang.Exception
import java.util.regex.Pattern

var WIZARD_CARDS: ArrayList<WizardCard> = ArrayList()

data class WizardCard(
        val color: String = "r",
        val type: String = "1",
        var owner: String = "n"
) : Serializable {
    var ay: Double? = null
    val sY = 320.0
    var y = sY

    fun type() = try { Integer.parseInt(type) } catch(e: Exception) { null }
    fun image() = Start.wizardcards["$color-$type"] ?: error("cant find my card: $this")
    fun back() = Start.images["wizard-back"]!!
    fun format() = "$color-$type-$owner"
    fun draw(g2: GraphicsContext, x: Double, y: Double, f: Double, b: Boolean = false) {
        g2.translate(x,y)
        val i = if (b) back() else image()
        val w = i.width/f
        val h = i.height/f
        val c = 6.0
        g2.drawImage(i, c, c, w, h)
        g2.stroke = Color.BLACK
        g2.lineWidth = c
        g2.strokeRoundRect(c / 2.0, c / 2.0, w + c, h + c, c * 2, c * 2.5)
        g2.translate(-x,-y)
    }
    fun y() = ay!! + y
    fun animate(d: String, f: () -> Unit) {
        when (d) {
            "bottom" -> {
                if (y > 1.0) y *= 0.9
                else f()
            }
            "top" -> {
                if (y < sY) y *= 1.1
                else {
                    f()
                }
            }
        }
    }
    companion object {
        fun parse(s: String): WizardCard {
            val r = "([a-z])\\-(\\w\\d?)\\-(\\w+)"
            val m = Pattern.compile(r).matcher(s)
            m.find()
            return WizardCard(m.group(1), m.group(2), m.group(3))
        }
    }
}

data class WizardOnline(
    var o: Online,
    var points: Int = 0,
    var lastPlus: Int = 0,
    var vstiche: Int = 0,
    var astiche: Int = 0
) : OnlineI {
    override fun o() = o
    override fun clone() = copy()
}

data class WizardOnlines(val list: ArrayList<WizardOnline>) : Serializable
data class TableCards(val list: ArrayList<WizardCard>) : Serializable
data class WizardMove(val move: Int = -1, val dealer: Int = -1, val trumpf: WizardCard? = null) : Serializable
data class VorgeschlagenItem(val text: String, val thema: String, val from: Int) : Serializable
data class VorgeschlagenItemBack(val text: String, val thema: String) : Serializable
data class VorgeschlagenFinished(val b: Boolean, val thema: String, val own: Boolean) : Serializable
data class Cards(val list: ArrayList<WizardCard>) : Serializable
data class HellsehenData(val list: ArrayList<WizardCard>) : Serializable