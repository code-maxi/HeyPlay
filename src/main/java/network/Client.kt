package network

import gui.GameTimer
import main.clientQuiet
import main.version
import network.data.*
import java.io.*
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

abstract class Client {
    lateinit var host: InetAddress
    lateinit var socket: Socket
    lateinit var output: ObjectOutputStream
    lateinit var input: ObjectInputStream
    lateinit var userrole: String
    var username = "unnamed"
    var usertippt: Boolean = false
    var startListener: (s: Serializable) -> Unit = {}
    var started = false
    var closed = false
    var id = -1
    var gameName = ""

    var port = -1
    var server: String? = null

    @Throws(Exception::class)
    fun install(server: String?, port: Int) {
        this.port = port
        this.server = server
        host = if (server != null) InetAddress.getByName(server) else InetAddress.getLocalHost()
        println("${host.hostName} $port")
        socket = Socket(host.hostName, port)
        //socket.setSoTimeout(60000)

        //println("Socket to Server created.")
        output = ObjectOutputStream(socket.getOutputStream())
        input = ObjectInputStream(socket.getInputStream())

        afterInstall()

        thread { superListen() }
        //println("Socket is listening...")
    }
    open fun afterInstall() {}

    fun removeChatToServer() { send("ra") }

    fun sendUser(n: String? = null) { send(Online(n ?: username, userrole, id, gameName, usertippt)) }

    fun whichServer() = when(this) {
        is ChessClient -> "chess"
        is VGWClient -> "vgw"
        is MTMClient -> "mtm"
        is WizardClient -> "wizard"
        is HoverballClient -> "hoverball"
        else -> "--"
    }

    open fun sendUserStart() {
        send(OnlineStart(
                Online(username, userrole, id, gameName, usertippt),
                version, gameName, whichServer()
        ))
    }

    private fun superListen() {
        println("User.listen()")
        try {
            while (!closed) {
                val message: Any? = try {
                    input.readObject()
                } catch (e: ClassNotFoundException) { null }
                if (!clientQuiet) println("ClientSocket[$username] recieved: $message")

                if (message is Serializable) {
                    if (message is Info) {
                        if (message.subject == "time") {
                            GameTimer.time = Integer.parseInt(message.text)
                        }
                    }
                    listen(message)
                }

            }
        } catch (e: IOException) {
            if (!closed) {
                e.printStackTrace()
            }
        } finally {
            bye()
        }
    }

    fun bye() {
        if (closed) return
        closed = true
        if (this::output.isInitialized) send("bye")
        if (this::socket.isInitialized) socket.close()
        println("Bye.")
    }

    fun send(o: Serializable) {
        try {
            if (!clientQuiet) println("ClientSocket[${username}] sent: $o")
            output.writeObject(o)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun changeName(s: String) {
        sendUser(s)
    }

    abstract fun listen(o: Serializable)
    abstract fun installAll(server: String?, port: Int, name: String, role: String)
}