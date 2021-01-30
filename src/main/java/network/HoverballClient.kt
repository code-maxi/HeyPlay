package network

import adds.*
import gui.login
import gui.game.hoverball.chooseColor
import gui.gamePanel
import gui.hoverballAdress
import hoverball.Human
import hoverball.Team
import hoverball.team.*
import javafx.application.Platform
import javafx.stage.Stage
import main.*
import network.data.*
import network.data.hoverball.*
import java.io.Serializable
import kotlin.concurrent.thread

class HoverballClient : GameClient() {
    val onlines: ArrayList<HoverballOnline>
        get() = main.onlines.copyAs { it as HoverballOnline }

    lateinit var startWerte: StartWerte
    var team = ""
    var allTeams = HashMap<String, HoverballTeamsArray>()
    val teams: ArrayList<HoverballTeam>
        get() = allTeams[gameName]?.h ?: arrayListOf()
    var human: Human? = null
    var hoverballStage = ""
    var start = false

    var cyberTeams = ArrayList<Pair<CyberTeam, Team?>>()
    var dabei = false

    var beforeOnlineFrame: Stage? = null

    fun client(i: Int) = onlines.filter { it.o.id == i }[0]
    fun team() = teams.find { it is HumanTeam && it.users.find { u -> u.id == id } != null }

    fun disconnect() {
        human?.disconnect()
        if (iconi()) controller.disconnect()
    }
    fun connect() {
        var cc = false
        var hc = false
        var i = 0

        thread {
            while (!(cc && hc)) {
                disconnect()

                println("hoverballAdress: $hoverballAdress")
                val t = team()!!
                val c = teamColor(t.color).awt().rgb

                human = Human(t.name, username, c)
                controller.follow(human)
                controller.debug(human,true)

                hc = human!!.connect(hoverballAdress)
                cc = controller.connect(hoverballAdress)

                if (cc) send("connected!")
                if (i > 0) Thread.sleep(500)
                i ++
            }
        }
    }
    fun connectCyber(t: CyberTeam) {
        val c = teamColor(t.color).awt().rgb
        cyberTeams plus (t to when (t.name) {
            "Fantastic Three" -> FantasticThree(t.name, c)
            "Cosmic Friends" -> CosmicFriends(t.name, c)
            "Tribal Queens" -> TribalQueens(t.name, c)
            "Trio Gaulois" -> TrioGaulois(t.name, c)
            "Las Maquina Team" -> LasMaquinaTeam(t.name, c)
            "Barbarians" -> Barbarians(t.name, c)
            "Team Total" -> TeamTotal(t.name, c)
            "Duck Tales" -> DuckTales(t.name, c)

            else -> null
        }!!.apply { connect(hoverballAdress) })
    }

