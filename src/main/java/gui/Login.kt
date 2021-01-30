package gui

import adds.*
import game.chess.ChessView
import main.gamesOnlines
import main.onlines
import gui.dialog.SourcesDialog
import gui.game.hoverball.HoverballView
import gui.game.hoverball.OnlineTeams
import gui.game.mtm.MTMView
import gui.game.vgw.VGWView
import gui.game.wizard.WizardView
import hoverball.Controller
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import main.*
import main.Start.Companion.homepage
import network.*
import network.data.Info
import network.GameClient
import tornadofx.*
import java.io.Serializable
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

var hoverballAdress = ""

val nameValidator: (String?) -> String? = {
    if (it != null) {
        if (icin() && onlines.find { o -> o.o().name == it } != null) "Dieser Name existiert bereits."
        else if (it.length > 13) "Der Angegebene Text ist zu lang."
        else if (it.length < 3) "Der angegebene Text ist zu kurz."
        else null
    } else null
}

val hoverballTeams = mapOf(
        "Fantastic Three" to (arrayOf("Armin Hornung") to 4),
        "Cosmic Friends" to (arrayOf("Stefan Bornhofen") to 4),
        "Tribal Queens" to (arrayOf("Xavier Detant", "Fräntz Miccoli", "Vincent van Houtte") to 5),
        "Trio Gaulois" to (arrayOf("Cédric Leboucher", "Marc Bouleau") to 3),
        "Las Maquina Team" to (arrayOf("Thibaud Compte", "Florian Colin", "Cyrille Coin") to 3),
        "Barbarians" to (arrayOf("Timm Schneevoigt", "Alexander Dippel") to 3),
        "Team Total" to (arrayOf("Martin Gloderer") to 2),
        "Duck Tales" to (arrayOf("Sven Wunderlich", "Louis Lauser") to 1)
)

lateinit var login: Login
fun ili() = ::login.isInitialized

class Login : View("HeyPlay! Login") {
    val selectedGame = SimpleStringProperty()
    var model = TextFieldModel()
    lateinit var mcomputer: Parent
    lateinit var mhuman: Parent
    lateinit var settingsPane: VBox
    lateinit var actualSettings: Parent
    lateinit var startButton: Button
    var seeTeamsButton: Button
    lateinit var teamLabel: TextField
    lateinit var idField: TextField
    lateinit var disenableBox: Pane

    var actualType = ""
    var actualRole = ""

    var serverDefaults = true

    var startOnlines = StartGames()
    lateinit var startOnlinesPane: Pane

    var hbPortValue = 0
    var portValue = 0
    var serverValue = ""

    var serverDefaultControls = arrayListOf<Node>()

    lateinit var getClient: () -> GameClient

    lateinit var connectedLabel: Label
    lateinit var reconnectButton: Button
    var actualConnectedLabel = Label()

    var mySettings: Map<String, Parent?>

    fun disconnectedLabel(s: String) = Label(s).apply {
        addClass("connect-label")
        style { backgroundColor += Color.rgb(110, 0, 0) }
    }

    var onTypeChangeListeners = arrayListOf<() -> Unit>()

    var connected: Boolean = false
        set(value) {
            field = value
            Platform.runLater {
                actualConnectedLabel.replaceWith((if (field) connectedLabel.apply {
                    text = "HeyPlay! $serverValue:$portValue"
                } else disconnectedLabel("HeyPlay! Nicht Verbunden")).apply { actualConnectedLabel = this })
            }
        }

    var finished = false
    lateinit var beforeTeams: OnlineTeams

    override val root: StackPane

