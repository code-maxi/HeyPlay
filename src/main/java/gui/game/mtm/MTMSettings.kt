package gui.game.mtm

import adds.really
import main.mtm
import javafx.geometry.Pos
import javafx.scene.control.ButtonBase
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import adds.ownButton
import network.data.mtm.data
import tornadofx.*
import kotlin.math.roundToInt

fun ToggleButton.install(g: String, w: Double = 20.0, h: Double = 20.0, a: () -> Unit) {
    graphic = ImageView(g).apply {
        fitWidth = w
        fitHeight = h
    }
    addClass("mtm-settings-togglebutton")
    action { a() }
}
class MTMSettings : View() {
    var s = mapOf<Int, ToggleButton>()
    override val root = borderpane {
        useMaxWidth = true
        addClass("mtm-settings-pane")

        fun ob(
            t: ToggleGroup, i: String, tt: String, ty: Int,
            si: Double, c: String? = null, f: ButtonBase.() -> Unit
        ) = ownButton(
            true, i = i, tg = t, s = si,
            p = 5.0 to 5.0, tt = tt, f = f, c = c
        ).apply { s += ty to (this as ToggleButton) } as ToggleButton

        left {
            hbox {
                alignment = Pos.CENTER
                useMaxHeight = true
                colorpicker {
                    value = Color.BLACK
                    fun set(s: Color = value) {
                        mtm.mouse.m.color = s.data()
                        mtm.paint()
                    }
                    set()
                    valueProperty().onChange { it?.let { set(it) } }
                    hboxConstraints { marginRight = 15.0 }
                }
                hbox {
                    alignment = Pos.CENTER
                    useMaxHeight = true
                    spacing = 2.0
                    val l = Label()
                    slider(1.0, 150.0, 10.0) {
                        fun set(s: Double) {
                            l.text = "${s.roundToInt()}px"
                            mtm.mouse.m.width = s
                            mtm.paint()
                        }
                        set(value)
                        valueProperty().onChange { set(it) }
                    }
                    this += l
                }
            }
        }
        right {
            hbox {
                alignment = Pos.CENTER
                useMaxHeight = true
                spacing = 10.0

                hbox {
                    spacing = 5.0
                    val t = ToggleGroup()

                    fun oob(i: String, tt: String, ty: Int) {
                        this += ownButton(
                            true, i = i, tg = t, s = 22.0,
                            p = 8.0 to 8.0, tt = tt, f = { mtm.type = ty }, c = "small-button"
                        ).apply { s += ty to (this as ToggleButton) }
                    }

                    oob("oval-stroke", "Nicht gefülltes Oval", MTMBoard.OVAL_STROKE)
                    oob("oval-fill", "gefülltes Oval", MTMBoard.OVAL_FILL)
                    oob("path-stroke", "Nicht gefüllter Pfad", MTMBoard.PATH_STROKE)
                    oob("path-fill", "gefüllter Pfad", MTMBoard.PATH_FILL)
                    oob("line", "Linie", MTMBoard.LINE)
                    oob("pencil", "Pinsel", MTMBoard.PENCIL )
                    oob("hand-move", "Ansicht verschieben", MTMBoard.HAND)

                    hboxConstraints { marginRight = 10.0 }
                }
                hbox {
                    spacing = 5.0
                    useMaxHeight = true
                    useMaxWidth = false
                    alignment = Pos.CENTER
                    val t = ToggleGroup()

                    fun oob(i: String, tt: String, ty: Int) = ob(t, i, tt, ty, 20.0) { mtm.add = ty }.apply { this@hbox += this }
                    oob("arrow-back", "Hinten Einfügen", MTMBoard.HINTEN)
                    oob("arrow-fore", "Vorne Einfügen", MTMBoard.VORNE).isSelected = true
                    hboxConstraints { marginRight = 10.0 }
                }
                hbox {
                    alignment = Pos.BASELINE_CENTER
                    spacing = 5.0
                    this += ownButton(
                        false, i = "save", s = 15.0,
                        tt = "Bild speichern", c = "mtm-settings-trash-button"
                    ) { mtm.save() }
                    this += ownButton(
                        false, i = "reset-offsets", s = 20.0,
                        tt = "Position und Skalierung zurücksetzen", c = "mtm-settings-trash-button"
                    ) { mtm.resetOffsets() }
                    this += ownButton(
                        false, i = "trash", s = 19.0,
                        tt = "Zeichenfläche leeren", c = "mtm-settings-trash-button"
                    ) { really("Willst du wirklich die Zeichenfläche leeren?") { mtm.clearPartie() } }
                    this += ownButton(
                        false, i = "back", s = 26.0,
                        tt = "Rückgängig", c = "mtm-settings-back-button"
                    ) {
                        if (mtm.shapes.isNotEmpty()) mtm.removeLast()
                        mtm.paint(true)
                    }
                }
            }
        }
    }
}