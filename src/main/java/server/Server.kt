package server

import gui.game.wizard.WizardBoard
import gui.hoverballAdress
import main.hoverballPort
import main.serverQuiet
import main.standartPort
import network.data.Info
import server.user.*
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.*
import javax.swing.Timer
import kotlin.concurrent.thread

fun log(o: Any) { if (!serverQuiet) println(o) }

class Server(val port: Int) // nothing to do...
{
    lateinit var listeningThread: Thread
    var active = true
    lateinit var server: ServerSocket

    var time = 0

    val timer = Timer(5000) { time += 5 }

    fun install(start: Boolean = false) {
        WizardBoard.installWizardCards()

        server = ServerSocket(port)
        if (start) println("Server initialized at ${myLocalIP!!.hostName} :: Port[$port]")

        games.forEach { it.value.installSuper() }
    }

    @Synchronized
    fun refresh() {
        server.close()
        install(false)
        println("refreshed")
    }

    fun listen() {
        listeningThread = thread {
            while (active) {
                var socket: Socket?
                try {
                    socket = server.accept()
                    thread {
                        val input = ObjectInputStream(socket.getInputStream())
                        val output = ObjectOutputStream(socket.getOutputStream())
                        val o = input.readObject()
                        println(o)
                        val inf = o as Info

                        if (inf.subject == "before-start") {
                            beforeUsers.add(
                                (when (inf.text) {
                                    "chess" -> ChessUser(socket, output, input)
                                    "vgw" -> VGWUser(socket, output, input)
                                    "mtm" -> MTMUser(socket, output, input)
                                    "wizard" -> WizardUser(socket, output, input)
                                    "hoverball" -> HoverballUser(socket, output, input)
                                    else -> error("Den Typ ${inf.text} gibt es nicht!")
                                }).apply { thread { run() } }
                            )
                            Thread.sleep(500)
                            output.writeObject("queue exited")
                        }
                    }
                } catch (e: IOException) { }
            }
        }
    }

    companion object {
        // not found:
        // ---- Get my local IP [otherwise getLocalHost().getHostAddress() will return 127.0.1.1]
        val myLocalIP: InetAddress?
            get() {
                try {
                    val b = NetworkInterface.getNetworkInterfaces()
                    while (b.hasMoreElements()) {
                        for (f in b.nextElement().interfaceAddresses) if (f.address.isSiteLocalAddress) return f.address
                    }
                    // not found:
                    return InetAddress.getLocalHost()
                } catch (e: SocketException) {
                } catch (e: UnknownHostException) {
                }
                return null
            }
        // ----
    }
}

lateinit var server: Server
fun main(args: Array<String>) {
    if (args.isNotEmpty()) standartPort = args[1].toInt()
    server = Server(standartPort)
    server.install(true)
    server.listen()
}