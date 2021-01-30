package gui.chat

import adds.bindScrollBottom
import adds.key
import adds.pack
import adds.wordwrap
import main.game
import javafx.animation.ScaleTransition
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Duration
import main.*
import network.data.*
import tornadofx.*
import kotlin.collections.plusAssign
import kotlin.concurrent.thread


val chat = Chat()

var ownMessages = observableList<Chat.ChatPane.MassagePane>()

class Chat : View() {
    val chatWidth = 450.0
    lateinit var scrollPane: ScrollPane
    val chatPanel = ChatPane()
    val onlineUser = OnlineUsers()
    val textareaText = SimpleStringProperty()
    override val root = borderpane {
        id = "image-container"
        useMaxHeight = true

        maxWidth = chatWidth
        prefWidth = chatWidth
        minWidth = chatWidth

        vgrow = Priority.ALWAYS
        hgrow = Priority.NEVER

        managedProperty().bind(visibleProperty())
        visibleProperty().onChange { primaryStage.pack() }

        top { this += onlineUser }
        center {
            useMaxSize = true
            scrollpane {
                useMaxSize = true
                isFitToWidth = true
                scrollPane = this
                addClass("blue-scroll-pane")
                content = chatPanel.root
                bindScrollBottom()
                style { backgroundColor += c(0.0,0.0,0.0,0.0) }
            }
        }
        bottom { this += ChatField() }
    }
    fun send() {
        if (textareaText.value != null && textareaText.value != "") {
            client.send(Message(textareaText.value, client.online()))
            textareaText.value = ""
        }
    }
    inner class ChatField : View() {
        lateinit var sendB: Button
        lateinit var camaraB: Button
        lateinit var actualButton: Button
        override val root = vbox {
            useMaxWidth = true
            alignment = Pos.CENTER

            this += SmelyList()
            hbox {
                alignment = Pos.CENTER
                useMaxWidth = true

                sendB = Button().apply {
                    graphic = imageview("image/send.png") {
                        fitWidth = 22.0
                        fitHeight = 22.0
                    }
                    id = "send-button"
                    action { send() }
                }
                camaraB = Button().apply {
                    graphic = imageview("image/camara.png") {
                        fitWidth = 24.0
                        fitHeight = 24.0
                    }
                    id = "send-button"
                    action { send() }
                }

                textarea(textareaText) {
                    useMaxSize = true
                    maxHeight = 70.0
                    prefHeight = 70.0
                    id = "send-field"
                    isWrapText = true
                    textProperty().onChange {
                        if (it != null) {
                            if (it.isNotEmpty()) {
                                if (!client.usertippt) {
                                    client.send("tippt")
                                    changeToButton(sendB)
                                    client.usertippt = true
                                }
                            }
                            else {
                                if (client.usertippt) {
                                    client.send("!tippt")
                                    changeToButton(camaraB)
                                    client.usertippt = false
                                }
                            }
                        }
                    }
                    hboxConstraints {
                        marginRight = 10.0
                    }
                    key({ k: KeyEvent -> k.code == KeyCode.ENTER && (k.isShiftDown || k.isControlDown) } to { send() })
                }

                this += camaraB.apply { actualButton = this }
                style {
                    padding = box(5.px)
                }
            }
        }

        fun changeToButton(b: Button) { actualButton.replaceWith(b.apply { actualButton = this }) }

        fun reload() {
            Platform.runLater {
                root.layout()
                root.requestLayout()
                primaryStage.reloadViewsOnFocus()
                //primaryStage.pack()
            }
        }

        inner class SmelyList : View() {
            val smelyList = arrayOf("😃", "😆", "😈", "😍", "😎", "😝", "😠", "😧", "😴")
            override val root = hbox {
                useMaxSize = true
                alignment = Pos.CENTER
                for (s in smelyList) {
                    button(s) {
                        addClass("smely-button")
                        hboxConstraints {
                            marginRight = 4.0
                        }
                        action { if (text != null) this@Chat.textareaText += text }
                    }
                }
            }
        }
    }

