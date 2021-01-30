package network.data

import java.io.Serializable

data class VorschlagenItem(val value: Serializable, val text: String, val thema: String, val from: Online, val under: String? = null) : Serializable
data class VorschlagenItemBack(val b: Boolean, val subject: String) : Serializable
data class VorschlagenAngenommen(val b: Boolean, val thema: String, val text: String, val from: Online) : Serializable
data class MessagesData(val m: ArrayList<Message>) : Serializable
data class GameData(
    val name: String,
    val type: String,
    val stage: String,
    var onlines: Onlines
) : Serializable
data class GamesData(val l: ArrayList<GameData>) : Serializable