package server.user

import adds.SERVER_DEBUG
import adds.copy
import adds.copyAs
import adds.kill
import network.data.*
import network.data.wizard.*
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.math.abs

class WizardGame(name: String) : OverGame(name, "wizard") {
    var clients = ArrayList<WizardUser>()
    override fun users() = clients as ArrayList<GameUser>

    var rundenUmgehen = 0
        set(value) {
            field = value
            //println("rundenUmgehen: $field")
        }
    var smallRunden = 0
        set(value) {
            field = value
            //println("smallRunden: $field")
        }
    var OWN_CARDS: ArrayList<WizardCard> = WIZARD_CARDS
    var tableCards = ArrayList<WizardCard>()
    var hellsehenArray = ArrayList<WizardCard>()
    var actualCards = ArrayList<WizardCard>()
    var runde = 0
        set(value) {
            field = value
            forall { it.sendRunde() }
            //println("runde: $field")
        }
    var runden: Int = 20
        set(value) {
            field = value
            println("runden setted: $value")
            forall { it.sendRunden() }
        }
    var move = WizardMove()
        set(value) {
            field = value
            forall { it.sendMove() }
        }

    fun sendAllMoveUmgehen(i: Int) { forall { u -> u.send(Info(i.toString(), "move umgehen")) } }

    @Synchronized
    override fun install() {
        rundenUmgehen = 0
        smallRunden = 0
        stage = "waiting"
        OWN_CARDS = WIZARD_CARDS
        tableCards.clear()
        hellsehenArray.clear()
        move = WizardMove()
    }

    override fun themaAngenommen(b: Boolean, v: VorschlagenItem) {
        super.themaAngenommen(b, v)
        if (b) {
            when (v.value) {
                is String -> when(v.thema) { "wizard-starten" -> start() }
                is Int -> {
                    when (v.thema) {
                        "wizard-runden" -> {
                            if (v.value-1 > runde || stage == "waiting") runden = v.value
                        }
                    }
                }
            }
        }
    }

    override fun onlines(): ArrayList<OnlineI> =
        clients.copyAs { it.setOnline(); it.myOnline }

    fun start() {
        sendOnlines()
        forall { it.send("start game") }
        stage = "active"
        newRunde(true)
    }

    fun stich(): WizardCard {
        for (c in tableCards) { if (c.type == "z") return c }

        if (tableCards[0].type == "z") return tableCards[0]
        var firstColor = if (tableCards[0].type != "n") tableCards[0].color else "-"
        if (firstColor == "-") {
            loop@ for (i in tableCards) {
                if (i.type != "n") {
                    firstColor = i.color
                    break@loop
                }
            }
            if (firstColor == "-") return tableCards[0]
        }
        var color: WizardCard? = null
        var tr: WizardCard? = null
        for (c in tableCards) {
            if (c.color == firstColor) c.type()?.let {
                if (color == null) color = c
                else if (it > color!!.type()!!) color = c
            }
            if (c.color == move.trumpf?.color && move.trumpf!!.type != "z") c.type()?.let {
                if (tr == null) tr = c
                else if (it > tr!!.type()!!) tr = c
            }
        }
        return if (tr != null && tr!!.type()!! > 0) tr!!
        else color!!
    }

    fun stichWerten() {
        thread {
            rundenUmgehen = 0
            move = move.copy(move = -1)

            val stich = stich().owner
            forall { it.send(Info(stich, "stich an")) }
            client(stich.toInt()).myOnline.astiche ++
            forall { it.send(client(stich.toInt()).myOnline.copy()) }

            Thread.sleep(3000)

            tableCards.clear()
            //forall { it.sendTableCards() }
            forall { it.send("remove table cards") }

            if (smallRunden >= runde) { newRunde() }
            else {
                move = move.copy(move = stich.toInt())
                smallRunden ++
            }
        }
    }

    private fun setMove() {
        val d = clients[(clients.size*Math.random()).toInt()]
        val a = if (actualCards.isNotEmpty()) actualCards[(actualCards.size*Math.random()).toInt()] //actualCards.filter { it.type == "n" }[0]
        else null
        move = WizardMove(d.id, d.id, a)
        client(move.dealer).let {
            it.send("ready for stiche voraussagen first")
            sendAllMoveUmgehen(it.id)
        }
    }

    private fun giveCards() {
        actualCards = WIZARD_CARDS.copy()
        clients.forEach { u ->
            val a = ArrayList<WizardCard>()
            for (i in 0..runde) {
                actualCards[(Math.random()*actualCards.size).toInt()].let {
                    it.owner = u.id.toString()
                    a.add(it)
                    actualCards.remove(it)
                }
            }
            if (runde == 0) hellsehenArray.add(a[0])
            u.send(Cards(a.copy { it.copy() }).apply { u.cards = this })
        }
        if (runde == 0) forall { it.send(HellsehenData(hellsehenArray.copy { it.copy() })) }
    }

    fun sendAll(o: Serializable) { forall { it.send(o) } }

