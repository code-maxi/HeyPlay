package main

import tornadofx.*

class Test : View() {
    override val root = scrollpane {
        flowpane {
            useMaxSize = true
            Start.installImages()
            for (i in Start.wizardcards) imageview(i.value) {
                fitWidth = i.value.width/1.0
                fitHeight = i.value.height/1.0
            }
        }
    }
}