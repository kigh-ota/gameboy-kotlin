import BinaryOperation.*
import FlagCondition.*
import Ref16.*
import Ref8.*

abstract class Instruction {
    abstract fun execute(): Int
    protected fun read(): UByte {
        TODO()
    }
}

enum class BinaryOperation {
    ADD, ADC, SUB, SBC, AND, XOR, OR, CP,
}

enum class FlagCondition {
    FC_NONE, FC_NZ, FC_Z, FC_NC, FC_C;

    fun evaluate(): Boolean = TODO()
}

enum class Ref16 {
    BC, DE, HL, SP, AF, nn;

    fun get(): UShort = TODO()
    fun set(value: UShort): Unit = TODO()
}

enum class Ref8 {
    B, C, D, E, H, L, A, refHL, refBC, refDE, n;

    fun get(): UByte = TODO()
    fun set(value: UByte): Unit = TODO()
}

enum class Flags {
    Z, N, H, C;

    fun get(): Boolean = TODO()
    fun set(value: Boolean): Unit = TODO()
}


class NOP : Instruction() {
    override fun execute(): Int = 4
}

class LD16(private val dst: Ref16, private val src: Ref16) : Instruction() {
    override fun execute(): Int {
        dst.set(src.get())
        return when (src) {
            BC -> TODO()
            DE -> TODO()
            HL -> TODO()
            SP -> when(dst) {
                nn -> 20
                else -> TODO()
            }
            AF -> TODO()
            nn -> when (dst) {
                BC, DE, HL, SP -> 12
                AF -> TODO()
                nn -> TODO()
            }
        }
    }
}

class LD8(private val dst: Ref8, private val src: Ref8) : Instruction() {
    override fun execute(): Int {
        dst.set(src.get())
        return when (src) {
            refBC, refDE, refHL -> when (dst) {
                B, C, D, E, H, L, A -> 8
                refHL -> TODO()
                refBC -> TODO()
                refDE -> TODO()
                n -> TODO()
            }
            B, C, D, E, H, L, A -> when (dst) {
                B, C, D, E, H, L, A -> 4
                refHL, refBC, refDE -> 8
                n -> TODO()
            }
            n -> when (dst) {
                B, C, D, E, H, L, A -> 8
                refHL -> 12
                refBC -> TODO()
                refDE -> TODO()
                n -> TODO()
            }
        }
    }
}

class LDI8(private val dst: Ref8, private val src: Ref8) : Instruction() {
    override fun execute(): Int {
        when (src) {
            A -> when (dst) {
                refHL -> {
                    refHL.set(A.get())
                    HL.set(add16(HL.get(), 1u).first)
                    return 8
                }
                else -> TODO()
            }
            refHL -> when(dst)  {
                A -> {
                    A.set(refHL.get())
                    HL.set(add16(HL.get(), 1u).first)
                    return 8
                }
                else -> TODO()
            }
            else -> TODO()
        }
    }
}

class LDD8(private val dst: Ref8, private val src: Ref8) : Instruction() {
    override fun execute(): Int {
        when (src) {
            A -> when (dst) {
                refHL -> {
                    refHL.set(A.get())
                    HL.set(sub16(HL.get(), 1u).first)
                    return 8
                }
                else -> TODO()
            }
            refHL -> when(dst)  {
                A -> {
                    A.set(refHL.get())
                    HL.set(sub16(HL.get(), 1u).first)
                    return 8
                }
                else -> TODO()
            }
            else -> TODO()
        }
    }
}

class INC16(private val rr: Ref16) : Instruction() {
    override fun execute(): Int {
        when(rr) {
            BC,DE,HL,SP -> {
                rr.set(add16(rr.get(), 1u).first)
                return 8
            }
            AF -> TODO()
            nn -> TODO()
        }
    }
}

class DEC16(private val rr: Ref16) : Instruction() {
    override fun execute(): Int {
        when(rr) {
            BC,DE,HL,SP -> {
                rr.set(sub16(rr.get(), 1u).first)
                return 8
            }
            AF -> TODO()
            nn -> TODO()
        }
    }
}

