package gui

import adds.*
import main.game
import gui.chat.chat
import main.onlines
import main.stack
import main.wizard
import gui.game.wizard.selectRunden
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import main.*
import network.HoverballClient
import network.MTMClient
import network.WizardClient
import tornadofx.*
import javax.swing.Timer

lateinit var gamePanel: GamePanel
fun igpin() = ::gamePanel.isInitialized
fun mainPanel() = if (::gamePanel.isInitialized) gamePanel else login
fun mainPanelBox() = if (::gamePanel.isInitialized) gamePanel.disenableBox else login.disenableBox
class GamePanel(spiel: String, val view: View) : View("$spiel - ${client.username}") {
    lateinit var mainpane: BorderPane
    lateinit var disenableBox: Pane
    lateinit var stack: StackPane
    lateinit var rundenVerringern: MenuItem
    lateinit var boxVisible: Menu
    val overStack: StackPane
        get() = root
    override val root = stackpane {
        mainpane = borderpane {
            maximal()
            top {
                menubar {
                    menu("Chat") {
                        item("Chat ein/aus") {
                            accelerator = KeyCombination.keyCombination("Ctrl+C")
                            action { chat.root.isVisible = !chat.root.isVisible }
                        }
                        item("Chatverlauf Löschen") {
                            action {
                                really(
                                        "Willst du wirklich den ganzen Chatverlauf löschen?",
                                        if (onlines.filterNotNull().size > 1) "Es wird eine Anfrage an alle anderen Spieler gestellt." else null,
                                        pane = stack()
                                ) {
                                    if (it) client.vorschlagen(
                                            "",
                                            "${client.username} will den Chatverlauf löschen.",
                                            "kill-chat"
                                    )
                                }
                            }
                        }
                    }
                    if (client is HoverballClient) menu("Hoverball") {
                        item("Screen an/aus") {
                            accelerator = KeyCombination.keyCombination("Ctrl+H")
                            action { visibleHoverball(!frame().isVisible) }
                        }
                        item("Teams neuladen") {
                            accelerator = KeyCombination.keyCombination("Ctrl+T")
                            action { client.send("update-teams") }
                        }
                    }
                    menu("Spiel") {
                        item("Info") {
                            action {
                                OwnDialog(gamePanel.stack, true, "Info", p = 30) {
                                    vbox {
                                        alignment = Pos.CENTER
                                        spacing = 20.0
                                        imageview (Start.images["main-icon"]!!) {
                                            fitWidth = 100.0
                                            fitHeight = 100.0
                                        }
                                        label("HeyPlay!") {
                                            style {
                                                textFill = Color.LIGHTYELLOW
                                                fontWeight = FontWeight.BOLD
                                                padding = box(4.px, 8.px)
                                                backgroundColor += Color.rgb(120,120,120, 0.4)
                                                backgroundRadius += box(10.px)
                                                fontSize = 19.px
                                            }
                                        }
                                        vbox {
                                            spacing = 3.0
                                            label("Version: $version") {
                                                style {
                                                    textFill = Color.LIGHTYELLOW
                                                    fontSize = 14.px
                                                }
                                            }
                                            label("Server-Version: ${client.serverVersion}") {
                                                style {
                                                    textFill = Color.LIGHTYELLOW
                                                    fontSize = 14.px
                                                }
                                            }
                                        }
                                        vbox {
                                            spacing = 3.0
                                            label("Erstellt von:") { style {
                                                textFill = Color.rgb(220,220,220)
                                                fontSize = 14.px
                                            } }
                                            label("→  Maximilian Bornhofen") { style {
                                                textFill = Color.YELLOW
                                                fontSize = 15.px
                                            } }
                                        }
                                        button("Homepage") {
                                            addClass("center-component-button")
                                            style { fontSize = 16.px }
                                            action { hostServices.showDocument(Start.homepage) }
                                        }
                                    }
                                }
                            }
                        }
                        item("Videokonferenz öffnen") {
                            accelerator = KeyCombination.keyCombination("Ctrl+F")
                            action { Platform.runLater { hostServices.showDocument(client.meetingLink) } }
                        }
                        if (client is WizardClient || client is MTMClient) item("Toggle Fullscreen") {
                            accelerator = KeyCombination.keyCombination("F11")
                            action { gamePanel.primaryStage.apply { isFullScreen = !isFullScreen } }
                        }
                        item("Homepage") {
                            accelerator = KeyCombination.keyCombination("F1")
                            action { hostServices.showDocument(Start.homepage) }
                        }
                    }
                    if (client is WizardClient) menu ("Wizard") {
                        rundenVerringern = item("Rundenanzahl verändern") {
                            bindVisible()
                            isVisible = false
                            action {
                                selectRunden(((wizardClient.runde+2)..(60.0/ onlines.size.toDouble()).toInt()).toList()) {
                                    client.vorschlagen(
                                        it, "${client.username} will die Rundenanzahl auf $it setzen.",
                                        "wizard-runden"
                                    )
                                }
                            }
                        }
                        boxVisible = menu("Entwicklerwerkzeuge") {
                            bindVisible()
                            isVisible = false
                            item("Box Sichtbar") {
                                action {
                                    wizard.centerComponent.boxVisible(false)
                                }
                            }
                            item("Box Unsichtbar") {
                                action {
                                    wizard.centerComponent.boxVisible(false)
                                }
                            }
                        }

                    }
                    menu("Sonstige") {
                        checkmenuitem("Canvas automatisch neu erzeugen") {
                            isSelected = game.canvasReload
                            action { game.canvasReload = isSelected }
                        }
                        checkmenuitem("Animationen") {
                            isSelected = game.animationen
                            action { game.animationen = isSelected }
                        }
                        menu ("Entwicklerwerkzeuge") {
                            item("Gui neuladen") {
                                accelerator = KeyCombination.keyCombination("Ctrl+G")
                                action {
                                    Platform.runLater {
                                        primaryStage.scene.root.layout()
                                        primaryStage.scene.root.requestLayout()
                                        primaryStage.reloadViewsOnFocus()
                                        primaryStage.reloadStylesheetsOnFocus()
                                    }
                                }
                            }
                            item("Fenster packen") {
                                accelerator = KeyCombination.keyCombination("Ctrl+P")
                                action { primaryStage.pack() }
                            }
                            item("Canvas neu erzeugen") {
                                accelerator = KeyCombination.keyCombination("Ctrl+R")
                                action { game.reloadCanvas() }
                            }
                            item("Canvas neu zeichnen") {
                                accelerator = KeyCombination.keyCombination("Ctrl+D")
                                action { game.paint() }
                            }
                            item("Enable Buttons") {
                                accelerator = KeyCombination.keyCombination("Ctrl+E")
                                action { client.enableAll(true) }
                            }
                            item("Disenable Buttons") {
                                action { client.enableAll(false) }
                            }
                            if (client is MTMClient) item("Daten neuladen") {
                                accelerator = KeyCombination.keyCombination("Ctrl+W")
                                action { client.send("send data to me") }
                            }
                        }
                    }
                }
            }
            center {
                hbox {
                    maximal()
                    useMaxSize = true
                    alignment = Pos.CENTER

                    stack = stackpane { maximal(); this += view.apply { maximal() } }
                    this += chat.root.apply { isVisible = !LaunchUI.OPENSOFORT }

                    style { padding = box(5.px) }

                    setOnKeyPressed {
                        if (it.code == KeyCode.P && it.isControlDown) {
                            primaryStage.pack()
                            println("packed!")
                        }
                    }
                }
            }
        }
        this += disenableBox {
            shortcut("Ctrl+B") { isVisible = false }
            disenableBox = this
        }
    }

    class NoConnect : View() {
        var t = 0
        lateinit var tl: Label
        val s = "Wiederherstellen "
        val timer = Timer(500) {
            if (this::tl.isInitialized) {
                var p = ""
                for (i in 0 until t) p += "."
                Platform.runLater { tl.text = s + p }

                if (t == 3) t = 0
                else t ++
            }
        }.apply { start() }
        override val root = vbox {
            alignment = Pos.CENTER
            useMaxSize = true
            spacing = 5.0
            managedProperty().bind(visibleProperty())
            isVisible = false
            style {
                backgroundColor += Color(0.0,0.0,0.0, 0.3)
            }
            label("Verbindung unterbrochen") {
                useMaxSize = false
                addClass("noconnect-label")
                style { fontSize = 15.px }
            }
            tl = label {
                useMaxSize = false
                addClass("noconnect-label")
                style { fontSize = 20.px }
            }
        }
    }

    init {
        chat.onlineUser.reload()
        gamePanel = this
        Platform.runLater { if (isdi()) startDialog.show(overStack) }
    }
}