    inner class StartGames : View() {
        lateinit var onlinesFlow: FlowPane
        lateinit var gamesFlow: FlowPane
        lateinit var onlinesBox: HBox
        lateinit var gamesBox: HBox
        override val root = vbox {
            spacing = 5.0
            bindVisible { primaryStage }
            isVisible = false
            gamesFlow = flowpane {
                managedProperty().bind(visibleProperty())
                isVisible = false
                hgap = 10.0
                vgap = 5.0
                alignment = Pos.CENTER
                maxWidth = 300.0
                label("Aktuelle Spiele:") {
                    addClass("small-heading-label")
                    vboxConstraints { marginTop = 10.0 }
                }
                gamesBox = hbox {
                    spacing = 5.0
                    alignment = Pos.CENTER
                    vboxConstraints { marginBottom = 5.0 }
                }
            }
            onlinesFlow = flowpane {
                managedProperty().bind(visibleProperty())
                isVisible = false
                hgap = 10.0
                vgap = 5.0
                alignment = Pos.CENTER
                maxWidth = 300.0
                label("Aktuelle Spieler:") {
                    addClass("small-heading-label")
                    vboxConstraints { marginTop = 10.0 }
                }
                onlinesBox = hbox {
                    spacing = 5.0
                    alignment = Pos.CENTER
                    vboxConstraints { marginBottom = 5.0 }
                }
            }
        }
        fun reload() {
            Platform.runLater {
                onlinesBox.children.clear()
                gamesBox.children.clear()

                onlinesFlow.isVisible = client.anyoneOnline()
                gamesFlow.isVisible = gamesOnlines.size > 0
                root.isVisible = onlinesFlow.isVisible || gamesFlow.isVisible

                synchronized (this) {
                    if (gamesFlow.isVisible) {
                        gamesOnlines.forEach { n, g ->
                            gamesBox.button(g.name) {
                                addClass("game-button")
                                if (text == client.gameName) {
                                    addClass("selected")
                                    if (client is HoverballClient && g.stage == "running") {
                                        gamesBox.button("\uD83D\uDC41") {
                                            addClass("eye-label")
                                            ownTip("Dem Spiel zuschauen")
                                            action {
                                                if (iconi()) {
                                                    controller.disconnect()
                                                    controller.connect(hoverballAdress)
                                                    visibleHoverball(true)
                                                }
                                            }
                                        }
                                    } else if (hoverballVisible) visibleHoverball(false)
                                }
                                else removeClass("selected")
                                action { idField.text = text }
                            }
                        }
                    }
                    if (onlinesFlow.isVisible) {
                        onlines.forEach { u ->
                            onlinesBox.label(u.o().name) {
                                addClass("start-user-label")
                                style {
                                    backgroundColor += when (u.o().role) {
                                        "red" -> Color.rgb(70,0,0)
                                        "yellow" -> Color.rgb(80,80,0)
                                        "black" -> Color.rgb(55,55,55)
                                        "white" -> Color.rgb(90,90,90)
                                        else -> Color.rgb(70,70,70)
                                    }
                                }
                            }
                        }
                    }
                    if (client is HoverballClient) {
                        beforeTeams.reload { login.primaryStage.sizeToScene() }
                        hbClient.reloadTeams()
                    }
                }

                primaryStage.pack(false)
            }
            onlinesPane[client.type()]?.reload()
        }
    }

    inner class TextFieldModel : ViewModel() {
        val server = bind { SimpleStringProperty() }
        val port = bind { SimpleStringProperty() }
        val name = bind { SimpleStringProperty() }
        val hbPort = bind { SimpleStringProperty() }
        val teamName = bind { SimpleStringProperty() }
        val id = bind { SimpleStringProperty() }
    }

