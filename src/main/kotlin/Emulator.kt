import Emulator.CartridgeHeader.CartridgeType.MBC1
import Emulator.CartridgeHeader.CartridgeType.`ROM ONLY`
import org.slf4j.LoggerFactory
import java.io.File

@ExperimentalUnsignedTypes
class Emulator {
    companion object {
        private const val BIT_Z = 7
        private const val BIT_N = 6
        private const val BIT_H = 5
        private const val BIT_C = 4

        private const val MEMORY_BANK_SIZE = 0x4000
    }

    private val log = LoggerFactory.getLogger(javaClass.name)

    private var cycleCounter: Long = 0L

    private inner class Memory {
        /**
         * https://gbdev.io/pandocs/Memory_Map.html
         * 0000-3FFF	16 KiB ROM bank X0
         * 4000-7FFF	16 KiB ROM Bank 01-7F
         * 8000-9FFF	8 KiB Video RAM (VRAM)	In CGB mode, switchable bank 0/1
         * A000-BFFF	8 KiB RAM bank 00-03
         * C000-CFFF	4 KiB Work RAM (WRAM)
         * D000-DFFF	4 KiB Work RAM (WRAM)	In CGB mode, switchable bank 1~7
         * E000-FDFF	Mirror of C000~DDFF (ECHO RAM)	Nintendo says use of this area is prohibited.
         * FE00-FE9F	Sprite attribute table (OAM)
         * FEA0-FEFF	Not Usable	Nintendo says use of this area is prohibited
         * FF00-FF7F	I/O Registers
         * FF80-FFFE	High RAM (HRAM)
         * FFFF     	Interrupt Enable register (IE)
         */

        private val wram0 = UByteArray(0x1000)
        private val wram1 = UByteArray(0x1000)
        private val hram = UByteArray(127)

        // I/O registers
        private var P1: UByte = 0x00u
        private var SB: UByte = 0x00u
        private var SC: UByte = 0x00u
        private var DIV: UByte = 0x00u
        private var TIMA: UByte = 0x00u
        private var TMA: UByte = 0x00u
        private var TAC: UByte = 0x00u
        private var IF: UByte = 0x00u
        private var NR10: UByte = 0x00u
        private var NR11: UByte = 0x00u
        private var NR12: UByte = 0x00u
        private var NR13: UByte = 0x00u
        private var NR14: UByte = 0x00u
        private var NR21: UByte = 0x00u
        private var NR22: UByte = 0x00u
        private var NR23: UByte = 0x00u
        private var NR24: UByte = 0x00u
        private var NR30: UByte = 0x00u
        private var NR31: UByte = 0x00u
        private var NR32: UByte = 0x00u
        private var NR33: UByte = 0x00u
        private var NR34: UByte = 0x00u
        private var NR41: UByte = 0x00u
        private var NR42: UByte = 0x00u
        private var NR43: UByte = 0x00u
        private var NR44: UByte = 0x00u
        private var NR50: UByte = 0x00u
        private var NR51: UByte = 0x00u
        private var NR52: UByte = 0x00u
        private var LCDC: UByte = 0x00u
        private var STAT: UByte = 0x00u
        private var SCY: UByte = 0x00u
        private var SCX: UByte = 0x00u
        private var LY: UByte = 0x00u
        private var LYC: UByte = 0x00u
        private var DMA: UByte = 0x00u
        private var BGP: UByte = 0x00u
        private var OBP0: UByte = 0x00u
        private var OBP1: UByte = 0x00u
        private var WY: UByte = 0x00u
        private var WX: UByte = 0x00u
        private var KEY1: UByte = 0x00u
        private var VBK: UByte = 0x00u
        private var HDMA1: UByte = 0x00u
        private var HDMA2: UByte = 0x00u
        private var HDMA3: UByte = 0x00u
        private var HDMA4: UByte = 0x00u
        private var HDMA5: UByte = 0x00u
        private var RP: UByte = 0x00u
        private var BCPS: UByte = 0x00u
        private var BCPD: UByte = 0x00u
        private var OCPS: UByte = 0x00u
        private var OCPD: UByte = 0x00u
        private var SVBK: UByte = 0x00u
        private var IE: UByte = 0x00u

        private var RAMEnable: UByte = 0x00u
        private var ROMBankNumber: UByte = 0x01u // 5 bits
        private var RAMBankNumber: UByte = 0x01u // 2 bits
        private var bankingMode: UByte = 0x00u // 1 bit

        operator fun get(addr: UShort): UByte = when (addr.toInt()) {
            in 0x0000..0x3FFF -> when (cartridgeHeader!!.cartridgeType) {
                `ROM ONLY` -> rom!![addr.toInt()]
                MBC1 -> when (bankingMode.toInt()) {
                    0 -> rom!![addr.toInt()]
                    1 -> TODO()
                    else -> throw IllegalArgumentException()
                }
                else -> TODO()
            }
            in 0x4000..0x7FFF -> when (cartridgeHeader!!.cartridgeType) {
                `ROM ONLY` -> rom!![addr.toInt()]
                MBC1 -> when (ROMBankNumber.toInt()) {
                    0x00 -> rom!![addr.toInt()]
                    in 0x01..0x1F -> rom!![addr.toInt() + (ROMBankNumber.toInt() - 1) * MEMORY_BANK_SIZE]
                    else -> throw IllegalArgumentException()
                }
                else -> TODO()
            }
            in 0x8000..0x9FFF -> TODO("VRAM")
            in 0xA000..0xBFFF -> TODO("External RAM")
            in 0xC000..0xCFFF -> wram0[addr.toInt() - 0xC000]
            in 0xD000..0xDFFF -> wram1[addr.toInt() - 0xD000]
            in 0xE000..0xFDFF -> get((addr.toUInt() - 0xE000u).toUShort()) // ECHO RAM
            in 0xFE00..0xFE9F -> TODO("OAM")
            in 0xFEA0..0xFEFF -> TODO("Not Usable")
            in 0xFF00..0xFF7F, 0xFFFF -> getIORegisters(addr)
            in 0xFF80..0xFFFE -> hram[addr.toInt() - 0xFF80]
            else -> TODO()
        }

        operator fun set(addr: UShort, value: UByte) = when (addr.toInt()) {
            in 0x0000..0x7FFF -> when (cartridgeHeader!!.cartridgeType) {
                `ROM ONLY` -> throw IllegalArgumentException()
                MBC1 -> when (addr.toInt()) {
                    in 0x0000..0x1FFF -> RAMEnable = (value.toUInt() and 0b00001111u).toUByte()
                    in 0x2000..0x3FFF -> ROMBankNumber = (value.toUInt() and 0b00011111u).toUByte()
                    in 0x4000..0x5FFF -> RAMBankNumber = (value.toUInt() and 0b00000011u).toUByte()
                    in 0x6000..0x7FFF -> bankingMode = (value.toUInt() and 0b00000001u).toUByte()
                    else -> throw IllegalArgumentException()
                }
                else -> TODO()
            }
            in 0x8000..0x9FFF -> TODO("VRAM")
            in 0xA000..0xBFFF -> TODO("External RAM")
            in 0xC000..0xCFFF -> wram0[addr.toInt() - 0xC000] = value
            in 0xD000..0xDFFF -> wram1[addr.toInt() - 0xD000] = value
            in 0xE000..0xFDFF -> wram0[addr.toInt() - 0xE000] = value // ECHO RAM
            in 0xFE00..0xFE9F -> TODO("OAM")
            in 0xFEA0..0xFEFF -> TODO("Not Usable")
            in 0xFF00..0xFF7F, 0xFFFF -> setIORegisters(addr, value)
            in 0xFF80..0xFFFE -> hram[addr.toInt() - 0xFF80] = value // HRAM
            else -> TODO()
        }

        private fun getIORegisters(addr: UShort) = when (addr.toInt()) {
            0xFF00 -> P1
            0xFF01 -> SB
            0xFF02 -> SC
            0xFF04 -> DIV
            0xFF05 -> TIMA
            0xFF06 -> TMA
            0xFF07 -> TAC
            0xFF0F -> IF
            0xFF10 -> NR10
            0xFF11 -> NR11
            0xFF12 -> NR12
            0xFF13 -> NR13
            0xFF14 -> NR14
            0xFF16 -> NR21
            0xFF17 -> NR22
            0xFF18 -> NR23
            0xFF19 -> NR24
            0xFF1A -> NR30
            0xFF1B -> NR31
            0xFF1C -> NR32
            0xFF1D -> NR33
            0xFF1E -> NR34
            0xFF20 -> NR41
            0xFF21 -> NR42
            0xFF22 -> NR43
            0xFF23 -> NR44
            0xFF24 -> NR50
            0xFF25 -> NR51
            0xFF26 -> NR52
            0xFF40 -> LCDC
            0xFF41 -> STAT
            0xFF42 -> SCY
            0xFF43 -> SCX
            0xFF44 -> LY
            0xFF45 -> LYC
            0xFF46 -> DMA
            0xFF47 -> BGP
            0xFF48 -> OBP0
            0xFF49 -> OBP1
            0xFF4A -> WY
            0xFF4B -> WX
            0xFF4D -> KEY1
            0xFF4F -> VBK
            0xFF51 -> HDMA1
            0xFF52 -> HDMA2
            0xFF53 -> HDMA3
            0xFF54 -> HDMA4
            0xFF55 -> HDMA5
            0xFF56 -> RP
            0xFF68 -> BCPS
            0xFF69 -> BCPD
            0xFF6A -> OCPS
            0xFF6B -> OCPD
            0xFF70 -> SVBK
            0xFFFF -> IE
            else -> throw IllegalArgumentException()
        }

        private fun setIORegisters(addr: UShort, value: UByte) = when (addr.toInt()) {
            0xFF00 -> P1 = value
            0xFF01 -> SB = value
            0xFF02 -> SC = value
            0xFF04 -> DIV = value
            0xFF05 -> TIMA = value
            0xFF06 -> TMA = value
            0xFF07 -> TAC = value
            0xFF0F -> IF = value
            0xFF10 -> NR10 = value
            0xFF11 -> NR11 = value
            0xFF12 -> NR12 = value
            0xFF13 -> NR13 = value
            0xFF14 -> NR14 = value
            0xFF16 -> NR21 = value
            0xFF17 -> NR22 = value
            0xFF18 -> NR23 = value
            0xFF19 -> NR24 = value
            0xFF1A -> NR30 = value
            0xFF1B -> NR31 = value
            0xFF1C -> NR32 = value
            0xFF1D -> NR33 = value
            0xFF1E -> NR34 = value
            0xFF20 -> NR41 = value
            0xFF21 -> NR42 = value
            0xFF22 -> NR43 = value
            0xFF23 -> NR44 = value
            0xFF24 -> NR50 = value
            0xFF25 -> NR51 = value
            0xFF26 -> NR52 = value
            0xFF40 -> LCDC = value
            0xFF41 -> STAT = value
            0xFF42 -> SCY = value
            0xFF43 -> SCX = value
            0xFF44 -> LY = value
            0xFF45 -> LYC = value
            0xFF46 -> DMA = value
            0xFF47 -> BGP = value
            0xFF48 -> OBP0 = value
            0xFF49 -> OBP1 = value
            0xFF4A -> WY = value
            0xFF4B -> WX = value
            0xFF4D -> KEY1 = value
            0xFF4F -> VBK = value
            0xFF51 -> HDMA1 = value
            0xFF52 -> HDMA2 = value
            0xFF53 -> HDMA3 = value
            0xFF54 -> HDMA4 = value
            0xFF55 -> HDMA5 = value
            0xFF56 -> RP = value
            0xFF68 -> BCPS = value
            0xFF69 -> BCPD = value
            0xFF6A -> OCPS = value
            0xFF6B -> OCPD = value
            0xFF70 -> SVBK = value
            0xFFFF -> IE = value
            else -> throw IllegalArgumentException()
        }

        fun initialize() {
            wram0.fill(0u)
            wram1.fill(0u)
            hram.fill(0u)
            initializeHardwareRegisters()
            RAMEnable = 0x00u
            ROMBankNumber = 0x01u
            RAMBankNumber = 0x01u
            bankingMode = 0x00u
        }

        private fun initializeHardwareRegisters() {
            P1 = 0xCFu
            SB = 0x00u
            SC = 0x7Eu
            DIV = 0xABu
            TIMA = 0x00u
            TMA = 0x00u
            TAC = 0xF8u
            IF = 0xE1u
            NR10 = 0x80u
            NR11 = 0xBFu
            NR12 = 0xF3u
            NR13 = 0xFFu
            NR14 = 0xBFu
            NR21 = 0x3Fu
            NR22 = 0x00u
            NR23 = 0xFFu
            NR24 = 0xBFu
            NR30 = 0x7Fu
            NR31 = 0xFFu
            NR32 = 0x9Fu
            NR33 = 0xFFu
            NR34 = 0xBFu
            NR41 = 0xFFu
            NR42 = 0x00u
            NR43 = 0x00u
            NR44 = 0xBFu
            NR50 = 0x77u
            NR51 = 0xF3u
            NR52 = 0xF1u
            LCDC = 0x91u
            STAT = 0x85u
            SCY = 0x00u
            SCX = 0x00u
            LY = 0x00u
            LYC = 0x00u
            DMA = 0xFFu
            BGP = 0xFCu
            OBP0 = 0xFFu
            OBP1 = 0xFFu
            WY = 0xFFu
            WX = 0xFFu
            KEY1 = 0xFFu
            VBK = 0xFFu
            HDMA1 = 0xFFu
            HDMA2 = 0xFFu
            HDMA3 = 0xFFu
            HDMA4 = 0xFFu
            HDMA5 = 0xFFu
            RP = 0xFFu
            BCPS = 0xFFu
            BCPD = 0xFFu
            OCPS = 0xFFu
            OCPD = 0xFFu
            SVBK = 0xFFu
            IE = 0x00u
        }
    }