    private fun newRunde(start: Boolean = false) {
        if (!start) { if (runde < runden) runde += 1 }
        else { runde = 0 }

        smallRunden = 0
        rundenUmgehen = 0

        if (!start) {
            forall { it.werten() }
            sendOnlines()
        }
        if (runde < runden) {
            sendAll("new runde")
            if (false) { // DEBUG!
                thread {
                    for (i in 0..9) {
                        Thread.sleep(5000)
                        giveCards()
                    }
                    setMove()
                }
            } else {
                giveCards()
                setMove()
            }
        } else {
            sendAll("game end")
            stage = "finish"
        }
    }

    fun testMitmachen(a: Array<Pair<Boolean, Int>>): Boolean? {
        if (a.size == clients.size) {
            a.forEach { if (!it.first) return false }
            return true
        }
        return null
    }

    fun client(i: Int) = clients.filter { it.id == i }[0]

    fun forall(f: (WizardUser) -> Unit) { clients.forEach { f(it) } }
}

class WizardUser(socket: Socket, out: ObjectOutputStream, inp: ObjectInputStream) : GameUser(socket, out, inp) {
    lateinit var game: WizardGame
    lateinit var myOnline: WizardOnline

    var cards = Cards(arrayListOf())

    fun setOnline() {
        val e = online()
        val o = if (this::myOnline.isInitialized) myOnline.copy(o = e) else WizardOnline(e)
        myOnline = o
    }

    override fun myGame() = game
    override fun igi() = this::game.isInitialized

    override fun getStage() = "not needed"
    override fun setStage(s: String) { }
    override fun setDialog(i: Info?) {  }
    override fun getDialog(): Info? { error("dialog not needed") }

    override fun otherError(m: OnlineStart): String? =
        if (games[m.game]?.stage == "active") "Es exeitiert bereits eine Partie."
        else if ((games[m.game]?.users()?.size ?: -1)+1 > 6) "Die Wizard Runde ist schon besetzt!"
        else null

    @Synchronized
    override fun addUser() {
        if (games[start.game] == null) games[start.game] = WizardGame(start.game)
        game = games[start.game]!! as WizardGame
        game.clients.add(this)
        game.sendOnlines()
        beforeUsers kill this
    }

    override fun toOther(f: (GameUser?) -> Unit) { error("Es kann keinen anderen geben!") }
    override fun allOn(): Boolean { error("Es sind immer alle on!") }

    override fun addUserFinish() {
        if (!SERVER_DEBUG) {
            val t = (60.0/game.clients.size.toDouble()).toInt()
            if (game.runden > t) game.runden = t
            else sendRunden()
        } else  {
            sendRunden()
            println(game.clients.size)
            if (game.clients.size == 2) thread { Thread.sleep(2000); game.start() }
        }
        afterAdded()
    }

    fun next(b: Int): Pair<WizardUser, Int> {
        var o = 0
        game.clients.forEachIndexed { i, w -> if (w === this) o = i }
        for (i in 1..b) {
            if (o == game.clients.size-1) o = 0
            else o ++
        }
        return game.clients[o] to o
    }

    override fun subListen(o: Serializable) {
        when (o) {
            is Info -> {
                when (o.subject) {
                    "stiche vorraussagen" -> {
                        myOnline.vstiche = o.text.toInt()
                        forall { it.send(myOnline.copy()) }
                        next(1).let {
                            val last = game.rundenUmgehen == game.clients.size-1
                            if (game.rundenUmgehen < game.clients.size-1 && !last) {
                                game.sendAllMoveUmgehen(it.first.id)
                                it.first.send("ready for stiche voraussagen${ if(game.rundenUmgehen == game.clients.size-2) " last" else "" }")
                            }
                            if (!last) game.rundenUmgehen ++
                            else {
                                game.rundenUmgehen = 0
                                forall { it.send("new runde beginns") }
                            }
                        }
                    }
                    "set trumpf" -> {
                        game.move = game.move.copy(trumpf = WizardCard.parse(o.text))
                    }
                }
            }
            is WizardCard -> {
                game.tableCards.add(o.copy())
                forall { it.send(o.copy()) }
                next(1).let {
                    val last = game.rundenUmgehen >= game.clients.size-1
                    if (last) {
                        game.stichWerten()
                        game.rundenUmgehen = 0
                    } else {
                        game.move = game.move.copy(move = it.first.id)
                        game.rundenUmgehen ++
                    }
                }
            }
        }
    }

    override fun forall(f: (GameUser) -> Unit) { if (igi()) game.forall { f(it) } }

    @Synchronized
    override fun afterBye() {
        if (igi()) game.clients kill this
        else beforeUsers kill this

        if (igi() && game.clients.size == 0) game.stage = "waiting"
        OverGame.reloadGames(type(), igi())
    }

    fun sendMove() { send(game.move) }
    fun sendRunden() { send(Info(game.runden.toString(), "runden")) }
    fun sendRunde() { send(Info(game.runde.toString(), "runde")) }
    fun sendTableCards() {
        send(TableCards(game.tableCards.copy { it.copy() }))
    }
    fun sendOnline() { send(myOnline.copy()) }
    fun werten() {
        myOnline.apply {
            var add: Int
            if (vstiche == astiche) add = vstiche + 2
            else add = -abs(vstiche-astiche)
            points += add
            lastPlus = add
            vstiche = 0
            astiche = 0
        }
    }
}
