package server.user

import adds.*
import hoverball.Simulator
import main.*
import network.data.*
import network.data.hoverball.*
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket
import kotlin.concurrent.thread

class HoverballGame(name: String) : OverGame(name, "hoverball") {
    var clients = ArrayList<HoverballUser>()

    var connectedCount = 0
    var connectedListener: () -> Unit = {}

    var creatingTeamName = ""

    //var clients = ArrayList<HoverballUser>()
    //var beforeClients = ArrayList<HoverballUser>()

    var startWerte = StartWerte()
        set(value) {
            field = value
            forall { it.send(field.copy()) }
        }

    var cyberTeams = ArrayList<CyberTeam>()

    lateinit var simulator: Simulator

    @Synchronized
    override fun install() {
        /*if (this::simulator.isInitialized) simulator.close()
        simulator = Simulator(hoverballPort, false)
        slog("Hoverball-Adresse: ${simulator.address}")*/

        stage = "waiting"
        startWerte = StartWerte()
        connectedCount = 0
        creatingTeamName = ""
        cyberTeams.clear()

        updateTeams()
    }

    override fun users(): ArrayList<GameUser> = clients as ArrayList<GameUser>
    override fun onlines(): ArrayList<OnlineI> = clients.copyAs { it.setOnline(); it.online }
    fun forall(f: (HoverballUser) -> Unit) { clients.forEach { f(it) } }


    override fun themaAngenommen(b: Boolean, v: VorschlagenItem) {
        super.themaAngenommen(b, v)
        if (b) {
            when (v.value) {
                is String -> when (v.thema) {
                    "laufzeit" -> startWerte = startWerte.copy(duration = v.value.toInt())
                    "shot" -> startWerte = startWerte.copy(shot = v.value.toInt())
                    "team" -> startWerte = startWerte.copy(team = v.value.toInt())
                    "spiel starten" -> startHoverball()
                    "pause game" -> {
                        randomClient().send("pause game")
                        clients.forEach { it.send(Info("Weiterspielen", "pause button text")) }
                    }
                    "go game" -> {
                        randomClient().send("go game")
                        clients.forEach { it.send(Info("Pause", "pause button text")) }
                    }
                    "quit game" -> {
                        stage = "waiting"
                        clients.forEach {
                            it.send("disvisible hoverball")
                            it.send("enable anfragen")
                            it.send(Info("Das Spiel wurde vorzeitig beendet.", "info"))
                        }
                        randomClient().send("quit game")
                    }
                }
                is CyberTeam -> when(v.thema) {
                    "invite cyber team" -> inviteTeam(v.value)
                    "cyber team entfernen" -> killTeam(v.value)
                }
            }
        }
    }

    var teams: ArrayList<HoverballTeam> = ArrayList()

    fun client(i: Int) = clients.find { it.id == i }!!

    fun inviteTeam(v: CyberTeam) {
        clients.find { v.owner == it.id }!!.apply {
            cyberTeams plus v.copy()
            send(CyberTeamAngenommen(v.copy()))
        }
        updateTeams()
    }
    fun killTeam(v: CyberTeam) {
        clients.forEach { it.cyberTeams entfernen { it == v } }
        clients.find { v.owner == it.id }!!.apply { send(SInfo(v.copy(), "remove cyber team")) }
        updateTeams()
    }

    fun randomClient() = clients[(Math.random()*clients.size.toDouble()).toInt()]

    fun startHoverball() {
        thread {
            slog("Spiel starting...")

            stage = "running"

            /*if (simulator.state() == 2) simulator.state(1)
            simulator.state(0)*/
            randomClient().send("start hoverball")
            forall {
                it.send("disenable hoverball anfragen")
                it.send("visible hoverball")
            }
        }
    }
    fun finishGame() {
        thread {
            Thread.sleep(3000)
            stage = "waiting"
            forall {
                it.send("disvisible hoverball")
                it.send("enable anfragen")
            }
        }
    }

    fun updateTeams() {
        forall {
            teams.find { e -> e is HumanTeam && e.name == it.teamName }?.let { u ->
                if (u is HumanTeam && u.users.find { e -> e.id == it.id } == null) u.users plus it.online.o
            } ?: run {
                teams plus HumanTeam(it.teamName, it.startColor!!, ArrayList()).apply { users plus it.online.o }
            }
            it.cyberTeams.filter { !teams.contains(it) }.forEach { teams plus it.copy() }
        }
        teams.filter { it is HumanTeam }.forEach { (it as HumanTeam).users entfernen { clients.find { u -> u.online.o == it } == null } }
        teams entfernen { it is HumanTeam && it.users.isEmpty() }
        teams entfernen { t -> t is CyberTeam && clients.find { it.cyberTeams.contains(t) } == null }

        forall { it.sendTeams() }
        myBefore().forEach { (it as HoverballUser).sendTeams() }
    }
}