    inner class ChatPane : View() {
        lateinit var panel: VBox
        var zwischenChildren: Array<Node> = arrayOf()

        override val root = vbox {
            useMaxWidth = true
            panel = this
            style { padding = box(10.px) }
        }

        init {
            thread {
                Thread.sleep(1000)
                Platform.runLater {
                    root.children.bind(ownMessages) { it.root }
                }
            }
        }

        fun clearChildren() {
            zwischenChildren = arrayOf()
            for (i in root.children) zwischenChildren += i
            root.children.clear()
        }
        fun setChildren() {
            for (i in zwischenChildren) root.children.add(i)
        }

        fun addMessage(message: Message, animate: Boolean = true) {
            Platform.runLater {
                ownMessages.add(
                        MassagePane(
                                message,
                                !(ownMessages.isNotEmpty() && ownMessages.last().m.o.name == message.o.name)
                        ).apply { if (animate) animate() }
                )
            }
        }

        inner class MassagePane(val m: Message, val name: Boolean) : View() {
            override val root = hbox {
                useMaxWidth = true
                alignment = if (m.anchor == Message.LEFT) Pos.BASELINE_LEFT else { Pos.BASELINE_RIGHT }
                vbox {
                    useMaxSize = false
                    if (name) label(m.o.name) {
                        useMaxSize = true
                        addClass("message-name")
                    }
                    label(wordwrap(m.text, 45)) {
                        useMaxSize = true
                        addClass("message-text")
                        if (!name) style { backgroundRadius = multi(box(10.px)) }
                    }
                }
                style {
                    padding = box(if (!name) 4.px else 10.px, 0.px, 0.px, 0.px)
                }
            }
            fun animate() {
                Platform.runLater {
                    root.apply {
                        if (game.animationen) ScaleTransition(Duration.millis(200.0), this).apply {
                            byX = 0.0; byY = 0.2
                            cycleCount = 4
                            isAutoReverse = true
                            play()
                        }
                    }
                }
            }
        }
    }
}

class OnlineUsers : View() {
    lateinit var pane: VBox
    var items = ArrayList<Item>()
    override val root = vbox {
        pane = this
        useMaxSize = false
        style {
            backgroundColor += c(0.0,0.0,0.0,0.0)
        }
    }
    fun reloadList() {
        Platform.runLater {
            root.children.clear()
            for (i in items.indices) {
                if (i < items.size)
                    root += items[i]
            }
            primaryStage.pack()
        }
    }
    fun reload() {
        items.clear()
        fun f(o: OnlineI?) { if (o != null) items.add(Item(o)) }
        for (i in onlines) f(i)
        reloadList()
    }
    fun addItem(o: Online?) { o?.let { pane.children += Item(it).apply { items.plusAssign(this) }.root } }
    fun reloadItem(id: Int, b: Boolean) {
        items.find { it.o.o().id == id }?.let { it.o.o().tippt = b }
        items.forEach { it.reload() }
    }
    inner class Item(var o: OnlineI) : View() {
        lateinit var nameL: Label
        lateinit var tipptL: Label
        override val root = borderpane {
            managedProperty().bind(visibleProperty())
            visibleProperty().onChange { primaryStage.pack() }

            addClass("online-pane")

            left {
                label {
                    addClass("online-name")
                    nameL = this
                }
            }
            right {
                label("tippt...") {
                    addClass("online-tippt")
                    tipptL = this
                }
            }
        }
        fun reload() {
            Platform.runLater {
                nameL.text = o.o().name
                root.isVisible = o.o().tippt && o.o().id != client.id
            }
        }

        init {
            reload()
        }
    }
}

fun Node.removeAllClasses() {
    for (i in styleClass.indices) removeClass(styleClass[i])
}