    init {
        onlinesPane["vgw"] = OnlinePanes(mapOf(
            ("red") to ("TEAM ROT" to Color.rgb(255,0,0, 0.3)),
            ("yellow") to ("TEAM GELB" to Color.rgb(220,220,0, 0.4)),
            ("viewer") to ("ZUSCHAUER" to Color.rgb(255,255,255, 0.2))
        ))
        onlinesPane["chess"] = OnlinePanes(mapOf(
            ("red") to ("TEAM ROT" to Color.rgb(255,0,0, 0.3)),
            ("yellow") to ("TEAM GELB" to Color.rgb(220,220,0, 0.4)),
            ("viewer") to ("ZUSCHAUER" to Color.rgb(255,255,255, 0.2))
        ))
        fun roleSettingsPane(ty: String, fs: String, m: Map<String, String>) = VBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            hbox {
                alignment = Pos.CENTER
                maximal(false)
                hbox {
                    spacing = 10.0
                    maximal(false)
                    ToggleGroup().let {
                        m.forEach { t, s ->
                            radiobutton(t, it) {
                                fun f() { actualRole = s }
                                action { f() }
                                if (text == fs) onTypeChangeListeners.add({
                                    if (client.type() == ty) {
                                        isSelected = true
                                        f()
                                    }
                                })
                            }
                        }
                    }
                    addClass("game-pane")
                }
            }
            onlinesPane[ty]?.allPanes(Orientation.HORIZONTAL) { !igi() }?.let { this += it }
        }
        mySettings = mapOf<String, Parent?>(
            "chess" to roleSettingsPane("chess", "Weiß", mapOf(
                "Weiß" to "white",
                "Schwarz" to "black",
                "Zuschauer" to "viewer"
            )),
            "vgw" to roleSettingsPane(
                "vgw", if (LaunchUI.OPENSOFORT) (when (LaunchUI.params[2]) {
                    "Maxi" -> "Gelb"
                    "Felix" -> "Rot"
                    else -> null
                }!!) else "Gelb", mapOf(
                "Gelb" to "yellow",
                "Rot" to "red",
                "Zuschauer" to "viewer"
            )),
            "mtm" to null,
            "wizard" to null,
            "hoverball" to VBox().apply {
                alignment = Pos.CENTER
                useMaxWidth = false
                spacing = 10.0
                this += settings(mapOf(
                    ("Team-Name" to false) to { e ->
                        teamLabel = textfield(model.teamName) {
                            text = "Beispiel"
                            addClass("my-textfield")
                            maxWidth = 120.0
                            enter { go() }
                            validator { n ->
                                var m: ValidationMessage? = null
                                if (n != null) {
                                    if (n.length <= 2) m = error("Der angegebene Text ist zu kurz.")
                                    else if (!n.matches(Regex("[0-9a-zA-ZäöüßÄÖÜß\\.! ]+")))
                                        m = error("Das Textfeld ist nicht valid.")
                                }
                                m
                            }
                            e()
                        }
                    }
                ))
                seeTeamsButton = button ("Teams Ansehen  →") {
                    addClass("invite-team-button")
                    minWidth = 200.0
                    action {
                        client {
                            fun v(b: Boolean) {
                                if (b) text = "Teams Ausblenden  ←"
                                else text = "Teams Ansehen  →"
                                beforeTeams.root.isVisible = b
                            }

                            if (beforeTeams.root.isVisible) v(false)
                            else if (hbClient.teams.isNotEmpty()) v(true)
                            else info("Es existieren noch keine Teams.")

                            Platform.runLater { primaryStage.pack() }
                        }
                    }
                }
            }
        )
        selectedGame.onChange {
            /*fun setTo(g: GamePane, c: () -> GameClient): Parent {
                if (icin()) client.bye()
                getClient = c
                client = c()
                g.install()
                startOnlines.reload()
                reconnect()
                gamePaneContainer = g.root
                gamePane = g
                Platform.runLater {
                    model.validate(focusFirstError = true, decorateErrors = true)
                    primaryStage.pack(true)
                }
                setDefaultSettings()
                return gamePaneContainer
            }*/
            settingsPane.isVisible = false
            reloadGameType {
                when (it) {
                    "Schach" -> { ChessClient() }
                    "Vier Gewinnt" -> { VGWClient() }
                    "Montagsmaler" -> { MTMClient() }
                    "Wizard" -> { WizardClient() }
                    "Hoverball" -> { HoverballClient() }
                    else -> kotlin.error("Eigen 1")
                }
            }
        }

