package network.data

import java.io.Serializable

data class Onlines(val l: ArrayList<OnlineI>) : Serializable {
    fun rn(s: String) = l.find { it.o().role == s }
    fun r(s: String) = rn(s)!!
}
data class Online(
    var name: String,
    val role: String,
    val id: Int,
    val game: String,
    var tippt: Boolean
) : OnlineI {
    override fun o() = this
    override fun clone() = copy()
}
data class OnlineStart(val o: Online, val version: String, val game: String, val type: String) : Serializable
interface OnlineI : Serializable {
    fun o() : Online
    fun clone(): OnlineI
}