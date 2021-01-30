package main

import javafx.application.Application

class LaunchUI {
    companion object {
        lateinit var params: Array<String>
        var OPENSOFORT = false
        @JvmStatic
        fun main(args: Array<String>) {
            params = args
            OPENSOFORT = params.isNotEmpty() && params.first() == "opensofort"
            Application.launch(StartApp::class.java, *args)
        }
    }
}