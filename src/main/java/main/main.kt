package main

import adds.toHashMap
import gui.gamePanel
import gui.mainPanel
import hoverball.Application
import hoverball.Controller
import hoverball.Human
import javafx.application.Platform
import network.*
import network.GameClient
import network.data.GameData
import network.data.Info
import network.data.OnlineI
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameUI
import javax.swing.border.EmptyBorder
import kotlin.concurrent.thread

lateinit var client: GameClient
fun icin() = ::client.isInitialized

val chessClient: ChessClient
    get() = client as ChessClient

val vgwClient: VGWClient
    get() = client as VGWClient

val mtmClient: MTMClient
    get() = client as MTMClient

val wizardClient: WizardClient
    get() = client as WizardClient

val hbClient: HoverballClient
    get() = client as HoverballClient

lateinit var screen: Container
lateinit var hbFrame: JFrame
lateinit var controller: Controller

var hoverballVisible = false
fun innerframe(): JInternalFrame {
    var frame: Container = controller.viewer;
    while (frame !is JInternalFrame) frame = frame.parent
    return frame
}
fun frame(): JFrame {
    var frame: Container = controller.viewer;
    while (frame !is JFrame) frame = frame.parent
    return frame
}
fun innerdesktop(): JDesktopPane {
    var frame: Container = controller.viewer;
    while (frame !is JDesktopPane) frame = frame.parent
    return frame
}

lateinit var hoverballTop: HoverballTop
class HoverballTop : JPanel() {
    lateinit var pauseButton: JButton
    lateinit var quitButton: JButton
    lateinit var selfQuitButton: JButton
    lateinit var buttonPane: JPanel
    lateinit var really: JPanel
    lateinit var reallyLabel: JLabel
    lateinit var infoLabel: JLabel
    var reallyF: (Boolean) -> Unit = {}
    val bcColor = Color(24,24,60)
    lateinit var powerBar: MyProgress
    val powerTimer = Timer(30) {
        if (powerBar.value < 100) powerBar.value += 1
        else powerBar.value = 100
    }
    init {
        Timer(500) { if (hoverballVisible && hoverballTop.isVisible) fix() }.apply { start() }
        SwingUtilities.invokeLater {
            layout = BorderLayout()
            border = EmptyBorder(5, 5, 0, 0)
            background = bcColor
            fun JButton.i(b: Color = Color(24,24,150)) {
                background = b
                foreground = Color(255,255,255)
                border = EmptyBorder(2, 4, 2, 4)
                addActionListener { SwingUtilities.invokeLater { innerframe().contentPane.requestFocus() } }
            }

            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                background = bcColor
                add(JPanel().apply {
                    background = bcColor
                    buttonPane = this
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(JButton("Pause").apply {
                        i(); pauseButton = this
                        addActionListener {
                            when (text) {
                                "Pause" -> client.vorschlagen(
                                    "", "Bist du dafür, das Spiel pausieren?",
                                    "pause game"
                                )
                                "Weiterspielen" -> client.vorschlagen(
                                    "", "Bist du dafür, weiterzuspielen?",
                                    "go game"
                                )
                            }
                        }
                    })
                    add(JLabel("  "))
                    add(JButton("Spiel Abbrechen").apply {
                        i(); quitButton = this
                        addActionListener {
                            client.enableAll(false)
                            makeReally("Willst du das Spiel wirklich abbrechen?") {
                                if (it) client.vorschlagen(
                                    "", "Bist du dafür, das Spiel jetzt zu beenden?",
                                    "quit game"
                                )
                                else client.enableAll(true)
                            }
                        }
                    })
                    add(JLabel("  "))
                    add(JButton("Aussteigen").apply {
                        selfQuitButton = this; i(Color(100, 0, 0))
                        addActionListener {
                            client.enableAll(false)
                            makeReally("Willst du wirklich aussteigen?") {
                                client.enableAll(true)
                                if (it) {
                                    client.send(Info("${client.username} ist vorzeitig ausgestiegen.", "hbinfo"))
                                    hbClient.human?.disconnect()
                                    controller.disconnect()
                                    visibleHoverball(false)
                                    Platform.runLater { gamePanel.primaryStage.close() }
                                    client.bye()
                                }
                            }
                        }
                    })
                })
                add(JLabel("      "))
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    background = bcColor
                    add(JLabel().apply { foreground = Color(255,247,205); isVisible = false; infoLabel = this })
                    add(JPanel().apply {
                        really = this
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        background = bcColor
                        add(JLabel().apply { foreground = Color(230, 230, 230); reallyLabel = this })
                        add(JButton("Ja").apply {
                            i(Color(0,100, 0))
                            addActionListener { reallyF(true); really.isVisible = false }
                        })
                        add(JLabel("  "))
                        add(JButton("Nein").apply {
                            i(Color(100,0, 0))
                            addActionListener { reallyF(false); really.isVisible = false }
                        })
                        isVisible = false
                    })
                }, BorderLayout.WEST)
                /*add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    background = bcColor
                    add(JLabel("Energie  ").apply { foreground = Color(255,247,205) })
                    add(MyProgress(300, 40, 100).apply { powerBar = this })
                }, BorderLayout.EAST)*/
                /*thread {
                    Thread.sleep(1000)
                    SwingUtilities.invokeLater {
                        doLayout()
                        revalidate()
                        repaint()
                    }
                }*/
            })
        }
    }
    fun fix() {
        //doLayout()
        //revalidate()
        //frame().pack()
        repaint()
    }
    fun makeReally(text: String, f: (Boolean) -> Unit) {        SwingUtilities.invokeLater {
            reallyF = f
            reallyLabel.text = "   $text    "
            really.isVisible = true
        }
    }
    fun makeInfo(t: String) {
        SwingUtilities.invokeLater {
            infoLabel.text = "   $t"
            infoLabel.isVisible = true
            thread {
                Thread.sleep(3000)
                infoLabel.isVisible = false
            }
        }
    }
    fun enableButtons(b: Boolean) {
        SwingUtilities.invokeLater {
            quitButton.isEnabled = b
            pauseButton.isEnabled = b
            selfQuitButton.isEnabled = b
        }
    }
    class MyProgress(val w: Int, val h: Int, val max: Int) : JPanel() {
        var value = 0
            set(value) {
                field = value
                repaint()
            }
        init {
            maximumSize = Dimension(w, h)
            size = Dimension(w, h)
            minimumSize = Dimension(w, h)
        }

        override fun paintComponent(g: Graphics?) {
            val g2 = g as Graphics2D
            g2.stroke = BasicStroke(2f)
            Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()).let { b ->
                Rectangle2D.Double(0.0, 0.0, w.toDouble() * (value.toDouble() / max.toDouble()), h.toDouble()).let { v ->
                    g2.color = Color(152, 152, 152)
                    g2.fill(b)
                    g2.color = Color.YELLOW
                    g2.fill(v)
                }
            }
        }
    }
}