class INC8(private val r: Ref8) : Instruction() {
    override fun execute(): Int {
        when(r) {
            B, C, D, E, H, L, A, refHL -> {
                val (res, _, halfCarry) = add8(r.get(), 1u)
                r.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(false)
                Flags.H.set(halfCarry)
                return if (r == refHL) 12 else 4
            }
            refBC -> TODO()
            refDE -> TODO()
            n -> TODO()
        }
    }
}

class DEC8(private val r: Ref8) : Instruction() {
    override fun execute(): Int {
        when(r) {
            B, C, D, E, H, L, A, refHL -> {
                val (res, _, halfCarry) = sub8(r.get(), 1u)
                r.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(true)
                Flags.H.set(halfCarry)
                return if (r == refHL) 12 else 4
            }
            refBC -> TODO()
            refDE -> TODO()
            n -> TODO()
        }
    }
}

class LD_r_n(private val dst: Ref8) : Instruction() {
    override fun execute(): Int = TODO()
}

// (1,4t;000C)
class RLCA() : Instruction() {
    override fun execute(): Int = TODO()
}
class RRCA() : Instruction() {
    override fun execute(): Int = TODO()
}
class RLA() : Instruction() {
    override fun execute(): Int = TODO()
}
class RRA() : Instruction() {
    override fun execute(): Int = TODO()
}

// -0HC
class ADD_HL_rr(private val rr: Ref16) : Instruction() {
    override fun execute(): Int {
        when (rr) {
            BC, DE, HL, SP -> {
                val (res, overflow, halfCarry) = add16(HL.get(), rr.get())
                HL.set(res)
                Flags.N.set(false)
                Flags.H.set(halfCarry)
                Flags.C.set(overflow)
                return 8
            }
            AF -> TODO()
            nn -> TODO()
        }
    }
}

class STOP() : Instruction() {
    override fun execute(): Int {
        // TODO
        return 4
    }
}

class JR_i8(private val fc: FlagCondition) : Instruction() {
    override fun execute(): Int {
        val u8 = read()
        if (fc.evaluate()) {
//            jumpI8(u8) TODO
            return 12
        } else {
            return 8
        }
    }
}

class DAA() : Instruction() {
    override fun execute(): Int = TODO()
}

class CPL() : Instruction() {
    override fun execute(): Int = TODO()
}

class SCF() : Instruction() {
    override fun execute(): Int = TODO()
}

class CCF() : Instruction() {
    override fun execute(): Int = TODO()
}

class HALT() : Instruction() {
    override fun execute(): Int {
        // TODO halt until interrupt occurs (low power)
        return 4
    }
}

class PUSH_rr(private val rr: Ref16) : Instruction() {
    override fun execute(): Int = TODO()
}

class POP_rr(private val rr: Ref16) : Instruction() {
    override fun execute(): Int = TODO()
}

class BINOP8_A(private val op: BinaryOperation, private val r: Ref8) : Instruction() {
    override fun execute(): Int {
        when (op) {
            // Z0HC
            ADD -> {
                val (res, overflow, halfCarry) = add8(A.get(), r.get())
                A.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(false)
                Flags.H.set(halfCarry)
                Flags.C.set(overflow)
            }
            ADC -> {
                val (res, overflow, halfCarry) = adc8(A.get(), r.get(), Flags.C.get())
                A.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(false)
                Flags.H.set(halfCarry)
                Flags.C.set(overflow)
            }
            // Z1HC
            SUB -> {
                val (res, underflow, halfCarry) = sub8(A.get(), r.get())
                A.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(true)
                Flags.H.set(halfCarry)
                Flags.C.set(underflow)
            }
            SBC -> {
                val (res, underflow, halfCarry) = sbc8(A.get(), r.get(), Flags.C.get())
                A.set(res)
                Flags.Z.set(res.equals(0u))
                Flags.N.set(true)
                Flags.H.set(halfCarry)
                Flags.C.set(underflow)
            }
            // Z010
            AND -> TODO()
            // Z000
            XOR -> TODO()
            // Z000
            OR -> TODO()
            // Z1HC
            CP -> TODO()
        }
        return when (r) {
            B, C, D, E, H, L, A -> 4
            refHL -> 8
            n -> 8
            refBC -> TODO()
            refDE -> TODO()
        }
    }
}

