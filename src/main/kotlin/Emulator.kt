import org.slf4j.LoggerFactory
import java.io.File

@ExperimentalUnsignedTypes
class Emulator {
    companion object {
        private const val BIT_Z = 7
        private const val BIT_N = 6
        private const val BIT_H = 5
        private const val BIT_C = 4

        private fun mask(b: Int) = 1u shl b

        /**
         * b must be in 0..7
         */
        private fun bit8(v: UByte, b: Int) = (v.toUInt() shr b) and 1u == 1u
        private fun bit8(v: UByte, b: Int, set: Boolean) = when (set) {
            true -> (v.toUInt() or mask(b)).toUByte()
            false -> (v.toUInt() and mask(b).inv()).toUByte()
        }

        /**
         * b must be in 0..15
         */
        private fun bit16(v: UShort, b: Int) = (v.toUInt() shr b) and 1u == 1u

        private fun u16(hi: UByte, lo: UByte) = (lo.toUInt() or (hi.toUInt() shl 8)).toUShort()
        private fun hi8(v: UShort) = ((v.toUInt() and 0xFF00u) shr 8).toUByte()
        private fun lo8(v: UShort) = (v.toUInt() and 0x00FFu).toUByte()
        internal fun add16(v: UShort, w: UShort): Triple<UShort, Boolean, Boolean> {
            val t = v + w
            val overflow = t > UShort.MAX_VALUE
            val carryFromBit7 = bit16(
                (((v.toUInt() and 0x00FFu) + (w.toUInt() and 0x00FFu)).toUShort()), 8
            )
            return Triple(t.toUShort(), overflow, carryFromBit7)
        }

        internal fun sub16(v: UShort, w: UShort): Pair<UShort, Boolean> {
            val t = v.toInt() - w.toInt()
            val underflow = t < 0
            return Pair(t.toUShort(), underflow)
        }

        internal fun add8(v: UShort, d: Byte): Triple<UShort, Boolean, Boolean> {
            val t = v.toInt() + d
            val carryFromBit3 =
                bit16((((v.toUInt() and 0x000Fu) + (d.toUInt() and 0x000Fu)).toUShort()), 4)
            val carryFromBit7 =
                bit16((((v.toUInt() and 0x00FFu) + (d.toUInt() and 0x00FFu)).toUShort()), 8)
            return Triple(t.toUShort(), carryFromBit7, carryFromBit3)
        }

        internal fun add8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
            val t = v + w
            val overflow = t > UByte.MAX_VALUE
            val carryFromBit3 =
                bit8((((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu)).toUByte()), 4)
            return Triple(t.toUByte(), overflow, carryFromBit3)
        }

        internal fun adc8(v: UByte, w: UByte, c: Boolean): Triple<UByte, Boolean, Boolean> {
            val x = if (c) 1u else 0u
            val t = v + w + x
            val overflow = t > UByte.MAX_VALUE
            val carryFromBit3 =
                bit8((((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu) + x).toUByte()), 4)
            return Triple(t.toUByte(), overflow, carryFromBit3)
        }

