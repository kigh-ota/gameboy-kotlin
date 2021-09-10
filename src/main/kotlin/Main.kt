fun main() {

}

@ExperimentalUnsignedTypes
class Emulator {
    companion object {
        private const val MEMORY_SIZE = 0x10000 // 64KiB
        private const val STACK_SIZE = 0x10 // 16 stacks
        private const val TARGET_FREQ = 500u

        private const val MASK_Z: UByte = 0b10000000u
        private const val MASK_N: UByte = 0b01000000u
        private const val MASK_H: UByte = 0b00100000u
        private const val MASK_C: UByte = 0b00010000u

        private fun u16(hi: UByte, lo: UByte) = (lo.toUInt() or (hi.toUInt() shl 8)).toUShort()
        private fun hi8(v: UShort) = ((v.toUInt() and 0xFF00u) shr 8).toUByte()
        private fun lo8(v: UShort) = (v.toUInt() and 0x00FFu).toUByte()
        internal fun add16(v: UShort, w: UShort): Triple<UShort, Boolean, Boolean> {
            val t = v + w
            val overflow = t > UShort.MAX_VALUE
            val halfCarry = ((v.toUInt() and 0x00FFu) + (w.toUInt() and 0x00FFu)) and 0x100u == 0x100u
            return Triple(t.toUShort(), overflow, halfCarry)
        }

        internal fun sub16(v: UShort, w: UShort): Pair<UShort, Boolean> {
            val t = v.toInt() - w.toInt()
            val underflow = t < 0
            return Pair(t.toUShort(), underflow)
        }

        internal fun add8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
            val t = v + w
            val overflow = t > UByte.MAX_VALUE
            val halfCarry = ((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu)) and 0x10u == 0x10u
            return Triple(t.toUByte(), overflow, halfCarry)
        }

        internal fun adc8(v: UByte, w: UByte, c: Boolean): Triple<UByte, Boolean, Boolean> {
            val x = if (c) 1u else 0u
            val t = v + w + x
            val overflow = t > UByte.MAX_VALUE
            val halfCarry = ((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu) + x) and 0x10u == 0x10u
            return Triple(t.toUByte(), overflow, halfCarry)
        }

        internal fun sub8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
            val t = v.toInt() - w.toInt()
            val underflow = t < 0
            val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu)) and 0x10u == 0x10u
            return Triple(t.toUByte(), underflow, halfCarry)
        }

        internal fun sbc8(v: UByte, w: UByte,c: Boolean): Triple<UByte, Boolean, Boolean> {
            val x = if (c) 1u else 0u
            val t = v.toInt() - w.toInt() - x.toInt()
            val underflow = t < 0
            val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu) - x) and 0x10u == 0x10u
            return Triple(t.toUByte(), underflow, halfCarry)
        }
    }

    private val mem = UByteArray(MEMORY_SIZE) { 0u }

    // Registers
    private var A: UByte = 0x00u
    private var F: UByte = 0x00u
    private var z: Boolean
        get() = (F.toUInt() and MASK_Z.toUInt()) > 0u
        set(value) = when (value) {
            true -> F = (F.toUInt() or MASK_Z.toUInt()).toUByte()
            false -> F = (F.toUInt() and MASK_Z.toUInt().inv()).toUByte()
        }
    private var n: Boolean
        get() = (F.toUInt() and MASK_N.toUInt()) > 0u
        set(value) = when (value) {
            true -> F = (F.toUInt() or MASK_N.toUInt()).toUByte()
            false -> F = (F.toUInt() and MASK_N.toUInt().inv()).toUByte()
        }
    private var h: Boolean
        get() = (F.toUInt() and MASK_H.toUInt()) > 0u
        set(value) = when (value) {
            true -> F = (F.toUInt() or MASK_H.toUInt()).toUByte()
            false -> F = (F.toUInt() and MASK_H.toUInt().inv()).toUByte()
        }
    private var c: Boolean
        get() = (F.toUInt() and MASK_C.toUInt()) > 0u
        set(value) = when (value) {
            true -> F = (F.toUInt() or MASK_C.toUInt()).toUByte()
            false -> F = (F.toUInt() and MASK_C.toUInt().inv()).toUByte()
        }

    private var B: UByte = 0x00u
    private var C: UByte = 0x00u
    private var BC: UShort
        get() = u16(B, C)
        set(value) {
            B = hi8(value)
            C = lo8(value)
        }
    private var refBC: UByte
        get() = mem[BC.toInt()]
        set(value) {
            mem[BC.toInt()] = value
        }
    private var D: UByte = 0x00u
    private var E: UByte = 0x00u
    private var DE: UShort
        get() = u16(D, E)
        set(value) {
            D = hi8(value)
            E = lo8(value)
        }
    private var refDE: UByte
        get() = mem[DE.toInt()]
        set(value) {
            mem[DE.toInt()] = value
        }
    private var H: UByte = 0x00u
    private var L: UByte = 0x00u
    private var HL: UShort
        get() = u16(H, L)
        set(value) {
            H = hi8(value)
            L = lo8(value)
        }
    private var refHL: UByte
        get() = mem[HL.toInt()]
        set(value) {
            mem[HL.toInt()] = value
        }
    private var SP: UShort = 0x0000u
    internal var PC: UShort = 0x0000u
    internal fun jumpI8(v: UByte) {
        val i8 = v.toByte()
        if (i8 >= 0) {
            PC = (PC + i8.toUShort()).toUShort()
        } else {
            PC = (PC - (-i8).toUShort()).toUShort()
        }
    }

    init {
        initializeRegisters()
    }

    private fun initializeRegisters() {
        A = 0x01u
        F = 0x00u // FIXME
        B = 0x00u
        C = 0x13u
        D = 0x00u
        E = 0xD8u
        H = 0x01u
        L = 0x4Du
        SP = 0xFFFEu
        PC = 0x0100u
    }

    private fun read(): UByte {
        val b = mem[PC.toInt()]
        PC++
        return b
    }

    /**
     * @return number of cycles
     */
    private fun fetchAndExecuteInstruction(): Int {
        val opcode = read()
//        val instruction = InstructionType.of(opcode)
        val instruction = instructions[opcode.toInt()]
        return instruction.execute()
    }
}

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
