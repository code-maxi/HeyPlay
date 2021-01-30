package network

import main.game
import main.igi
import main.vgw
import main.redUser
import main.yellowUser
import network.data.Info
import network.data.Onlines
import network.data.vgw.VGWData
import network.data.vgw.VGWMove
import network.data.vgw.VGWon
import java.io.Serializable

class VGWClient : GameClient() {
    var data: VGWData? = null

    override fun allOnline() = redUser != null && yellowUser != null

    override fun subListen(o: Serializable) {
        when (o) {
            is VGWData -> {
                data = o.copy()
            }
            is String -> {
                if (o.startsWith("CM")) {
                    game.move = o.substring(2)
                    game.upperComponent.neuladen()
                }
            }
            is Info -> {
                when (o.subject) {
                    "hover" -> vgw.setHover(Integer.parseInt(o.text))
                }
            }
            is VGWMove -> {
                vgw.addPiece(o.i)
            }
            is VGWon -> {
                vgw.finished = true
                vgw.setWonFields(o.list.clone())
            }
        }
    }

    override fun reloadOnlines(o: Onlines) {
        super.reloadOnlines(o)
        if (igi()) vgw.started = allOnline()
    }
}