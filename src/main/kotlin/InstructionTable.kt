import FlagCondition.*
import Ref16.*
import Ref8.*

enum class Ref16 { BC, DE, HL, SP, AF, nn }
enum class Ref8 { B, C, D, E, H, L, A, refHL, refBC, refDE, n, refNN, refC, refN }
enum class FlagCondition { FC_NONE, FC_NZ, FC_Z, FC_NC, FC_C }

// 8-bit loads
private fun LD8(dst: Ref8, src: Ref8) = fun(em: Emulator) = em.ld8(dst, src)
private fun LDI8(dst: Ref8, src: Ref8) = fun(em: Emulator) = em.ldi8(dst, src)
private fun LDD8(dst: Ref8, src: Ref8) = fun(em: Emulator) = em.ldd8(dst, src)

// 16-bit loads
private fun LD16(dst: Ref16, src: Ref16) = fun(em: Emulator) = em.ld16(dst, src)
private fun LDHL16() = fun(em: Emulator) = em.ldHL16()
private fun PUSH16(rr: Ref16) = fun(em: Emulator) = em.push16(rr)
private fun POP16(rr: Ref16) = fun(em: Emulator) = em.pop16(rr)

// 8-bit ALU
private fun ADD_A(r: Ref8) = fun(em: Emulator) = em.addA8(r)
private fun ADC_A(r: Ref8) = fun(em: Emulator) = em.adcA8(r)
private fun SUB_A(r: Ref8) = fun(em: Emulator) = em.subA8(r)
private fun SBC_A(r: Ref8) = fun(em: Emulator) = em.sbcA8(r)
private fun AND_A(r: Ref8) = fun(em: Emulator) = em.andA8(r)
private fun XOR_A(r: Ref8) = fun(em: Emulator) = em.xorA8(r)
private fun OR_A(r: Ref8) = fun(em: Emulator) = em.orA8(r)
private fun CP_A(r: Ref8) = fun(em: Emulator) = em.cpA8(r)
private fun INC8(r: Ref8) = fun(em: Emulator) = em.inc8(r)
private fun DEC8(r: Ref8) = fun(em: Emulator) = em.dec8(r)

// 16-bit arithmetic
private fun ADD_HL16(rr: Ref16) = fun(em: Emulator) = em.addHL16(rr)
private fun ADD_SP_n() = fun(em: Emulator) = em.addSP()
private fun INC16(rr: Ref16) = fun(em: Emulator) = em.inc16(rr)
private fun DEC16(rr: Ref16) = fun(em: Emulator) = em.dec16(rr)

// Miscellaneous
private fun JR(fc: FlagCondition) = fun(em: Emulator) = em.jr(fc)
private fun DAA() = fun(em: Emulator) = em.daa()
private fun CPL() = fun(em: Emulator) = em.cpl()
private fun SCF() = fun(em: Emulator) = em.scf()
private fun CCF() = fun(em: Emulator) = em.ccf()
private fun NOP() = fun(em: Emulator) = em.nop()
private fun HALT() = fun(em: Emulator) = em.halt()
private fun STOP() = fun(em: Emulator) = em.stop()
private fun DI() = fun(em: Emulator) = em.di()
private fun EI() = fun(em: Emulator) = em.ei()
private fun RLCA() = fun(em: Emulator) = em.rlca()
private fun RRCA() = fun(em: Emulator) = em.rrca()
private fun RLA() = fun(em: Emulator) = em.rla()
private fun RRA() = fun(em: Emulator) = em.rra()
private fun RST(addr: UShort) = fun(em: Emulator) = em.rst(addr)
private fun RET(fc: FlagCondition) = fun(em: Emulator) = em.ret(fc)
private fun RETI() = fun(em: Emulator) = em.reti()
private fun CALL(fc: FlagCondition) = fun(em: Emulator) = em.call(fc)
private fun PREFIX_CB() = fun(em: Emulator) = 0
private fun NULL(opcode: Int) = fun(em: Emulator) = 0
private fun RLC(r: Ref8) = fun(em: Emulator) = em.rlc(r)
private fun RRC(r: Ref8) = fun(em: Emulator) = em.rrc(r)
private fun RL(r: Ref8) = fun(em: Emulator) = em.rl(r)
private fun RR(r: Ref8) = fun(em: Emulator) = em.rr(r)
private fun SLA(r: Ref8) = fun(em: Emulator) = em.sla(r)
private fun SRA(r: Ref8) = fun(em: Emulator) = em.sra(r)
private fun SRL(r: Ref8) = fun(em: Emulator) = em.srl(r)
private fun SWAP(r: Ref8) = fun(em: Emulator) = em.swap(r)
private fun BIT(r: Ref8, b: Int) = fun(em: Emulator) = em.bit(r, b)
private fun RES(r: Ref8, b: Int) = fun(em: Emulator) = em.res(r, b)
private fun SET(r: Ref8, b: Int) = fun(em: Emulator) = em.set(r, b)
private fun JP(fc: FlagCondition) = fun(em: Emulator) = em.jp(fc)
private fun JP_HL() = fun(em: Emulator) = em.jpHL()