        root = stackpane {
            borderpane {
                useMaxSize = true
                addClass("anmelden")

                right {
                    this += OnlineTeams(1).apply {
                        borderpaneConstraints { marginLeft = 10.0 }
                        root.apply {
                            managedProperty().bind(visibleProperty())
                            isVisible = false
                            minWidth = 350.0
                            maxWidth = 350.0
                        }
                        clickListener = { teamLabel.text = it.name }
                        beforeTeams = this
                    }
                }

                center {
                    vbox {
                        useMaxSize = false
                        alignment = Pos.CENTER

                        vbox {
                            alignment = Pos.CENTER
                            label("Willkommen zur HeyPlay!") {
                                vboxConstraints {
                                    marginBottom = 15.0
                                }
                                addClass("welcome-label")
                                vboxConstraints { marginBottom = 5.0 }
                            }
                            hbox {
                                alignment = Pos.CENTER
                                useMaxSize = true
                                spacing = 7.0
                                button("Homepage") {
                                    addClass("source-button")
                                    action { hostServices.showDocument(homepage) }
                                }
                                button("Quellen") {
                                    addClass("source-button")
                                    action { SourcesDialog() }
                                }
                            }
                        }

                        vbox {
                            alignment = Pos.CENTER
                            maximal()
                            this += images.values.first().apply { currentImage = this }

                            combobox(property = selectedGame, values = listOf("Schach", "Vier Gewinnt", "Montagsmaler", "Wizard", "Hoverball")) {
                                vboxConstraints {
                                    marginTop = 10.0
                                    marginBottom = 15.0
                                }
                                addClass("my-chombobox")
                            }

                            scrollpane {
                                alignment = Pos.CENTER
                                isFitToWidth = true
                                isFitToHeight = false
                                addClass("dark-scroll-pane")
                                vbox {
                                    useMaxSize = true
                                    alignment = Pos.CENTER

                                    vbox {
                                        alignment = Pos.CENTER
                                        spacing = 5.0
                                        vboxConstraints { marginBottom = 15.0 }
                                        label ("Server-Einstellungen:") {
                                            addClass("small-heading-label")
                                            style { fontSize = 12.px }
                                        }
                                        hbox {
                                            alignment = Pos.CENTER
                                            spacing = 10.0
                                            ToggleGroup().let {
                                                fun r(s: String, b: Boolean) {
                                                    radiobutton (s, it) {
                                                        isSelected = serverDefaults == b
                                                        action {
                                                            serverDefaults = b
                                                            setDefaultSettings()
                                                        }
                                                        style { textFill = Color.rgb(251,255,138) }
                                                    }
                                                }
                                                r("Defaults", true)
                                                r("Benutzerdefiniert", false)
                                            }
                                        }
                                    }

                                    gridpane {
                                        vgap = 3.0
                                        hgap = 10.0

                                        useMaxSize = false
                                        alignment = Pos.CENTER

                                        val l: TextField.(Int) -> Unit = {
                                            useMaxWidth = true
                                            addClass("my-textfield")
                                            style {
                                                fontSize = 15.px
                                                when (it) {
                                                    1 -> textFill = Color.YELLOW
                                                    2 -> {
                                                        textFill = Color.LIGHTGREEN
                                                        fontSize = 18.px
                                                    }
                                                    3 -> textFill = Color.LIGHTBLUE
                                                    4 -> textFill = Color.LIGHTYELLOW
                                                }
                                            }
                                        }

                                        label("Server") {
                                            addClass("small-heading-label")
                                            style { fontSize = 12.px }
                                            serverDefaultControls plus this
                                            gridpaneConstraints { columnRowIndex(0, 0) }
                                        }
                                        label("Port") {
                                            addClass("small-heading-label")
                                            style { fontSize = 12.px }
                                            serverDefaultControls plus this
                                            gridpaneConstraints { columnRowIndex(0, 1) }
                                        }
                                        label("Name") {
                                            addClass("small-heading-label")
                                            style { fontSize = 12.px }
                                            gridpaneConstraints { columnRowIndex(0, 2) }
                                        }
                                        label("Spiel-Id") {
                                            addClass("small-heading-label")
                                            style { fontSize = 12.px }
                                            gridpaneConstraints { columnRowIndex(0, 3) }
                                        }

                                        textfield(model.server) {
                                            serverDefaultControls plus this
                                            text = if (parameter.raw.isEmpty()) "server.maxi.li" else "ZenBook"
                                            gridpaneConstraints { columnRowIndex(1, 0) }
                                            enter { reconnect() }
                                            l(0)
                                        }
                                        textfield(model.port) {
                                            serverDefaultControls plus this
                                            text = standartPort.toString()
                                            gridpaneConstraints { columnRowIndex(1, 1) }
                                            enter { reconnect() }
                                            validator {
                                                var m: ValidationMessage? = null
                                                if (it != null) {
                                                    if (!it.matches(Regex("[0-9]+"))) m = error("Der Port darf nur Ziffern enthalten.")
                                                }
                                                m
                                            }
                                            l(1)
                                        }
                                        textfield(model.name) {
                                            text = if (parameter.raw.isEmpty()) "" else "Maxi"
                                            gridpaneConstraints { columnRowIndex(1, 2) }
                                            enter { go() }
                                            validator { n ->
                                                var m: ValidationMessage? = null
                                                nameValidator(n).let {
                                                    if (it != null) m = error(it)
                                                }
                                                m
                                            }
                                            l(2)
                                        }
                                        idField = textfield(model.id) {
                                            text = ""
                                            gridpaneConstraints { columnRowIndex(1, 3) }
                                            enter { go() }
                                            validator { n ->
                                                var m: ValidationMessage? = null
                                                if (n != null) {
                                                    if (n.length < 3) m = error("Die ID muss mindestens 3 Zeichen enthalten.")
                                                    if (n.length > 10) m = error("Die ID darf höchstens 10 Zeichen enthalten.")
                                                    else if (!n.matches(Regex("[a-z0-9]+"))) m = error("Die ID darf nur Kleinbuchstaben und Ziffern enthalten.")
                                                }
                                                m
                                            }
                                            l(4)
                                            textProperty().onChange {
                                                if (it != null) {
                                                    client.gameName = it
                                                    setHBAdress()
                                                    startOnlines.reload()
                                                }
                                            }
                                        }
                                        vboxConstraints { marginBottom = 5.0 }
                                    }

                                    settingsPane = vbox {
                                        alignment = Pos.CENTER
                                        spacing = 10.0
                                        vboxConstraints { marginTop = 10.0; marginBottom = 10.0 }
                                        bindVisible { primaryStage }

                                        this += HBox().apply { actualSettings = this }
                                    }

                                    startOnlinesPane = hbox {
                                        alignment = Pos.CENTER
                                        bindVisible { primaryStage }
                                        maximal()
                                        this += startOnlines
                                        vboxConstraints { marginTop = 5.0 }
                                    }

                                    hbox {
                                        alignment = Pos.CENTER
                                        spacing = 7.0
                                        vboxConstraints { marginTop = 12.0; }

                                        this += ownButton(
                                            t = "⭯", s = 15.0,
                                            tt = "Verbindung neu aufbauen", c = "go-button"
                                        ) { /*reconnect()*/ onlinesPane[client.type()]?.reload() }.apply { reconnectButton = this as Button }

                                        fun connectedLabel(f: Label.() -> Unit = {}) = Label().apply {
                                            addClass("connect-label")
                                            style { backgroundColor += Color.rgb(0,110,0) }
                                            f()
                                        }

                                        vbox {
                                            alignment = Pos.CENTER
                                            spacing = 3.0

                                            connectedLabel = connectedLabel { actualConnectedLabel = this }

                                            this += actualConnectedLabel
                                        }
                                    }

                                    startButton = button("Los geht's!") {
                                        addClass("go-button")
                                        vboxConstraints { marginTop = 25.0 }
                                        maxWidth = 300.0

                                        action { go() }
                                    }
                                }
                            }
                        }
                    }
                    setMinSize(prefWidth, prefHeight)
                }
                forAllNodes(this) {
                    addClass("white-text-label")
                }
            }
            this += disenableBox {
                shortcut("Ctrl+B") { isVisible = false }
                disenableBox = this
            }
        }
        Platform.runLater {
            selectedGame.value = if (LaunchUI.OPENSOFORT) when (LaunchUI.params[1]) {
                "vgw" -> "Vier Gewinnt"
                "mtm" -> "Montagsmaler"
                "chess" -> "Schach"
                "wizard" -> "Wizard"
                "hoverball" -> "Hoverball"
                else -> ""
            } else "Montagsmaler"

            try { model.validate(focusFirstError = true, decorateErrors = true) }
            catch (e: java.lang.Exception) {}
        }

