package network.data.hoverball

import javafx.scene.paint.Color
import adds.copy
import network.data.Online
import network.data.OnlineI
import network.data.OnlineStart
import java.io.Serializable

data class HoverballOnline(val o: Online, val teamName: String) : OnlineI {
    override fun o() = o
    override fun clone() = copy()
}
data class StartWerte(val shot: Int = 1, val team: Int = 1, val duration: Int = 2) : Serializable

data class HoverballStartOnline(val s: OnlineStart, val team: String) : Serializable
data class HoverballStartOnlineBack(val s: HoverballStartOnline) : Serializable
data class HoverballTeamsArray(val h: ArrayList<HoverballTeam>) : Serializable {
    fun clone() = HoverballTeamsArray(h.copy { it.clone() })
}
data class GameHoverballTeamsArray(val h: HashMap<String, HoverballTeamsArray>) : Serializable

interface HoverballTeam {
    val name: String
    val color: TeamColors
    fun clone(): HoverballTeam
}
data class HumanTeam(
        override val name: String,
        override val color: TeamColors = TeamColors.RED,
        var users: ArrayList<Online>
) : Serializable, HoverballTeam {
    override fun clone() = HumanTeam(name, color, users.copy { it.copy() })
}
data class CyberTeam(
        override val name: String,
        override val color: TeamColors = TeamColors.RED,
        val size: Int,
        val owner: Int
) : Serializable, HoverballTeam {
    override fun clone() = copy()
}

data class CyberTeamAngenommen(val c: CyberTeam): Serializable

data class ColorAnfrage(val text: String?, val c: List<TeamColors>, val s: String) : Serializable
data class ColorAnfrageB(val c: TeamColors?, val s: String) : Serializable

enum class TeamColors { RED,GREEN,BLUE,ORANGE,PURPLE }

fun teamColor(c: TeamColors) = when (c) {
    TeamColors.RED -> Color.RED
    TeamColors.BLUE -> Color.BLUE
    TeamColors.GREEN -> Color.GREEN
    TeamColors.ORANGE -> Color.ORANGE
    TeamColors.PURPLE -> Color.PURPLE
}

