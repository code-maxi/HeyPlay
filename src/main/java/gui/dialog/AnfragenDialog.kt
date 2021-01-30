package gui.dialog

import adds.OwnDialog
import main.mtm
import main.stack
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import adds.enter
import adds.maximal
import tornadofx.*

class AnfragenDialog(val f: (String?) -> Unit = {}) {
    val model = Model()
    lateinit var dialog: OwnDialog
    class Model : ViewModel() {
        var text = bind { SimpleStringProperty() }
    }
    fun quit() {
        model.validate()
        if (model.commit { f(model.text.value) }) dialog.schliessen()
    }
    lateinit var textfield: TextField
    val root = VBox().apply {
        maximal(h = false)
        alignment = Pos.CENTER
        spacing = 7.0

        label("Anfrage:") { addClass("anfragen-dialog-label") }
        textfield(model.text) {
            addClass("anfragen-dialog-textfield")
            validator {
                var m: ValidationMessage? = null
                if (it != null) {
                    if (it.length > 22) m = error("Der angegebene Text ist zu lang.")
                }
                m
            }
            enter { quit() }
            textfield = this
        }.required()
        hbox {
            maximal(h = false)
            alignment = Pos.BASELINE_RIGHT
            spacing = 5.0
            button("Abbrechen") {
                addClass("really-ja")
                action {
                    model.validate()
                    f(null)
                    dialog.schliessen()
                }
            }
            button("OK") {
                addClass("really-nein")
                action { quit(); mtm.standart.anfragen.isDisable = true }
            }
        }
    }
    init {
        dialog = OwnDialog(
            stack(), false, null,
            p = 15, s = 0
        ) { this += root }
    }
}
