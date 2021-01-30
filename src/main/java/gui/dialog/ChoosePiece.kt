package gui.dialog

import tornadofx.*

class ChoosePiece(val color: String, val f: (String) -> Unit) : View() {
    override val root = gridpane {
        hgap = 5.0
        addClass("choose-piece-container")
        row {
            fun addButton(ss: String) {
                val s = "image/$ss${if (color == "white") "White" else "Black"}.png"
                button(graphic = imageview(s) {
                    fitWidth = 40.0
                    fitHeight = 40.0
                }) {
                    addClass("choose-piece-image")
                    action {
                        f(ss)
                        close()
                    }
                }
            }
            addButton("queen")
            addButton("rook")
            addButton("bishop")
            addButton("knite")
        }
    }
}