        serverDefaultControls.forEach { it.bindVisible() }

        primaryStage.isResizable = true
        primaryStage.setOnCloseRequest {
            if (icin() && client is HoverballClient) (client as HoverballClient).disconnect()
            if (finished) client.bye()
            exitProcess(0)
        }
        login = this

        thread {
            Thread.sleep(1000)
            Platform.runLater {
                primaryStage.pack()
            }
        }
    }

    fun client(o: Serializable? = null, s: GameClient.() -> Unit = { send(o!!) }) {
        if (connected) client.s()
        else info("Es besteht keine Verbindung.", type = "error")
    }
    fun start() {
        client.started = true
        Platform.runLater {
            login.replaceWith(
                GamePanel(selectedGame.value, when (client) {
                    is ChessClient -> ChessView()
                    is VGWClient -> VGWView()
                    is MTMClient -> MTMView()
                    is WizardClient -> WizardView()
                    is HoverballClient -> HoverballView()
                    else -> null
                }!!),
                ViewTransition.Slide(0.4.seconds, ViewTransition.Direction.LEFT), sizeToScene = true, centerOnScreen = true
            )
            thread {
                Thread.sleep(1000)
                Platform.runLater {
                    if (!LaunchUI.OPENSOFORT) really(
                        "Willst du die Viedeokonferenz öffnen?",
                        "Über die Videokonferenz könnt ihr zusammen sprechen und euch sehen.",
                        noText = "Später"
                    ) {
                        if (it) hostServices.showDocument(client.meetingLink)
                    } else {
                        when (LaunchUI.params[2]) {
                            "Maxi" -> {
                                primaryStage.apply {
                                    x = 0.0
                                    y = 0.0
                                }
                            }
                            "Felix" -> {
                                primaryStage.apply {
                                    x = screenW - width
                                    y = screenH - height
                                }
                            }
                            "Franzi" -> {
                                primaryStage.apply {
                                    x = 0.0
                                    y = screenH - height
                                }
                            }
                            "Steffi" -> {
                                primaryStage.apply {
                                    x = screenW - width
                                    y = 0.0
                                }
                            }
                        }
                        primaryStage.isAlwaysOnTop = true
                    }
                }
            }
        }
    }
    fun reloadGameType(c: () -> GameClient) {
        getClient = c
        if (icin()) client.bye()
        client = getClient()
        actualType = client.type()
        if (actualType != "chess" && actualType != "vgw") actualRole = "viewer"
        changeImage(actualType)

        Platform.runLater {
            idField.text = if (LaunchUI.OPENSOFORT) LaunchUI.params[3] else ""
            if (LaunchUI.OPENSOFORT) model.name.value = LaunchUI.params[2]

            reconnect()
        }

        changeSettings(actualType)

        when (actualType) {
             "hoverball" -> {
                controller = Controller(false)
                installHoverball()
            }
            //"vgw", "chess" -> Platform.runLaHoter { (settingsPane.children[0] as RadioButton).isSelected = true }
        }
        onTypeChangeListeners.forEach { it() }
        startOnlinesPane.isVisible = true
        startOnlines.reload()
        setDefaultSettings()

        primaryStage.pack(true)
        thread {
            Thread.sleep(1000)
            Platform.runLater { model.validate(focusFirstError = true, decorateErrors = true) }
        }
    }
    fun setHBAdress() { hoverballAdress = "hoverball.net:1234#spielekiste${client.gameName}" }
    fun reconnect() {
        thread {
            reconnectButton.isDisable = true

            portValue = if (serverDefaults) standartPort else model.port.value.toInt()
            serverValue = if (serverDefaults) if (parameter.raw.isEmpty()) "server.maxi.li" else "ZenBook" else model.server.value

            try {
                client.install(serverValue, portValue)
                client.send(Info(actualType, "before-start"))
                finished = true

                if (version != client.serverVersion && client.serverVersion != "-1") really(
                    "Du benutzt eine ältere Version ($version | Neuste: ${client.serverVersion})",
                    "Dies kann schwerwiegende Probleme hervorrufen,\n    da Versionen sich unterscheiden.\n",
                    jesText = "Neuste Version herunterladen", noText = "Risiko akzeptieren", onClose = {}
                ) {
                    if (it) hostServices.showDocument("${homepage}src/HeyPlay-${client.serverVersion}.jar")
                }

                connected = true

                if (LaunchUI.OPENSOFORT) {
                    thread {
                        Thread.sleep(500)
                        go()
                    }
                }
            }
            catch (e: Exception) {
                val s = when (e) {
                    is UnknownHostException -> "Serveradresse nicht gefunden!"
                    is NoRouteToHostException -> "Route zu Server nicht gefunden!"
                    is ConnectException -> "Verbindung fehlgeschlagen!"
                    else -> {
                        e.printStackTrace()
                        "Nicht bekannter Error"
                    }
                }
                info(s, type = "error", pane = root)
                connected = false
            }

            reconnectButton.isDisable = false
        }
    }
    fun setDefaultSettings() {
        Platform.runLater {
            serverDefaultControls.forEach {
                it.isVisible = !serverDefaults
            }
        }
    }
    fun go() {
        if (connected) {
            fun f() {
                if (model.isValid) {
                    fun f1() {
                        if (true || gamesOnlines[client.gameName]!!.stage != "waiting") {
                            client.username = model.name.value
                            client.userrole = actualRole
                            if (client is HoverballClient) (client as HoverballClient).team = model.teamName.value
                            client { sendUserStart() }
                        } else info("Es besteht bereits eine Partie", pane = root)
                    }
                    if (gamesOnlines.values.find { it.name == client.gameName } == null) {
                        if (!LaunchUI.OPENSOFORT) really(
                            "Willst du das Spiel \"${client.gameName}\" erstellen?",
                            "Dieses Spiel existiert nämlich noch nicht."
                        ) {
                            if (it) f1()
                        } else f1()
                    } else f1()
                } else info("Textfelder sind nicht valid!", pane = root)
            }
            f()
        } else info("Es besteht bereits noch keine Verbindung", pane = root)
    }
    fun changeSettings(g: String) {
        val s = mySettings[g]
        Platform.runLater {
            println(s)
            if (s != null) {
                settingsPane.isVisible = true
                actualSettings.replaceWith(s.apply { actualSettings = this })
            }
            else settingsPane.isVisible = false
            primaryStage.pack()
        }
    }
    fun settings(args: Map<Pair<String, Boolean>, GridPane.(Node.() -> Unit) -> Unit>) = GridPane().apply {
        alignment = Pos.CENTER
        hgap = 10.0
        vgap = 5.0

        var i = 0
        args.forEach { (a, b) ->
            label(a.first) {
                addClass("small-heading-label"); gridpaneConstraints { columnRowIndex(0, i) }
                if (a.second) serverDefaultControls plus this
            }
            b { gridpaneConstraints { columnRowIndex(1, i) } }
            i ++
        }
    }

    companion object {
        private fun installImage(v: ImageView): ImageView {
            val w = 994.0; val h = 588.0; val fak = 0.5
            v.fitWidth = w*fak
            v.fitHeight = h*fak
            return v
        }

        fun changeImage(type: String) {
            if (currentImage != images[type]) currentImage?.replaceWith(images[type].apply { currentImage = this }!!, ViewTransition.Fade(1.5.seconds))
        }
        val images: Map<String, ImageView> = mapOf(
                "chess" to installImage(ImageView("image/chess2.png")),
                "vgw" to installImage(ImageView("image/vgw.png")),
                "mtm" to installImage(ImageView("image/montagsmaler.png")),
                "wizard" to installImage(ImageView("image/wizardBanner.png")),
                "hoverball" to installImage(ImageView("image/hoverballBanner.png"))
        )
        var currentImage: ImageView? = null
    }
}

fun forAllNodes(root: Parent, f: Node.() -> Unit) {
    val nodes = ArrayList<Node>()
    addAllDescendents(root, nodes)
    nodes.forEach { it.f() }
}

private fun addAllDescendents(
        parent: Parent,
        nodes: ArrayList<Node>
) {
    for (node in parent.childrenUnmodifiable) {
        nodes.add(node)
        if (node is Parent) addAllDescendents(node, nodes)
    }
}
/*fun GridPane.hid(a: Node.() -> Unit, b: Property<String>, bool: Boolean = true) {
    textfield(b) {
        text = if (bool) hoverballPort.toString() else "server.hoverball.net"
        addClass("my-textfield")
        maxWidth = if (bool) 65.0 else 170.0
        enter { reconnect() }
        if (!bool) style { textFill = Color.WHITE }
        a()
        if (bool) validator { n ->
            var m: ValidationMessage? = null
            if (n != null) {
                if (n.length < 2) m = error("Der angegebene Text ist zu kurz.")
                else if (!n.matches(Regex("[0-9]+")))
                    m = error("Der Port darf nur Ziffern enthalten.")
            }
            m
        }
        serverDefaultControls plus this
    }
}*/