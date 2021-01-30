package gui.dialog

import javafx.animation.RotateTransition
import javafx.geometry.Pos
import javafx.scene.transform.Rotate
import javafx.stage.Stage
import javafx.util.Duration
import adds.openNewWindow
import tornadofx.*

class LoadingDialog(vararg text: String) : View() {
    fun schliesen() { stage.close() }
    override val root = hbox {
        addClass("message-container")
        alignment = Pos.CENTER
        spacing = 10.0
        imageview("image/loading.png") {
            val ani = RotateTransition().apply {
                axis = Rotate.Z_AXIS
                byAngle = 360.0
                cycleCount = 500
                duration = Duration.millis(1200.0)
                node = this@imageview
                play()
            }
            fitHeight = 40.0
            fitWidth = 40.0
        }
        vbox {
            useMaxSize = false
            alignment = Pos.CENTER
            spacing = 5.0
            addClass("message-label-container")
            for (i in text) {
                label(i) { addClass("message-label"); useMaxSize = false }
            }
        }
    }
    lateinit var stage: Stage
    init {
        openNewWindow(view = root, stage = primaryStage, dialog = true) { stage = this }
    }
}