package main

import gui.game.wizard.WizardBoard
import javafx.scene.image.Image

class Start {
    companion object {
        var picefigures: Map<String, Image> = mapOf()
        var wizardcards: Map<String, Image> = mapOf()
        var images: Map<String, Image> = mapOf()

        const val W_WIDTH = 614.0
        const val W_HEIGHT = 986.0
        val stylesheet = "style/style.css"
        val homepage = "http://spielekiste.maxi.li/"
        var sources = mapOf<String,String>()

        @JvmStatic
        fun installImages() {
            picefigures += "queen black" to Image("image/queenBlack.png")
            picefigures += "queen white" to Image("image/queenWhite.png")
            picefigures += "rook black" to Image("image/rookBlack.png")
            picefigures += "rook white" to Image("image/rookWhite.png")
            picefigures += "bishop black" to Image("image/bishopBlack.png")
            picefigures += "bishop white" to Image("image/bishopWhite.png")
            picefigures += "pawn black" to Image("image/pawnBlack.png")
            picefigures += "pawn white" to Image("image/pawnWhite.png")
            picefigures += "king black" to Image("image/kingBlack.png")
            picefigures += "king white" to Image("image/kingWhite.png")
            picefigures += "knite black" to Image("image/kniteBlack.png")
            picefigures += "knite white" to Image("image/kniteWhite.png")

            WizardBoard.installCards { c, s -> wizardcards += "$c-$s" to Image("image/wizard/"+c+"_"+s+".png") }
            WizardBoard.installWizardCards()

            images += "wizard-back" to Image("image/wizard/0_0.png")
            images += "drag-over-impossible" to Image("image/impossible-move.png")
            images += "drag-over-possible" to Image("image/possible-move.png")
            images += "wood-background" to Image("image/wood-background.jpg")
            images += "winner-end" to Image("image/winner.png")
            images += "patt-end" to Image("image/patt.png")
            images += "aufgeben-end" to Image("image/aufgeben.png")
            images += "winners-end" to Image("image/winners.png")
            images += "before-start-image" to  Image("image/roundnotfinishedyet.png")
            images += "before-start-image2" to  Image("image/spielnochnichtgestartet.png")
            images += "main-icon" to  Image("image/rotatedDice.png")
            images += "info" to  Image("image/info.png")
            images += "error" to  Image("image/error.png")

            images += "hand-move" to  Image("image/hand-move.png")
            images += "oval-stroke" to Image("image/ovalStroke.png")
            images += "oval-fill" to Image("image/ovalFill.png")
            images += "path-stroke" to Image("image/pathStroke.png")
            images += "path-fill" to Image("image/pathFill.png")
            images += "line" to Image("image/line.png")
            images += "pencil" to Image("image/pencil.png")
            images += "trash" to Image("image/trash.png")
            images += "back" to Image("image/back.png")
            images += "arrow-back" to Image("image/arrowBack.png")
            images += "arrow-fore" to Image("image/arrowFore.png")
            images += "save" to Image("image/save.png")
            images += "reset-offsets" to Image("image/offsetsBck.png")
            images += "vgw-left-side" to Image("image/leftSide.png")
            images += "vgw-right-side" to Image("image/rightSide.png")
            images += "vgw-icon" to Image("image/connect4.png")
            images += "ki-symbol" to Image("image/ai.png")

            /*File(javaClass.classLoader.getResource("file/sources.txt")!!.toURI()).forEachLine {
                val p = "([\\w\\- ]+): ([\\w:/\\.\\-_]+)$"
                val m = Pattern.compile(p).matcher(it)
                val m = Pattern.compile(p).matcher(it)
                m.find()
                sources += m.group(1) to m.group(2)
            }*/

            //if (parameter.raw.isEmpty()) Thread.sleep(3500)
        }
    }
}