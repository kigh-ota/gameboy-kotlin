import javafx.application.Application

fun main(args: Array<String>) {
    Application.launch(JavaFXGameBoyApplication::class.java, *args)
}

fun UShort.getBit(b: Int) = (this.toUInt() shr b) and 1u == 1u

abstract class Screen {
    companion object {
        const val WIDTH = 160u
        const val HEIGHT = 144u
    }

    abstract fun init()
    abstract fun flush()
    abstract fun clear()
    abstract fun draw(vx: UByte, vy: UByte, sprites: List<UByte>): Boolean
}
