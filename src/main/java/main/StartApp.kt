package main

import adds.pack
import gui.Login
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import main.Start.Companion.installImages
import main.Start.Companion.stylesheet
import tornadofx.*
import kotlin.concurrent.thread

lateinit var pstage: Stage
lateinit var parameter: javafx.application.Application.Parameters
class StartApp : App(PreloaderPane::class) {
    override fun start(stage: Stage) {
        parameter = parameters
        super.start(stage)
        pstage = stage
        stage.scene.stylesheets.add(stylesheet)
        stage.icons.add(Image("image/rotatedDice.png"))
        if (LaunchUI.OPENSOFORT) {
            stage.isAlwaysOnTop = true
            stage.toFront()
        }
        stage.isResizable = true

        thread {
            installImages()

            Platform.runLater {
                println("Hallo?")
                stage.scene.root = Login().root
                stage.pack()
                stage.isResizable = true
            }
        }
    }
}