fun getInstruction(opcodeReader: () -> UByte): (Emulator) -> Int {
    val opcode = opcodeReader()
    return when (opcode.toUInt()) {
        0xCBu -> INSTRUCTIONS_CB[opcode.toInt()]
        else -> INSTRUCTIONS[opcode.toInt()]
    }
}

internal val INSTRUCTIONS = arrayOf(
    // @formatter:off
    /* 00+ */ NOP(), LD16(BC, nn), LD8(refBC, A), INC16(BC), INC8(B), DEC8(B), LD8(B,n), RLCA(),
    /* 08+ */ LD16(nn,SP), ADD_HL16(BC), LD8(A, refBC), DEC16(BC), INC8(C), DEC8(C), LD8(C,n), RRCA(),
    /* 10+ */ STOP(), LD16(DE, nn), LD8(refDE, A), INC16(DE),INC8(D), DEC8(D), LD8(D,n), RLA(),
    /* 18+ */ JR(FC_NONE), ADD_HL16(DE), LD8(A, refDE), DEC16(DE), INC8(E), DEC8(E), LD8(E,n), RRA(),
    /* 20+ */ JR(FC_NZ), LD16(HL, nn), LDI8(refHL,A), INC16(HL), INC8(H), DEC8(H), LD8(H,n), DAA(),
    /* 28+ */ JR(FC_Z), ADD_HL16(HL), LDI8(A,refHL), DEC16(HL), INC8(L), DEC8(L), LD8(L,n), CPL(),
    /* 30+ */ JR(FC_NC), LD16(SP, nn), LDD8(refHL,A), INC16(SP), INC8(refHL), DEC8(refHL), LD8(refHL,n), SCF(),
    /* 38+ */ JR(FC_C), ADD_HL16(SP), LDD8(A,refHL), DEC16(SP), INC8(A), DEC8(A), LD8(A,n), CCF(),
    /* 40+ */ LD8(B,B), LD8(B,C), LD8(B,D), LD8(B,E), LD8(B,H), LD8(B,L), LD8(B,refHL), LD8(B,A),
    /* 48+ */ LD8(C,B), LD8(C,C), LD8(C,D), LD8(C,E), LD8(C,H), LD8(C,L), LD8(C,refHL), LD8(C,A),
    /* 50+ */ LD8(D,B), LD8(D,C), LD8(D,D), LD8(D,E), LD8(D,H), LD8(D,L), LD8(D,refHL), LD8(D,A),
    /* 58+ */ LD8(E,B), LD8(E,C), LD8(E,D), LD8(E,E), LD8(E,H), LD8(E,L), LD8(E,refHL), LD8(E,A),
    /* 60+ */ LD8(H,B), LD8(H,C), LD8(H,D), LD8(H,E), LD8(H,H), LD8(H,L), LD8(H,refHL), LD8(H,A),
    /* 68+ */ LD8(L,B), LD8(L,C), LD8(L,D), LD8(L,E), LD8(L,H), LD8(L,L), LD8(L,refHL), LD8(L,A),
    /* 70+ */ LD8(refHL,B), LD8(refHL,C), LD8(refHL,D), LD8(refHL,E), LD8(refHL,H), LD8(refHL,L), HALT(), LD8(refHL,A),
    /* 78+ */ LD8(A,B), LD8(A,C), LD8(A,D), LD8(A,E), LD8(A,H), LD8(A,L), LD8(A,refHL), LD8(A,A),
    /* 80+ */ ADD_A(B), ADD_A(C), ADD_A(D), ADD_A(E), ADD_A(H), ADD_A(L), ADD_A(refHL), ADD_A(A),
    /* 88+ */ ADC_A(B), ADC_A(C), ADC_A(D), ADC_A(E), ADC_A(H), ADC_A(L), ADC_A(refHL), ADC_A(A),
    /* 90+ */ SUB_A(B), SUB_A(C), SUB_A(D), SUB_A(E), SUB_A(H), SUB_A(L), SUB_A(refHL), SUB_A(A),
    /* 98+ */ SBC_A(B), SBC_A(C), SBC_A(D), SBC_A(E), SBC_A(H), SBC_A(L), SBC_A(refHL), SBC_A(A),
    /* A0+ */ AND_A(B), AND_A(C), AND_A(D), AND_A(E), AND_A(H), AND_A(L), AND_A(refHL), AND_A(A),
    /* A8+ */ XOR_A(B), XOR_A(C), XOR_A(D), XOR_A(E), XOR_A(H), XOR_A(L), XOR_A(refHL), XOR_A(A),
    /* B0+ */ OR_A(B), OR_A(C), OR_A(D), OR_A(E), OR_A(H), OR_A(L), OR_A(refHL), OR_A(A),
    /* B8+ */ CP_A(B), CP_A(C), CP_A(D), CP_A(E), CP_A(H), CP_A(L), CP_A(refHL), CP_A(A),
    /* C0+ */ RET(FC_NZ), POP16(BC), JP(FC_NZ), JP(FC_NONE), CALL(FC_NZ), PUSH16(BC), ADD_A(n), RST(0x0000u),
    /* C8+ */ RET(FC_Z), RET(FC_NONE), JP(FC_Z), PREFIX_CB(), CALL(FC_Z), CALL(FC_NONE), ADC_A(n), RST(0x0008u),
    /* D0+ */ RET(FC_NC), POP16(DE), JP(FC_NC), NULL(0xD3), CALL(FC_NC), PUSH16(DE), SUB_A(n), RST(0x0010u),
    /* D8+ */ RET(FC_C), RETI(), JP(FC_C), NULL(0xDB), CALL(FC_C), NULL(0xDD), SBC_A(n), RST(0x0018u),
    /* E0+ */ LD8(refN,A), POP16(HL), LD8(C,A), NULL(0xE3), NULL(0xE4), PUSH16(HL), AND_A(n), RST(0x0020u),
    /* E8+ */ ADD_SP_n(), JP_HL(), LD8(refNN,A), NULL(0xEB), NULL(0xEC), NULL(0xED), XOR_A(n), RST(0x0028u),
    /* F0+ */ LD8(A,refN), POP16(AF), LD8(A,refC), DI(), NULL(0xF4), PUSH16(AF), OR_A(n), RST(0x0030u),
    /* F8+ */ LDHL16(), LD16(SP,HL), LD8(A,refNN), EI(), NULL(0xFC), NULL(0xFD), CP_A(n), RST(0x0038u)
    // @formatter:on
)

internal val INSTRUCTIONS_CB = Array(256) {
    val ref8 = when (it % 8) {
        0 -> B
        1 -> C
        2 -> D
        3 -> E
        4 -> H
        5 -> L
        6 -> refHL
        7 -> A
        else -> throw IllegalArgumentException()
    }
    when (it) {
        in 0x00..0x07 -> RLC(ref8)
        in 0x08..0x0F -> RRC(ref8)
        in 0x10..0x17 -> RL(ref8)
        in 0x18..0x1F -> RR(ref8)
        in 0x20..0x27 -> SLA(ref8)
        in 0x28..0x2F -> SRA(ref8)
        in 0x30..0x37 -> SWAP(ref8)
        in 0x38..0x3F -> SRL(ref8)
        in 0x40..0x7F -> BIT(ref8, (it / 8) % 8)
        in 0x80..0xBF -> RES(ref8, (it / 8) % 8)
        in 0xC0..0xFF -> SET(ref8, (it / 8) % 8)
        else -> throw IllegalArgumentException()
    }
}
