package main

import adds.dots
import adds.pack
import adds.skalieren
import javafx.application.Platform
import javafx.application.Preloader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.stage.Stage
import tornadofx.*
import javax.swing.Timer
import kotlin.system.exitProcess

lateinit var startScreen: SpielekistePreloader
class SpielekistePreloader : Preloader() {
    lateinit var preloader: PreloaderPane
    lateinit var stage: Stage
    override fun start(s: Stage?) {
        preloader = PreloaderPane()
        Platform.runLater {
            s!!.apply {
                scene = Scene(preloader.root).apply { stylesheets.add(Start.stylesheet) }
                isResizable = true
                show()
                stage = this
            }
        }
    }
    init { startScreen = this }

    override fun handleStateChangeNotification(info: StateChangeNotification?) {
        if (info?.type == StateChangeNotification.Type.BEFORE_START) {
            stage.hide()
        }
    }
}
lateinit var preloader: PreloaderPane
class PreloaderPane : View() {
    lateinit var loadingLabel: Label
    var count = 1
    val timer = Timer(500) {
        if (count > 3) count = 1
        else count ++
        Platform.runLater { loadingLabel.text = "Loading${dots(count)}" }
    }.apply { start() }
    override val root = borderpane {
        prefWidth = 720.0
        prefHeight = 480.0
        addClass("preloader-container")
        bottom {
            useMaxSize = false
            right {
                label("Version $version") {
                    addClass("version-label")
                }
            }
            left {
                loadingLabel = label {
                    addClass("preloader-loading")
                }
            }
        }
        skalieren()
    }
    init {
        preloader = this
        primaryStage.pack()
        primaryStage.setOnCloseRequest { exitProcess(0) }
    }
}