package game.chess

import main.chess
import main.game
import main.Start.Companion.installImages
import tornadofx.*


class StartChess : View() {
    override val root = borderpane {
        center {
            installImages()

            game = ChessBoard()
            chess.send = false
            chess.install()
            chess.paint()

            this += game.canvas()
        }
    }
}