package gui

import javafx.scene.control.Label
import tornadofx.*

class GameTimer : View("My View") {
    lateinit var label: Label
    fun reloadLabel() {
        val s = 3
        val f = 60
        val t = Array(s) { 0 }
        var u = time.toDouble()

        for (i in t.indices) {
            t[i] = u.toInt() % f
            u /= 60.0
        }

        fun f(i: Int) = String.format("%1$02d", i)
        //label.text = "${f(t[2])}:${f(t[1])}:${f(t[0])}"
        label.text = "gegen"
    }
    override val root = label {
        label = this
        addClass("timer-label")
    }
    init {
        timers += this
        reloadLabel()
    }
    companion object {
        var time = 0
        var timers: Array<GameTimer> = arrayOf()
    }
}