    private val memory = Memory()

    private var rom: UByteArray? = null
    private var cartridgeHeader: CartridgeHeader? = null

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
        get() = memory[BC]
        set(value) {
            memory[BC] = value
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
        get() = memory[DE]
        set(value) {
            memory[DE] = value
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
        get() = memory[HL]
        set(value) {
            memory[HL] = value
        }
    private var SP: UShort = 0x0000u
    private fun pushStack(addr: UShort) {
        log.debug(String.format("\tpushStack=%04X", addr.toInt()))
        SP--
        memory[SP] = hi8(addr)
        SP--
        memory[SP] = lo8(addr)
    }

    private fun popStack(): UShort {
        val lo = memory[SP]
        SP++
        val hi = memory[SP]
        SP++
        return u16(hi, lo).also {
            log.debug(String.format("\tpopStack=%04X", it.toInt()))
        }
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
            Ref8.refNN -> memory[read16()]
            Ref8.refC -> memory[u16(0xFFu, C)]
            Ref8.refN -> memory[u16(0xFFu, read8())]
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
            Ref8.refNN -> memory[read16()] = value
            Ref8.refC -> memory[u16(0xFFu, C)] = value
            Ref8.refN -> memory[u16(0xFFu, read8())] = value
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
        Ref16.AF -> {
            A = hi8(value)
            F = lo8(value)
        }
        Ref16.nn -> throw IllegalArgumentException()
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
        log.debug("ld8\t${dst.name}\t${src.name}")
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
                Ref8.refNN -> 16
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
        log.debug("ldi8\t${dst.name}\t${src.name}")
        ld8(dst, src)
        inc16(Ref16.HL)
        return when {
            (src == Ref8.A && dst == Ref8.refHL) || (src == Ref8.refHL && dst == Ref8.A) -> 8
            else -> throw IllegalArgumentException()
        }
    }

    fun ldd8(dst: Ref8, src: Ref8): Int {
        log.debug("ldd8\t${dst.name}\t${src.name}")
        ld8(dst, src)
        dec16(Ref16.HL)
        return when {
            (src == Ref8.A && dst == Ref8.refHL) || (src == Ref8.refHL && dst == Ref8.A) -> 8
            else -> throw IllegalArgumentException()
        }
    }

    fun ld16(dst: Ref16, src: Ref16): Int {
        log.debug("ld16\t${dst.name}\t${src.name}")
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
            Ref16.HL -> when (dst) {
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
        log.debug("push16\t${ref16.name}")
        val nn = get16(ref16)
        pushStack(nn)
        return 16
    }

    fun pop16(ref16: Ref16): Int {
        log.debug("pop16\t${ref16.name}")
        val sp = popStack()
        set16(ref16, sp)
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
        log.debug("INC8\t${r.name}")
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
        log.debug("dec8\t${r.name}")
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
        log.debug("jp\t${fc.name}\t${addr.toUInt().toString(16)}")
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
        log.debug("ret\t${fc.name}")
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
        rom = null
        cartridgeHeader = null
        initializeRegisters()
        memory.initialize()
        loadROM(romFile)
        while (true) {
            fetchAndExecuteInstruction()

            // TODO divider and timer
        }
    }

    private fun loadROM(romFile: File) {
        rom = romFile.readBytes().asUByteArray()
        log.info("rom.size=${rom!!.size}")
        cartridgeHeader = parseCartridgeHeader()
    }

    data class CartridgeHeader(
        val title: String,
        val cartridgeType: CartridgeType,
        val romSize: ROMSize,
        val ramSize: RAMSize,
        val headerChecksum: UByte
    ) {

        enum class CartridgeType(private val code: UByte) {
            `ROM ONLY`(0x00u),
            `MBC1`(0x01u),
            `MBC1+RAM`(0x02u),
            `MBC1+RAM+BATTERY`(0x03u),
            `MBC2`(0x05u),
            `MBC2+BATTERY`(0x06u),
            `ROM+RAM 1`(0x08u),
            `ROM+RAM+BATTERY 1`(0x09u),
            `MMM01`(0x0Bu),
            `MMM01+RAM`(0x0Cu),
            `MMM01+RAM+BATTERY`(0x0Du),
            `MBC3+TIMER+BATTERY`(0x0Fu),
            `MBC3+TIMER+RAM+BATTERY 2`(0x10u),
            `MBC3`(0x11u),
            `MBC3+RAM 2`(0x12u),
            `MBC3+RAM+BATTERY 2`(0x13u),
            `MBC5`(0x19u),
            `MBC5+RAM`(0x1Au),
            `MBC5+RAM+BATTERY`(0x1Bu),
            `MBC5+RUMBLE`(0x1Cu),
            `MBC5+RUMBLE+RAM`(0x1Du),
            `MBC5+RUMBLE+RAM+BATTERY`(0x1Eu),
            `MBC6`(0x20u),
            `MBC7+SENSOR+RUMBLE+RAM+BATTERY`(0x22u),
            `POCKET CAMERA`(0xFCu),
            `BANDAI TAMA5`(0xFDu),
            `HuC3`(0xFEu),
            `HuC1+RAM+BATTERY`(0xFFu);

            companion object {
                fun of(code: UByte) = values().find { it.code == code }!!
            }
        }

        enum class ROMSize(private val code: UByte, nBanks: Int) {
            `32 KByte`(0x00u, 2),
            `64 KByte`(0x01u, 4),
            `128 KByte`(0x02u, 8),
            `256 KByte`(0x03u, 16),
            `512 KByte`(0x04u, 32),
            `1 MByte`(0x05u, 64),
            `2 MByte`(0x06u, 128),
            `4 MByte`(0x07u, 256),
            `8 MByte`(0x08u, 512),
            `1_1 MByte`(0x52u, 72),
            `1_2 MByte`(0x53u, 80),
            `1_5 MByte`(0x54u, 96);

            companion object {
                fun of(code: UByte) = values().find { it.code == code }!!
            }
        }

        enum class RAMSize(private val code: UByte) {
            `0`(0x00u),
            `-`(0x01u),
            `8 KB`(0x02u),
            `32 KB`(0x03u),
            `128 KB`(0x04u),
            `64 KB`(0x05u);

            companion object {
                fun of(code: UByte) = values().find { it.code == code }!!
            }
        }
    }

    private fun calculateHeaderChecksum(): UByte {
        var x = 0u
        (0x0134..0x014C).forEach {
            x = x - rom!![it].toUInt() - 1u
        }
        return x.toUByte()
    }

    private fun parseCartridgeHeader(): CartridgeHeader {
        val cartridgeHeader = CartridgeHeader(
            (0x0134..0x0143).map {
                rom!![it].toInt().toChar()
            }.joinToString("").trimEnd { it.code == 0 },
            CartridgeHeader.CartridgeType.of(rom!![0x0147]),
            CartridgeHeader.ROMSize.of(rom!![0x0148]),
            CartridgeHeader.RAMSize.of(rom!![0x0149]),
            calculateHeaderChecksum(),
        )
        log.debug(cartridgeHeader!!.toString())
        if (cartridgeHeader!!.headerChecksum != rom!![0x014D]) {
            throw RuntimeException()
        }
        return cartridgeHeader
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

        cycleCounter = 0L
    }

    private fun read16(): UShort {
        val lo = memory[PC]
        PC++
        val hi = memory[PC]
        PC++
        return u16(hi, lo).also {
            log.debug(String.format("\tnn=%04X", it.toInt()))
        }
    }

    private fun read8(): UByte {
        val b = memory[PC]
        PC++
        return b.also {
            log.debug(String.format("\tn=%02X", it.toInt()))
        }
    }

    /**
     * @return number of cycles
     */
    private fun fetchAndExecuteInstruction(): Int {
        val currentPC = PC

        val (instruction, opcode, opcode2) = getInstruction(::read8)
        if (log.isDebugEnabled) {
            val mnemonic = instruction.javaClass.name.split("$")[1]
            log.debug(
                listOf(
                    currentPC.toUInt().toString(16),
                    opcode.toUInt().toString(16),
                    opcode2?.toUInt()?.toString(16) ?: "",
                    mnemonic
                ).joinToString("\t")
            )
            log.debug(
                listOf(
                    "A=${String.format("%02X", A.toInt())}",
                    "Z=${if (flagZ) 1 else 0}",
                    "N=${if (flagN) 1 else 0}",
                    "H=${if (flagH) 1 else 0}",
                    "C=${if (flagC) 1 else 0}",
                    "B=${String.format("%02X", B.toInt())}",
                    "C=${String.format("%02X", C.toInt())}",
                    "D=${String.format("%02X", D.toInt())}",
                    "E=${String.format("%02X", E.toInt())}",
                    "H=${String.format("%02X", H.toInt())}",
                    "L=${String.format("%02X", L.toInt())}",
                    "SP=${String.format("%04X", SP.toInt())}",
                    "PC=${String.format("%04X", PC.toInt())}"
                ).joinToString(" ")
            )
        }
        val cycles = instruction(this)
        cycleCounter += cycles
        return cycles
    }
}

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
