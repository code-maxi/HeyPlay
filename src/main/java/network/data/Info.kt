package network.data

import java.io.Serializable

data class Info(val text: String, val subject: String) : Serializable
data class Info2(val text: Array<String>, val thema: String) : Serializable
data class SInfo(val thing: Serializable, val thema: String) : Serializable
data class InfoValidID(val id: Int, val name: String, val subject: String) : Serializable
data class Dialog(val s: String, val t: String) : Serializable