package game.chess.chessPieces

import main.chess
import game.chess.ChessField
import game.chess.H
import game.chess.W
import game.chess.fieldSize

class King(c: String, iid: Int? = null) : ChessPiece(iid) {
    override val type = "king"
    override val width = fieldSize / 1
    override val height = fieldSize / 1
    override val color = c

    var schach = false

    var rochadeLeft: RochadeData = RochadeData(null, null, null)
    var rochadeRight: RochadeData = RochadeData(null, null, null)

    data class RochadeData(var r: ChessField?, var p: ChessField?, var rp: ChessField?)
    data class PossiblePiece(val p: ChessPiece, var f: Array<ChessField>)

    private fun testChess(): Boolean {
        val fids = getPossibleFields({ it.color != color })
        for (ii in fids) {
            for (i in ii.f) {
                if (i.pp == pp) return true
            }
        }
        return false
    }

    fun schach() {
        //chess.fields[pp.x][pp.y].piece = null
        var o = 0

        schach = testChess()

        val fids = getPossibleFields({ it.color == color })

        for (p in fids) {
            val beforePos = p.p.pp
            chess.fields[beforePos.x][beforePos.y].piece = null
            p.p.possibleFieldsArray = arrayOf()

            for (f in p.f) {
                val beforePiece = f.piece
                f.piece = p.p

                val t = testChess()
                if (!t) {
                    p.p.possibleFieldsArray = p.p.possibleFieldsArray!! + f.pp
                }

                f.piece = beforePiece
            }

            if (p.p.possibleFieldsArray!!.isNotEmpty()) {
                o ++
                p.p.schachPossble = true
            }

            chess.fields[beforePos.x][beforePos.y].piece = p.p
        }

        if (!schach) {
            chess.fields {
                it.piece?.apply {
                    schachPossble = null
                    possibleFieldsArray = null
                }
            }
        }

        var oo = 0
        possibleFields { oo ++ }
        if (oo != 0) o ++

        schachPossble = if (schach && oo > 0) true else null

        if (schach && o == 0) chess.gewonnen()
        else if (!schach && o == 0) chess.unentschieden()
        //chess.fields[pp.x][pp.y].piece = this
    }

    fun rochiere() {
        if (moved == 0 && !schach) {
            val p = getPossibleFields({ it.color != color })

            fun pruefe(x: Int, y: Int): Boolean {
                val f = chess.fields[x][y]
                var inSchach = false
                for (a in p) {
                    for (b in a.f) {
                        if (b.pp == f.pp) inSchach = true
                    }
                }
                return f.piece == null && !inSchach
            }

            fun pruefeRochadeRight() =
                    rochadeRight.r != null && rochadeRight.r!!.piece!!.moved == 0 &&
                    pruefe(pp.x+1, pp.y) &&
                    pruefe(pp.x+2, pp.y)

            rochadeRight.r = if (chess.fields[W-1][pp.y].piece != null) chess.fields[W-1][pp.y] else null
            rochadeRight.p = if (pruefeRochadeRight()) chess.fields[W-2][pp.y] else null
            rochadeRight.rp = if (rochadeRight.p != null) chess.fields[W-3][pp.y] else null

            fun pruefeRochadeLeft() =
                    pruefe(pp.x-1, pp.y) &&
                    pruefe(pp.x-2, pp.y) &&
                    pruefe(pp.x-3, pp.y) &&
                    rochadeLeft.r != null && rochadeLeft.r!!.piece!!.moved == 0

            rochadeLeft.r = if (chess.fields[W-1][pp.y].piece != null) chess.fields[0][pp.y] else null
            rochadeLeft.p = if (pruefeRochadeLeft()) chess.fields[1][pp.y] else null
            rochadeLeft.rp = if (rochadeLeft.p != null) chess.fields[2][pp.y] else null

            rochadeRight.p?.rochade = true
            rochadeLeft.p?.rochade = true
        }
    }

    private fun possibleFields2(f: (ChessField) -> Unit, fids: Array<PossiblePiece>, idd: Int) {
        fun pruefe(x: Int, y: Int) {
            fun deckung(f: ChessField): Boolean {
                val before = f.piece
                f.piece = null
                val ff = getPossibleFields({ it.color != color })
                f.piece = before

                for (a in ff) {
                    for (fi in a.f) {
                        if (fi.pp == f.piece!!.pp) {
                            return true
                        }
                    }
                }
                return false
            }

            var bool = false
            loop@ for (i in fids) {
                for (ii in i.f) {
                    if (x == ii.pp.x && y == ii.pp.y) {
                        bool = true
                        break@loop
                    }
                }
            }
            val f = if (x >= 0 && y >= 0 && x < W && y < H) chess.fields[x][y] else null
            val b = f != null &&
                    ((f.piece == null && !bool) ||
                    (f.piece != null && f.piece!!.color != color && bool && !deckung(f)))

            if (b) {
                f(chess.fields[x][y])
            }
        }

        pruefe(pp.x + 0, pp.y + 1)
        pruefe(pp.x + 1, pp.y + 1)
        pruefe(pp.x + 1, pp.y + 0)
        pruefe(pp.x + 1, pp.y - 1)

        pruefe(pp.x + 0, pp.y - 1)
        pruefe(pp.x - 1, pp.y - 1)
        pruefe(pp.x - 1, pp.y + 0)
        pruefe(pp.x - 1, pp.y + 1)
    }

    private fun getPossibleFields(check: (ChessPiece) -> Boolean, ff: (PossiblePiece) -> Unit = {}): Array<PossiblePiece> {
        var fids: Array<PossiblePiece> = arrayOf()

        chess.fields { f ->
            f.piece?.let {
                if (it != this && check(it)) {
                    val p = PossiblePiece(it, arrayOf())

                    if (it is King) it.possibleFields2({ f -> p.f += f; }, arrayOf(), it.id)
                    else it.possibleFields { f -> p.f += f }

                    ff(p)
                    fids += p
                }
            }
        }
        return fids
    }

    override fun possibleFields(f: (ChessField) -> Unit) {
        chess.fields[pp.x][pp.y].piece = null
        possibleFields2(f, getPossibleFields({ it.color != color }), id)
        chess.fields[pp.x][pp.y].piece = this
    }
}