class RST(private val hex: Int) : Instruction() {
    override fun execute(): Int = TODO()
}

class RETI() : Instruction() {
    override fun execute(): Int = TODO()
}

class JP_u16(private val fc: FlagCondition) : Instruction() {
    override fun execute(): Int = TODO()
}

class CALL_u16(private val fc: FlagCondition) : Instruction() {
    override fun execute(): Int = TODO()
}

class RET(private val fc: FlagCondition) : Instruction() {
    override fun execute(): Int = TODO()
}

class LD_FF00pu8_A() : Instruction() {
    override fun execute(): Int = TODO()
}

class LD_FF00pC_A() : Instruction() {
    override fun execute(): Int = TODO()
}

class ADD_SP_i8() : Instruction() {
    override fun execute(): Int = TODO()
}

class JP_HL() : Instruction() {
    override fun execute(): Int = TODO()
}

class LD_u16_A() : Instruction() {
    override fun execute(): Int = TODO()
}

class LD_A_FF00pu8() : Instruction() {
    override fun execute(): Int = TODO()
}

class LD_A_FF00pC() : Instruction() {
    override fun execute(): Int = TODO()
}

class DI() : Instruction() {
    override fun execute(): Int = TODO()

}

class EI() : Instruction() {
    override fun execute(): Int = TODO()

}

class LD_HL_SPpi8() : Instruction() {
    override fun execute(): Int = TODO()

}

class LD_SP_HL() : Instruction() {
    override fun execute(): Int = TODO()

}

class LD_A_u16() : Instruction() {
    override fun execute(): Int = TODO()
}


class PREFIX_CB() : Instruction() {
    override fun execute(): Int = TODO()
}

class NULL(private val opcode: Int) : Instruction() {
    override fun execute(): Int = TODO()
}