        internal fun sub8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
            val t = v.toInt() - w.toInt()
            val underflow = t < 0
            val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu)) and 0x10u == 0x10u
            return Triple(t.toUByte(), underflow, halfCarry)
        }

        internal fun sbc8(v: UByte, w: UByte, c: Boolean): Triple<UByte, Boolean, Boolean> {
            val x = if (c) 1u else 0u
            val t = v.toInt() - w.toInt() - x.toInt()
            val underflow = t < 0
            val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu) - x) and 0x10u == 0x10u
            return Triple(t.toUByte(), underflow, halfCarry)
        }

        private fun rlc8(v: UByte): Pair<UByte, Boolean> {
            val oldBit7 = bit8(v, 7)
            val result = ((v.toUInt() shl 1) or (if (oldBit7) mask(0) else 0u)).toUByte()
            return Pair(result, oldBit7)
        }

        private fun rl8(v: UByte, c: Boolean): Pair<UByte, Boolean> {
            val oldBit7 = bit8(v, 7)
            val result = ((v.toUInt() shl 1) or (if (c) mask(0) else 0u)).toUByte()
            return Pair(result, oldBit7)
        }

        private fun rrc8(v: UByte): Pair<UByte, Boolean> {
            val oldBit0 = bit8(v, 0)
            val result = ((v.toUInt() shr 1) or (if (oldBit0) mask(7) else 0u)).toUByte()
            return Pair(result, oldBit0)
        }

        private fun rr8(v: UByte, c: Boolean): Pair<UByte, Boolean> {
            val oldBit0 = bit8(v, 0)
            val result = ((v.toUInt() shr 1) or (if (c) mask(7) else 0u)).toUByte()
            return Pair(result, oldBit0)
        }

    }

    private val log = LoggerFactory.getLogger(javaClass.name)

    private inner class Memory {
        operator fun get(addr: UShort): UByte = when (addr) {
            in 0x0000u..0x7FFFu -> rom!![addr.toInt()]
            else -> TODO()
        }

        operator fun set(addr: UShort, value: UByte) {
            TODO()
        }
    }

    private val mem = Memory()

    private var rom: UByteArray? = null

    // Registers
    private var A: UByte = 0x00u
    private var F: UByte = 0x00u
    private val AF: UShort
        get() = u16(A, F)
    private var flagZ: Boolean
        get() = bit8(F, BIT_Z)
        set(value) {
            F = bit8(F, BIT_Z, value)
        }
    private var flagN: Boolean
        get() = bit8(F, BIT_N)
        set(value) {
            F = bit8(F, BIT_N, value)
        }
    private var flagH: Boolean
        get() = bit8(F, BIT_H)
        set(value) {
            F = bit8(F, BIT_H, value)
        }
    private var flagC: Boolean
        get() = bit8(F, BIT_C)
        set(value) {
            F = bit8(F, BIT_C, value)
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
        get() = mem[BC]
        set(value) {
            mem[BC] = value
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
        get() = mem[DE]
        set(value) {
            mem[DE] = value
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
        get() = mem[HL]
        set(value) {
            mem[HL] = value
        }
    private var SP: UShort = 0x0000u
    private fun pushStack(addr: UShort) {
        SP--
        mem[SP] = hi8(addr)
        SP--
        mem[SP] = lo8(addr)
    }

    private fun popStack(): UShort {
        val lo = mem[SP]
        SP++
        val hi = mem[SP]
        SP++
        return u16(hi, lo)
    }

    internal var PC: UShort = 0x0000u
    internal fun jumpI8(v: UByte) {
        val i8 = v.toByte()
        if (i8 >= 0) {
            PC = (PC + i8.toUShort()).toUShort()
        } else {
            PC = (PC - (-i8).toUShort()).toUShort()
        }
    }

    private fun get8(ref8: Ref8): UByte {
        return when (ref8) {
            Ref8.B -> B
            Ref8.C -> C
            Ref8.D -> D
            Ref8.E -> E
            Ref8.H -> H
            Ref8.L -> L
            Ref8.A -> A
            Ref8.refHL -> refHL
            Ref8.refBC -> refBC
            Ref8.refDE -> refDE
            Ref8.n -> read8()
            Ref8.refNN -> mem[read16()]
            Ref8.refC -> mem[u16(0xFFu, C)]
            Ref8.refN -> mem[u16(0xFFu, read8())]
        }
    }

    private fun set8(ref8: Ref8, value: UByte) {
        when (ref8) {
            Ref8.B -> B = value
            Ref8.C -> C = value
            Ref8.D -> D = value
            Ref8.E -> E = value
            Ref8.H -> H = value
            Ref8.L -> L = value
            Ref8.A -> A = value
            Ref8.refHL -> refHL = value
            Ref8.refBC -> refBC = value
            Ref8.refDE -> refDE = value
            Ref8.n -> throw IllegalArgumentException()
            Ref8.refNN -> mem[read16()] = value
            Ref8.refC -> mem[u16(0xFFu, C)] = value
            Ref8.refN -> mem[u16(0xFFu, read8())] = value
        }
    }

    private fun get16(ref16: Ref16) = when (ref16) {
        Ref16.BC -> BC
        Ref16.DE -> DE
        Ref16.HL -> HL
        Ref16.SP -> SP
        Ref16.AF -> AF
        Ref16.nn -> read16()
    }

    private fun set16(ref16: Ref16, value: UShort) = when (ref16) {
        Ref16.BC -> BC = value
        Ref16.DE -> DE = value
        Ref16.HL -> HL = value
        Ref16.SP -> SP = value
        Ref16.AF, Ref16.nn -> throw IllegalArgumentException()
    }

    private fun add16(ref16: Ref16, v: UShort): Pair<Boolean, Boolean> {
        val (result, overflow, carryFromBit7) = add16(get16(ref16), v)
        set16(ref16, result)
        return Pair(overflow, carryFromBit7)
    }

    private fun sub16(ref16: Ref16, v: UShort): Boolean {
        val (result, underflow) = sub16(get16(ref16), v)
        set16(ref16, result)
        return underflow
    }

    private fun add8(ref8: Ref8, v: UByte): Pair<Boolean, Boolean> {
        val (result, overflow, carryFromBit3) = add8(get8(ref8), v)
        set8(ref8, result)
        return Pair(overflow, carryFromBit3)
    }

    private fun adc8(ref8: Ref8, v: UByte): Pair<Boolean, Boolean> {
        val (result, overflow, carryFromBit3) = adc8(get8(ref8), v, flagC)
        set8(ref8, result)
        return Pair(overflow, carryFromBit3)
    }

    private fun sub8(ref8: Ref8, v: UByte): Pair<Boolean, Boolean> {
        val (result, underflow, halfCarry) = sub8(get8(ref8), v)
        set8(ref8, result)
        return Pair(underflow, halfCarry)
    }

    private fun sbc8(ref8: Ref8, v: UByte): Pair<Boolean, Boolean> {
        val (result, underflow, halfCarry) = sbc8(get8(ref8), v, flagC)
        set8(ref8, result)
        return Pair(underflow, halfCarry)
    }

    private fun evaluate(fc: FlagCondition) = when (fc) {
        FlagCondition.FC_NONE -> true
        FlagCondition.FC_NZ -> !flagZ
        FlagCondition.FC_Z -> flagZ
        FlagCondition.FC_NC -> !flagC
        FlagCondition.FC_C -> flagC
    }

    // Instructions
    fun ld8(dst: Ref8, src: Ref8): Int {
        set8(dst, get8(src))
        return when (src) {
            Ref8.refBC, Ref8.refDE, Ref8.refHL -> when (dst) {
                Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 8
                else -> throw IllegalArgumentException()
            }
            Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L -> when (dst) {
                Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 4
                Ref8.refHL, Ref8.refBC, Ref8.refDE -> 8
                else -> throw IllegalArgumentException()
            }
            Ref8.A -> when (dst) {
                Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 4
                Ref8.refHL, Ref8.refBC, Ref8.refDE, Ref8.refC -> 8
                Ref8.refN -> 12
                else -> throw IllegalArgumentException()
            }
            Ref8.n -> when (dst) {
                Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 8
                Ref8.refHL -> 12
                else -> throw IllegalArgumentException()
            }
            Ref8.refNN -> when (dst) {
                Ref8.A -> 16
                else -> throw IllegalArgumentException()
            }
            Ref8.refC -> when (dst) {
                Ref8.A -> 8
                else -> throw IllegalArgumentException()
            }
            Ref8.refN -> when (dst) {
                Ref8.A -> 12
                else -> throw IllegalArgumentException()
            }
        }
    }

    fun ldi8(dst: Ref8, src: Ref8): Int {
        ld8(dst, src)
        inc16(Ref16.HL)
        return when {
            (src == Ref8.A && dst == Ref8.refHL) || (src == Ref8.refHL && dst == Ref8.A) -> 8
            else -> throw IllegalArgumentException()
        }
    }

    fun ldd8(dst: Ref8, src: Ref8): Int {
        ld8(dst, src)
        dec16(Ref16.HL)
        return when {
            (src == Ref8.A && dst == Ref8.refHL) || (src == Ref8.refHL && dst == Ref8.A) -> 8
            else -> throw IllegalArgumentException()
        }
    }

    fun ld16(dst: Ref16, src: Ref16): Int {
        set16(dst, get16(src))
        return when (src) {
            Ref16.SP -> when (dst) {
                Ref16.nn -> 20
                else -> throw IllegalArgumentException()
            }
            Ref16.nn -> when (dst) {
                Ref16.BC, Ref16.DE, Ref16.HL, Ref16.SP -> 12
                Ref16.AF, Ref16.nn -> throw IllegalArgumentException()
            }
            Ref16.BC, Ref16.DE, Ref16.AF -> throw IllegalArgumentException()
            Ref16.HL -> when(dst) {
                Ref16.SP -> 8
                else -> throw IllegalArgumentException()
            }
        }
    }

    // 00hc
    fun ldHL16(): Int {
        val d = read8().toByte()
        val (res, carryFromBit7, carryFromBit3) = add8(SP, d)
        HL = res
        flagZ = false
        flagN = false
        flagH = carryFromBit3
        flagC = carryFromBit7
        return 12
    }

    fun push16(ref16: Ref16): Int {
        val addr = SP
        val nn = get16(ref16)
        mem[addr] = lo8(nn)
        mem[add16(addr, 1u).first] = hi8(nn)
        sub16(Ref16.SP, 2u)
        return 16
    }

    fun pop16(ref16: Ref16): Int {
        set16(ref16, SP)
        add16(Ref16.SP, 2u)
        return 16
    }

    // z037
    fun addA8(r: Ref8): Int {
        val (overflow, halfCarry) = add8(Ref8.A, get8(r))
        flagZ = A.equals(0u)
        flagN = false
        flagH = halfCarry
        flagC = overflow
        return nCycleForBinaryOps(r)
    }

    // z037
    fun adcA8(r: Ref8): Int {
        val (overflow, halfCarry) = adc8(Ref8.A, get8(r))
        flagZ = A.equals(0u)
        flagN = false
        flagH = halfCarry
        flagC = overflow
        return nCycleForBinaryOps(r)
    }

    // z137
    fun subA8(r: Ref8): Int {
        val (underflow, halfCarry) = sub8(Ref8.A, get8(r))
        flagZ = A.equals(0u)
        flagN = true
        flagH = halfCarry
        flagC = underflow
        return nCycleForBinaryOps(r)
    }

    // z137
    fun sbcA8(r: Ref8): Int {
        val (underflow, halfCarry) = sbc8(Ref8.A, get8(r))
        flagZ = A.equals(0u)
        flagN = true
        flagH = halfCarry
        flagC = underflow
        return nCycleForBinaryOps(r)
    }

    // z010
    fun andA8(r: Ref8): Int {
        A = (A.toUInt() and get8(r).toUInt()).toUByte()
        flagZ = A.equals(0u)
        flagN = false
        flagH = true
        flagC = false
        return nCycleForBinaryOps(r)
    }

    // z000
    fun orA8(r: Ref8): Int {
        A = (A.toUInt() or get8(r).toUInt()).toUByte()
        flagZ = A.equals(0u)
        flagN = false
        flagH = false
        flagC = false
        return nCycleForBinaryOps(r)
    }

    // z000
    fun xorA8(r: Ref8): Int {
        A = (A.toUInt() xor get8(r).toUInt()).toUByte()
        flagZ = A.equals(0u)
        flagN = false
        flagH = false
        flagC = false
        return nCycleForBinaryOps(r)
    }

    // z137
    fun cpA8(r: Ref8): Int {
        val (result, underflow, halfCarry) = sub8(A, get8(r))
        flagZ = result.equals(0u)
        flagN = true
        flagH = halfCarry
        flagC = underflow
        return nCycleForBinaryOps(r)
    }

    // z03-
    fun inc8(r: Ref8): Int {
        val (res, _, halfCarry) = add8(get8(r), 1u)
        set8(r, res)
        flagZ = res.equals(0u)
        flagN = false
        flagH = halfCarry
        return when (r) {
            Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 4
            Ref8.refHL -> 12
            else -> throw IllegalArgumentException()
        }
    }

    // z13-
    fun dec8(r: Ref8): Int {
        val (res, _, halfCarry) = sub8(get8(r), 1u)
        set8(r, res)
        flagZ = res.equals(0u)
        flagN = true
        flagH = halfCarry
        return when (r) {
            Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 4
            Ref8.refHL -> 12
            else -> throw IllegalArgumentException()
        }
    }

    // -0bf
    fun addHL16(rr: Ref16): Int {
        val addend = get16(rr)
        val (res, overflow, _) = add16(
            HL,
            addend
        )
        val carryFromBit11 =
            bit16(((HL.toUInt() and 0x0FFFu) + (addend.toUInt() and 0x0FFFu)).toUShort(), 12)
        HL = res
        flagN = false
        flagH = carryFromBit11
        flagC = overflow
        return 8
    }

    // 0037
    fun addSP(): Int {
        val d = read8().toByte()
        val (res, carryFromBit7, carryFromBit3) = add8(SP, d)
        SP = res
        flagZ = false
        flagN = false
        flagH = carryFromBit3
        flagC = carryFromBit7
        return 16
    }

    fun inc16(rr: Ref16): Int {
        set16(rr, add16(get16(rr), 1u).first)
        return 8
    }

    fun dec16(rr: Ref16): Int {
        set16(rr, sub16(get16(rr), 1u).first)
        return 8
    }

    // z000
    fun swap(r: Ref8): Int {
        val v = get8(r).toUInt()
        val result = (((v and 0xF0u) shr 8) or ((v and 0x0Fu) shl 8)).toUByte()
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = false
        return when (r) {
            Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 8
            Ref8.refHL -> 16
            else -> throw IllegalArgumentException()
        }
    }

    // z-0c
    fun daa(): Int {
        var v = A.toUInt()
        if (flagH || (v and 0b00001111u) > 9u) {
            v = if (flagN) v.minus(0x06u) else v.plus(0x06u)
        }
        if (flagC || (v and 0b11110000u) shr 4 > 9u) {
            v = if (flagN) v.minus(0x60u) else v.plus(0x60u)
        }
        val overflow = v > UByte.MAX_VALUE
        A = v.toUByte()

        flagZ = A.equals(0u)
        flagH = false
        flagC = overflow
        return 4
    }

    // -11-
    fun cpl(): Int {
        A = A.toUInt().inv().toUByte()
        flagH = true
        flagC = true
        return 4
    }

    // -00c
    fun ccf(): Int {
        flagC = !flagC
        flagH = false
        flagC = false
        return 4
    }

    // -001
    fun scf(): Int {
        flagC = true
        flagH = false
        flagC = false
        return 4
    }

    fun nop() = 4
    fun halt(): Int {
        // TODO halt until interrupt occurs (low power)
        return 4
    }

    fun stop(): Int {
        // TODO stop
        return 4
    }

    fun di(): Int {
        // TODO disable interrupts
        return 4
    }

    fun ei(): Int {
        // TODO enable interrupts
        return 4
    }

    // z00c
    fun rlca(): Int {
        val (result, oldBit7) = rlc8(A)
        A = result
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit7
        return 4
    }

    // z00c
    fun rla(): Int {
        val (result, oldBit7) = rl8(A, flagC)
        A = result
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit7
        return 4
    }

    // z00c
    fun rrca(): Int {
        val (result, oldBit0) = rrc8(A)
        A = result
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return 4
    }

    // z00c
    fun rra(): Int {
        val (result, oldBit0) = rr8(A, flagC)
        A = result
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return 4
    }

    private fun nCycleForBinaryOps(r: Ref8) = when (r) {
        Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 4
        Ref8.refHL -> 8
        Ref8.n -> 8
        else -> throw IllegalArgumentException()
    }

    private fun nCycleForCBOps(r: Ref8) = when (r) {
        Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 8
        Ref8.refHL -> 16
        else -> throw IllegalArgumentException()
    }

    private fun nCycleForCBBitOps(r: Ref8) = when (r) {
        Ref8.B, Ref8.C, Ref8.D, Ref8.E, Ref8.H, Ref8.L, Ref8.A -> 8
        Ref8.refHL -> 12
        else -> throw IllegalArgumentException()
    }

    // z00c
    fun rlc(r: Ref8): Int {
        val (result, oldBit7) = rlc8(get8(r))
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit7
        return nCycleForCBOps(r)
    }

    // z00c
    fun rl(r: Ref8): Int {
        val (result, oldBit7) = rl8(get8(r), flagC)
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit7
        return nCycleForCBOps(r)
    }

    // z00c
    fun rrc(r: Ref8): Int {
        val (result, oldBit0) = rrc8(get8(r))
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return nCycleForCBOps(r)
    }

    // z00c
    fun rr(r: Ref8): Int {
        val (result, oldBit0) = rr8(get8(r), flagC)
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return nCycleForCBOps(r)
    }

    // z00c
    fun sla(r: Ref8): Int {
        val v = get8(r)
        val oldBit7 = bit8(v, 7)
        val result = (v.toUInt() shl 1).toUByte()
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit7
        return nCycleForCBOps(r)
    }

    // z00c
    fun sra(r: Ref8): Int {
        val v = get8(r)
        val oldBit0 = bit8(v, 0)
        val oldBit7 = bit8(v, 7)
        val result = bit8((v.toUInt() shr 1).toUByte(), 7, oldBit7)
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return nCycleForCBOps(r)
    }

    // z00c
    fun srl(r: Ref8): Int {
        val v = get8(r)
        val oldBit0 = bit8(v, 0)
        val result = (v.toUInt() shr 1).toUByte()
        set8(r, result)
        flagZ = result.equals(0u)
        flagN = false
        flagH = false
        flagC = oldBit0
        return nCycleForCBOps(r)
    }

    // z01-
    fun bit(r: Ref8, b: Int): Int {
        val bit = bit8(get8(r), b)
        flagZ = !bit
        flagN = false
        flagH = true
        return nCycleForCBBitOps(r)
    }

    // ----
    fun set(r: Ref8, b: Int): Int {
        set8(r, bit8(get8(r), b, true))
        return nCycleForCBOps(r)
    }

    // ----
    fun res(r: Ref8, b: Int): Int {
        set8(r, bit8(get8(r), b, false))
        return nCycleForCBOps(r)
    }

    fun jp(fc: FlagCondition): Int {
        val addr = read16()
        if (evaluate(fc)) {
            PC = addr
            return 16
        } else {
            return 12
        }
    }

    fun jpHL(): Int {
        PC = HL
        return 4
    }

    fun jr(fc: FlagCondition): Int {
        val u8 = read8()
        if (evaluate(fc)) {
            jumpI8(u8)
            return 12
        } else {
            return 8
        }
    }

    fun call(fc: FlagCondition): Int {
        val addr = read16()
        if (evaluate(fc)) {
            pushStack(PC)
            PC = addr
            return 24
        } else {
            return 12
        }
    }

    fun rst(addr: UShort): Int {
        pushStack(PC)
        PC = addr
        return 32
    }

    fun ret(fc: FlagCondition): Int {
        if (evaluate(fc)) {
            PC = popStack()
            return 20
        } else {
            return 8
        }
    }

    fun reti(): Int {
        PC = popStack()
        // TODO enable interrupts
        return 16
    }

    fun start(romFile: File) {
        initializeRegisters()
        // initialize memory
        loadROM(romFile)
        fetchAndExecuteInstruction()
    }

    private fun loadROM(romFile: File) {
        rom = romFile.readBytes().asUByteArray()
        log.info("rom.size=${rom!!.size}")
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

    private fun read16(): UShort {
        val lo = read8()
        val hi = read8()
        return u16(hi, lo)
    }

    private fun read8(): UByte {
        val b = mem[PC]
        PC++
        return b
    }

    /**
     * @return number of cycles
     */
    private fun fetchAndExecuteInstruction(): Int {
        val instruction = getInstruction(::read8)
        return instruction(this)
    }
}
