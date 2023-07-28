package xyz.emlyn.snapnote

class Note(
    val id: String,
    val text : String,
    val datetime : Long,
    val bgColor : Int,
    var tagColor : Int) {

    fun renderToString() : String {
        val noteSB = StringBuilder()

        noteSB.append(id)
        noteSB.append(Constants.SEP1)
        noteSB.append(text)
        noteSB.append(Constants.SEP2)
        noteSB.append(datetime.toString())
        noteSB.append(Constants.SEP3)
        noteSB.append(bgColor.toString())
        noteSB.append(Constants.SEP4)
        noteSB.append(tagColor.toString())

        return noteSB.toString()
    }

    companion object {
        val idCmp = Comparator<Note> { a, b ->
            when {
                (a.id < b.id) -> -1
                (a.id > b.id) -> 1
                else -> 0
            }
        }

        val txCmp = Comparator<Note> { a, b ->
            when {
                (a.text < b.text) -> -1
                (a.text > b.text) -> 1
                else -> 0
            }
        }

        val dtCmp = Comparator<Note> { a, b ->
            when {
                (a.datetime < b.datetime) -> 1
                (a.datetime > b.datetime) -> -1
                else -> 0
            }
        }

        val tgCmp = Comparator<Note> { a, b ->
            when {
                (a.tagColor < b.tagColor) -> -1
                (a.tagColor > b.tagColor) -> 1
                else -> 0
            }
        }

    }
}