class HoverballUser(
    socket: Socket,
    out: ObjectOutputStream,
    inp: ObjectInputStream
) : GameUser(socket, out, inp) {
    lateinit var game: HoverballGame
    lateinit var online: HoverballOnline
    lateinit var hoverballStart: HoverballStartOnline
    var teamName = "-"
    var startColor: TeamColors? = null
    var cyberTeams = ArrayList<CyberTeam>()

    fun setOnline() { online = HoverballOnline(c(this)!!, teamName) }

    override fun otherError(m: OnlineStart): String? =
        if (games[m.game]?.stage == "running") "Es existiert bereits ein Spiel."
        else null

    fun setGame() {
        if (games[start.game] == null) games[start.game] = HoverballGame(start.game)
        game = games[start.game]!! as HoverballGame
        game.clients.add(this)
    }
    
    override fun addUser() {
        beforeUsers kill this
    }

    override fun toOther(f: (GameUser?) -> Unit) { error("Es kann keinen anderen geben!") }
    override fun allOn(): Boolean { error("Es sind immer alle on!") }
    override fun myGame() = game

    override fun addUserFinish() { send(game.startWerte) }

    fun next(b: Int): Pair<HoverballUser, Int> {
        var o = 0
        game.clients.forEachIndexed { i, w -> if (w === this) o = i }
        for (i in 1..b) {
            if (o == game.clients.size-1) o = 0
            else o ++
        }
        return game.clients[o] to o
    }

    override fun sendStage() { if (igi()) send(Info(game.stage, "hoverballStage")) else sendGames() }

    override fun getProperties() {
        super.getProperties()
        sendStage()
        sendTeams()
    }

    override fun subListen(o: Serializable) {
        when (o) {
            is String -> {
                when (o) {
                    "connected!" -> {
                        game.connectedCount ++
                        if (game.connectedCount == game.clients.size) game.connectedListener()
                    }
                    "update-teams" -> game.updateTeams()
                    "game finished" -> game.finishGame()
                }
            }
            is HoverballStartOnline -> {
                if (validUser(o.s)) {
                    start = o.s.copy()
                    setGame()
                    val exist = game.teams.find { it.name == o.team } != null

                    hoverballStart = o.copy()
                    val c = TeamColors.values().filter { c -> game.teams.find { it.color == c } == null }
                    val full = game.teams.size >= TeamColors.values().size
                    fun full() { send(Info("Du kannst kein neues Team mehr hinzufügen.", "user")) }

                    if (exist) send(Info("Dieses Team existiert schon. Willst du ihm beitreten?", "really enter"))
                    else if (!full) send(ColorAnfrage("Dieses Team existiert noch nicht. Willst du es erstellen?", c, "start"))
                    else full()
                }
            }
            is Info -> {
                when (o.subject) {
                    "invite cyber team" -> {
                        game.apply {
                            val exist = teams.find { it.name == o.text } != null
                            fun full() { send(Info("Du kannst kein neues Team mehr hinzufügen.", "error-text")) }
                            val c = TeamColors.values().filter { c -> teams.find { it.color == c } == null }
                            val full = teams.size >= TeamColors.values().size

                            if (exist) send(Info("Das Team ${o.text} wurde bereits eingeladen.", "already invited"))
                            else if (!full) {
                                send(ColorAnfrage(null, c, "cyber"))
                                creatingTeamName = o.text
                            }
                            else full()
                        }
                    }
                }
            }
            is ColorAnfrageB -> {
                if (o.s == "start") {
                    println("in start")
                    setUser(hoverballStart.s)
                    teamName = hoverballStart.team
                    startColor = o.c
                    afterAdded()
                    send("dabei")
                    game.updateTeams()
                } else if (o.s == "cyber") {
                    game.vorschlagen(id, VorschlagenItem(
                        CyberTeam(game.creatingTeamName, o.c!!, 3, id),
                        "$name will das Team ${game.creatingTeamName} hinzufügen.",
                        "invite cyber team", online.o
                    ))
                }
            }
        }
    }

    override fun forall(f: (GameUser) -> Unit) { game.forall { f(it) } }
    override fun igi() = this::game.isInitialized

    @Synchronized
    override fun afterBye() {
        if (igi()) {
            game.apply {
                if (clients.contains(this@HoverballUser)) {
                    clients kill this@HoverballUser
                    if (clients.size == 0) {
                        stage = "waiting"
                        startWerte = StartWerte()
                    }
                    updateTeams()
                }
            }
        } else if (beforeUsers.contains(this)) beforeUsers kill this
        OverGame.reloadGames(type(), igi())
    }

    override fun sendGames() {
        super.sendGames()
        sendTeams()
    }

    fun sendTeams() {
        if (igi()) send(HoverballTeamsArray(game.teams.copy { it.clone() }))
        else {
            send(GameHoverballTeamsArray(
                myGames().toHashMap().copyAs { k, v -> k to HoverballTeamsArray((v as HoverballGame).teams.copyAs { it.clone() }) }
            ))
        }
    }
}