fun installHoverball() {
    SwingUtilities.invokeLater {
        controller.apply {
            Application.setBounds(Dimension(1000, 800))
            val inner = innerframe()
            inner.isMaximum = true
            inner.border = null
            (inner.ui as BasicInternalFrameUI).northPane = null
            inner.contentPane.addKeyListener(Human.listener)
            frame().apply {
                hoverballTop = HoverballTop()
                contentPane.add(hoverballTop, BorderLayout.NORTH)

                contentPane.addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent?) {
                        if (e?.keyCode == KeyEvent.VK_Y) {
                            hoverballTop.apply {
                                powerBar.value = 0
                                powerTimer.start()
                            }
                        }
                    }

                    override fun keyReleased(e: KeyEvent?) {
                        if (e?.keyCode == KeyEvent.VK_Y) {
                            hoverballTop.apply {
                                powerTimer.stop()
                                powerBar.value = 100
                            }
                        }
                    }
                })
            }
            visibleHoverball(false)
        }
    }
}
fun visibleHoverball(b: Boolean) {
    SwingUtilities.invokeLater {
        frame().apply {
            isVisible = b
            extendedState = JFrame.MAXIMIZED_BOTH
        }
        innerframe().apply {
            isVisible = b
            if (b) contentPane.requestFocus()
        }
        hoverballTop.isVisible = client.started && gamesOnlines[client.gameName]!!.stage == "running"
    }
    if (!b) Platform.runLater { mainPanel().primaryStage.toFront() }
    hoverballVisible = b
}

fun iconi() = ::controller.isInitialized

val hoverballPort = 1234
var standartPort = 2442

const val staticW = 1920.0
const val STATIC_H = 1080.0
val screenW
    get() = Toolkit.getDefaultToolkit().screenSize.width.toDouble()
val screenH
    get() = Toolkit.getDefaultToolkit().screenSize.height.toDouble()

var clientQuiet = true
var serverQuiet = true

var allGamesOnlines = hashMapOf<String, GameData>()
val gamesOnlines: HashMap<String, GameData>
    get() = allGamesOnlines.filter { it.value.type == client.type() }.toHashMap()
val onlines: ArrayList<OnlineI>
    get() = gamesOnlines[client.gameName]?.let { return@let it.onlines.l } ?: arrayListOf()
val redUser: OnlineI?
    get() = onlines.find { it.o().role == "red" }
val yellowUser: OnlineI?
    get() = onlines.find { it.o().role == "yellow" }
val whiteUser: OnlineI?
    get() = onlines.find { it.o().role == "white" }
val blackUser: OnlineI?
    get() = onlines.find { it.o().role == "black" }

fun onUsers(f: (OnlineI) -> Unit) { for (u in onlines) f(u)  }