val instructions = arrayOf(
    // @formatter:off
    /* 00+ */ NOP(), LD16(BC, nn), LD8(refBC, A), INC16(BC), INC8(B), DEC8(B), LD8(B,n), RLCA(),
    /* 08+ */ LD16(nn,SP), ADD_HL_rr(BC), LD8(A, refBC), DEC16(BC), INC8(C), DEC8(C), LD8(C,n), RRCA(),
    /* 10+ */ STOP(), LD16(DE, nn), LD8(refDE, A), INC16(DE),INC8(D), DEC8(D), LD8(D,n), RLA(),
    /* 18+ */ JR_i8(FC_NONE), ADD_HL_rr(DE), LD8(A, refDE), DEC16(DE), INC8(E), DEC8(E), LD_r_n(E), RRA(),
    /* 20+ */ JR_i8(FC_NZ), LD16(HL, nn), LDI8(refHL,A), INC16(HL), INC8(H), DEC8(H), LD8(H,n), DAA(),
    /* 28+ */ JR_i8(FC_Z), ADD_HL_rr(HL), LDI8(A,refHL), DEC16(HL), INC8(L), DEC8(L), LD8(L,n), CPL(),
    /* 30+ */ JR_i8(FC_NC), LD16(SP, nn), LDD8(refHL,A), INC16(SP), INC8(refHL), DEC8(refHL), LD8(refHL,n), SCF(),
    /* 38+ */ JR_i8(FC_C), ADD_HL_rr(SP), LDD8(A,refHL), DEC16(SP), INC8(A), DEC8(A), LD8(A,n), CCF(),
    /* 40+ */ LD8(B,B), LD8(B,C), LD8(B,D), LD8(B,E), LD8(B,H), LD8(B,L), LD8(B,refHL), LD8(B,A),
    /* 48+ */ LD8(C,B), LD8(C,C), LD8(C,D), LD8(C,E), LD8(C,H), LD8(C,L), LD8(C,refHL), LD8(C,A),
    /* 50+ */ LD8(D,B), LD8(D,C), LD8(D,D), LD8(D,E), LD8(D,H), LD8(D,L), LD8(D,refHL), LD8(D,A),
    /* 58+ */ LD8(E,B), LD8(E,C), LD8(E,D), LD8(E,E), LD8(E,H), LD8(E,L), LD8(E,refHL), LD8(E,A),
    /* 60+ */ LD8(H,B), LD8(H,C), LD8(H,D), LD8(H,E), LD8(H,H), LD8(H,L), LD8(H,refHL), LD8(H,A),
    /* 68+ */ LD8(L,B), LD8(L,C), LD8(L,D), LD8(L,E), LD8(L,H), LD8(L,L), LD8(L,refHL), LD8(L,A),
    /* 70+ */ LD8(refHL,B), LD8(refHL,C), LD8(refHL,D), LD8(refHL,E), LD8(refHL,H), LD8(refHL,L), HALT(), LD8(refHL,A),
    /* 78+ */ LD8(A,B), LD8(A,C), LD8(A,D), LD8(A,E), LD8(A,H), LD8(A,L), LD8(A,refHL), LD8(A,A),
    /* 80+ */ BINOP8_A(ADD,B), BINOP8_A(ADD,C), BINOP8_A(ADD,D), BINOP8_A(ADD,E), BINOP8_A(ADD, H), BINOP8_A(ADD,L), BINOP8_A(ADD,refHL), BINOP8_A(ADD,A),
    /* 88+ */ BINOP8_A(ADC,B), BINOP8_A(ADC,C), BINOP8_A(ADC,D), BINOP8_A(ADC,E), BINOP8_A(ADC, H), BINOP8_A(ADC,L), BINOP8_A(ADC,refHL), BINOP8_A(ADC,A),
    /* 90+ */ BINOP8_A(SUB,B), BINOP8_A(SUB,C), BINOP8_A(SUB,D), BINOP8_A(SUB,E), BINOP8_A(SUB, H), BINOP8_A(SUB,L), BINOP8_A(SUB,refHL), BINOP8_A(SUB,A),
    /* 98+ */ BINOP8_A(SBC,B), BINOP8_A(SBC,C), BINOP8_A(SBC,D), BINOP8_A(SBC,E), BINOP8_A(SBC, H), BINOP8_A(SBC,L), BINOP8_A(SBC,refHL), BINOP8_A(SBC,A),
    /* A0+ */ BINOP8_A(AND,B), BINOP8_A(AND,C), BINOP8_A(AND,D), BINOP8_A(AND,E), BINOP8_A(AND, H), BINOP8_A(AND,L), BINOP8_A(AND,refHL), BINOP8_A(AND,A),
    /* A8+ */ BINOP8_A(XOR,B), BINOP8_A(XOR,C), BINOP8_A(XOR,D), BINOP8_A(XOR,E), BINOP8_A(XOR, H), BINOP8_A(XOR,L), BINOP8_A(XOR,refHL), BINOP8_A(XOR,A),
    /* B0+ */ BINOP8_A(OR,B), BINOP8_A(OR,C), BINOP8_A(OR,D), BINOP8_A(OR,E), BINOP8_A(OR,H), BINOP8_A(OR,L), BINOP8_A(OR,refHL), BINOP8_A(OR,A),
    /* B8+ */ BINOP8_A(CP,B), BINOP8_A(CP,C), BINOP8_A(CP,D), BINOP8_A(CP,E), BINOP8_A(CP,H), BINOP8_A(CP,L), BINOP8_A(CP,refHL), BINOP8_A(CP,A),
    /* C0+ */ RET(FC_NZ), POP_rr(BC), JP_u16(FC_NZ), JP_u16(FC_NONE), CALL_u16(FC_NZ), PUSH_rr(BC), BINOP8_A(ADD, n), RST(0x00),
    /* C8+ */ RET(FC_Z), RET(FC_NONE), JP_u16(FC_Z), PREFIX_CB(), CALL_u16(FC_Z), CALL_u16(FC_NONE), BINOP8_A(ADC,n), RST(0x08),
    /* D0+ */ RET(FC_NC), POP_rr(DE), JP_u16(FC_NC), NULL(0xD3), CALL_u16(FC_NC), PUSH_rr(DE), BINOP8_A(SUB,n), RST(0x10),
    /* D8+ */ RET(FC_C), RETI(), JP_u16(FC_C), NULL(0xDB), CALL_u16(FC_C), NULL(0xDD), BINOP8_A(SBC,n), RST(0x18),
    /* E0+ */ LD_FF00pu8_A(), POP_rr(HL), LD_FF00pC_A(), NULL(0xE3), NULL(0xE4), PUSH_rr(HL), BINOP8_A(AND,n), RST(0x20),
    /* E8+ */ ADD_SP_i8(), JP_HL(), LD_u16_A(), NULL(0xEB), NULL(0xEC), NULL(0xED), BINOP8_A(XOR,n), RST(0x28),
    /* F0+ */ LD_A_FF00pu8(), POP_rr(AF), LD_A_FF00pC(), DI(), NULL(0xF4), PUSH_rr(AF), BINOP8_A(OR,n), RST(0x30),
    /* F8+ */ LD_HL_SPpi8(), LD_SP_HL(), LD_A_u16(), EI(), NULL(0xFC), NULL(0xFD), BINOP8_A(CP,n), RST(0x38)
    // @formatter:on
)

