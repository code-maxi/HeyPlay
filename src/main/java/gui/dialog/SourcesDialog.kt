package gui.dialog

import main.StartApp
import javafx.scene.Scene
import javafx.stage.Stage
import main.Start
import main.Start.Companion.stylesheet
import tornadofx.*
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern


class SourcesDialogView : View() {
    override val root = borderpane {
        addClass("sources-container")

        top {
            label("Quellen:") {
                addClass("welcome-label")
                borderpaneConstraints { marginBottom = 10.0 }
            }
        }

        center {
            gridpane {
                hgap = 30.0
                vgap = 5.0
                fun crow(l: String, h: String) {
                    row {
                        label(l) { addClass("sources-label") }
                        hyperlink(h) {
                            addClass("sources-link")
                            action {
                                hostServices.showDocument(h)
                            }
                        }
                    }
                }
                for (i in Start.sources) crow(i.key, i.value)
            }
        }
    }
}

class SourcesDialog : Stage() {
    init {
        val s = Scene(SourcesDialogView().root)
        s.stylesheets.add(stylesheet)
        scene = s
        isResizable = false
        show()
    }
}

