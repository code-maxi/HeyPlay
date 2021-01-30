package server.user

import main.serverQuiet
import server.log
import java.io.*
import java.net.Socket

abstract class User(
    var socket: Socket,
    out: ObjectOutputStream? = null,
    inp: ObjectInputStream? = null
) {
    var output = out ?: ObjectOutputStream(socket.getOutputStream())
    var input = inp ?: ObjectInputStream(socket.getInputStream())
    var started = false
    var closed = false
    var name = "unnamed"

    @Synchronized
    fun send(o: Serializable) {
        log("ServerSocket[$name] sent: $o")
        output.writeObject(o)
    }

    fun run() {
        try {
            log("Socket is listening ...")

            while (!closed) {
                val message: Any? = try { input.readObject() } catch (e: Exception) { e.printStackTrace() }
                if (!serverQuiet) println("ServerSocket[$name] recieved: $message")
                if (message is Serializable) {
                    selfListen(message)
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

    private fun selfListen(message: Serializable) {
        if (message is String) {
            if (message == "bye") {
                bye()
            }
        }
    }

    fun bye() {
        if (closed) return
        closed = true
        log("Bye.")
        try {
            socket.close()
        } catch (e: Exception) {
            log("-- ERROR -- ")
            e.printStackTrace()
        }
        afterBye()
    }

    abstract fun listen(o: Serializable)
    open fun afterBye() {}
}