private fun u16(hi: UByte, lo: UByte) = (lo.toUInt() or (hi.toUInt() shl 8)).toUShort()
private fun hi8(v: UShort) = ((v.toUInt() and 0xFF00u) shr 8).toUByte()
private fun lo8(v: UShort) = (v.toUInt() and 0x00FFu).toUByte()
private fun add16(v: UShort, w: UShort): Triple<UShort, Boolean, Boolean> {
    val t = v + w
    val overflow = t > UShort.MAX_VALUE
    val halfCarry = ((v.toUInt() and 0x00FFu) + (w.toUInt() and 0x00FFu)) and 0x100u == 0x100u
    return Triple(t.toUShort(), overflow, halfCarry)
}

private fun sub16(v: UShort, w: UShort): Pair<UShort, Boolean> {
    val t = v.toInt() - w.toInt()
    val underflow = t < 0
    return Pair(t.toUShort(), underflow)
}

private fun add8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
    val t = v + w
    val overflow = t > UByte.MAX_VALUE
    val halfCarry = ((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu)) and 0x10u == 0x10u
    return Triple(t.toUByte(), overflow, halfCarry)
}

private fun adc8(v: UByte, w: UByte, c: Boolean): Triple<UByte, Boolean, Boolean> {
    val x = if (c) 1u else 0u
    val t = v + w + x
    val overflow = t > UByte.MAX_VALUE
    val halfCarry = ((v.toUInt() and 0x0Fu) + (w.toUInt() and 0x0Fu) + x) and 0x10u == 0x10u
    return Triple(t.toUByte(), overflow, halfCarry)
}

private fun sub8(v: UByte, w: UByte): Triple<UByte, Boolean, Boolean> {
    val t = v.toInt() - w.toInt()
    val underflow = t < 0
    val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu)) and 0x10u == 0x10u
    return Triple(t.toUByte(), underflow, halfCarry)
}

private fun sbc8(v: UByte, w: UByte,c: Boolean): Triple<UByte, Boolean, Boolean> {
    val x = if (c) 1u else 0u
    val t = v.toInt() - w.toInt() - x.toInt()
    val underflow = t < 0
    val halfCarry = ((v.toUInt() and 0x0Fu) - (w.toUInt() and 0x0Fu) - x) and 0x10u == 0x10u
    return Triple(t.toUByte(), underflow, halfCarry)
}