    override fun subListen(o: Serializable) {
        when (o) {
            is StartWerte -> {
                startWerte = o.copy()
                if (igi() && hoverball.ici()) { hoverball.startFenster.settings.reload() }
            }
            is ColorAnfrage -> {
                fun f() {
                    { c: TeamColors -> send(ColorAnfrageB(c, o.s)) }.let {
                        if (!LaunchUI.OPENSOFORT && !igi()) chooseColor(o.c, if (igi()) gamePanel.stack else login.root, o.s != "cyber") { c -> it(c) }
                        else it(TeamColors.GREEN)
                    }
                }
                (if (!LaunchUI.OPENSOFORT) o.text else null)?.let { really(o.text!!) { if (it) f() } } ?: f()
            }
            is Info -> {
                when (o.subject) {
                    "pause button text" -> hoverballTop.pauseButton.text = o.text
                    "really enter" -> if (!LaunchUI.OPENSOFORT) really(o.text) {
                        if (it) send(ColorAnfrageB(null, "start"))
                    } else send(ColorAnfrageB(null, "start"))
                    "hoverballStage" -> hoverballStage = o.text
                    "cyber team not exist" -> hbClient.vorschlagen(
                            o, "${client.username} will das Team ${o.text} einladen.",
                            "invite cyber team"
                    )
                    "error-text" -> info(o.text, "error")
                    "info-text" -> info(o.text)
                    "already invited" -> info(o.text) { enableAll(true) }
                    "hbinfo" -> hoverballTop.makeInfo(o.text)
                }
            }
            is GameHoverballTeamsArray -> {
                allTeams = o.h.copy { k, v -> k to v.clone() }
                reloadTeams()
            }
            is HoverballTeamsArray -> {
                allTeams[gameName] = o.clone()
                reloadTeams()
            }
            is String -> {
                when (o) {
                    "visible hoverball" -> visibleHoverball(true)
                    "disvisible hoverball" -> visibleHoverball(false)
                    "disconnect" -> disconnect()
                    "disenable hoverball anfragen" -> {
                        disenableAnfragen(true)
                        hoverballTop.enableButtons(true)
                    }
                    "dabei" -> dabei = true
                    "pause game" -> controller.state(1)
                    "go game" -> controller.state(2)
                    "quit game" -> {
                        if (controller.state() == 2) controller.state(1)
                        if (controller.state() != 0) controller.state(0)
                    }
                    "start hoverball" -> {
                        println("hoverball started")
                        controller.set("game.balls.shot", startWerte.shot.toDouble())
                        controller.set("game.balls.team", startWerte.team.toDouble())
                        controller.set("game.duration", startWerte.duration.toDouble()*60.0)

                        if (controller.state() == 2) controller.state(1)
                        if (controller.state() != 0) controller.state(0)
                        controller.state(1)
                        controller.state(2)

                        thread {
                            while(!controller.complete()) Thread.sleep(500)
                            send("game finished")
                        }
                    }
                }
            }
            is CyberTeamAngenommen -> {
                connectCyber(o.c.copy())
            }
            is SInfo -> {
                when (o.thema) {
                    "remove cyber team" -> {
                        cyberTeams.find { it.first == o.thing as CyberTeam }!!.apply {
                            second?.disconnect()
                            cyberTeams kill this
                        }
                    }
                }
            }
        }
    }
    fun reloadTeams() {
        if (!dabei) {
            login.beforeTeams.reload()
            login.seeTeamsButton.apply {
                if (teams.size > 0) Platform.runLater {
                    if (text.contains("Teams Ansehen"))
                        text = "Teams Ansehen (${teams.size})  →"
                } else {
                    Platform.runLater {
                        text = "Teams Ansehen  →"
                        login.beforeTeams.root.isVisible = false
                        login.primaryStage.pack()
                    }
                }
            }
            if (teams.isEmpty()) login.beforeTeams.root.isVisible = false
        } else {
            if (!start) {
                connect()
                start = true
            }
            if (igi() && hoverball.ici()) hoverball.onlineTeams.reload()
        }
    }

    override fun sendUserStart() {
        send(HoverballStartOnline(OnlineStart(online(), version, gameName, whichServer()), team))
        println("User started!")
    }

    override fun vorschlagen(value: Serializable, text: String, thema: String, under: String?) {
        send(VorschlagenItem(value, text, thema, online(), under))
        //else Information("Du kannst nichts vorschlagen, weil außer dir noch niemand online ist.")
    }

    override fun disenableAnfragen(bb: Boolean) {
        if (igi()) {
            hoverball.apply {
                if (!hoverballVisible) {
                    if (ici()) {
                        startFenster.settings.a.forEach { (_, b) -> b.b.isDisable = bb }
                        startFenster.startButton.isDisable = bb
                        startFenster.teamButton.isDisable = bb
                    }
                    onlineTeams.items.forEach { it.entfernen?.isDisable = bb }
                } else hoverballTop.enableButtons(!bb)
            }
        }
    }
    override fun vorschlagAngenommen(v: VorschlagenAngenommen) {
        if ((v.thema != "spiel starten" || !v.b) && (v.thema != "quit game" || !v.b)) {
            if (!hoverballVisible || !igi()) super.vorschlagAngenommen(v)
            else hoverballTop.makeInfo(v.text)
        }
    }

    override fun vorschlagenReally(o: VorschlagenItem, f: (Boolean) -> Unit) {
        if (!hoverballVisible || !igi()) super.vorschlagenReally(o, f)
        else hoverballTop.makeReally(o.text, f)
    }
}