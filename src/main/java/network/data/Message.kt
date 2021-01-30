package network.data

import java.io.Serializable

data class Message constructor(val text: String, var o: Online, var anchor: Int = LEFT) : Serializable {
    override fun toString(): String { return "Name: ${o.name} || Text: $text" }
    companion object {
        const val LEFT = 0
        const val RIGHT = 1
    }
}