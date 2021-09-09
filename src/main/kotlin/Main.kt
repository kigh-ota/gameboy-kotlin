import Instruction.NOP

fun main() {

}

// https://izik1.github.io/gbops/
enum class Instruction {
    /* 00+ */ NOP, LD_BC_u16, LD_BC_A, INC_BC, INC_B, DEC_B, LD_B_u8, RLCA,
    /* 08+ */ LD_u16_SP, ADD_HL_BC, LD_A_BC, DEC_BC, INC_C, DEC_C, LD_C_u8, RRCA,
    /* 10+ */ STOP, LD_DE_u16, LD_DE_A, INC_DE, INC_D, DEC_D, LD_D_u8, RLA,
    /* 18+ */ JR_i8, ADD_HL_DE, LD_A_DE, DEC_DE, INC_E, DEC_E, LD_E_u8, RRA,
    /* 20+ */ JR_NZ_i8, LD_HL_u16, LD_HLp_A, INC_HL, INC_H, DEC_H, LD_H_u8, DAA,
    /* 28+ */ JR_Z_i8, ADD_HL_HL, LD_A_HLp, DEC_HL, INC_L, DEC_L, LD_L_u8, CPL,
    /* 30+ */ JR_NC_i8, LD_SP_u16, LD_HLn_A, INC_SP, INC2_HL, DEC2_HL, LD_HL_u8, SCF,
    /* 38+ */ JR_C_i8, ADD_HL_SP, LD_A_HLn, DEC_SP, INC_A, DEC_A, LD_A_u8, CCF,
    /* 40+ */ LD_B_B, LD_B_C, LD_B_D, LD_B_E, LD_B_H, LD_B_L, LD_B_HL, LD_B_A,
    /* 48+ */ LD_C_B, LD_C_C, LD_C_D, LD_C_E, LD_C_H, LD_C_L, LD_C_HL, LD_C_A,
    /* 50+ */ LD_D_B, LD_D_C, LD_D_D, LD_D_E, LD_D_H, LD_D_L, LD_D_HL, LD_D_A,
    /* 58+ */ LD_E_B, LD_E_C, LD_E_D, LD_E_E, LD_E_H, LD_E_L, LD_E_HL, LD_E_A,
    /* 60+ */ LD_H_B, LD_H_C, LD_H_D, LD_H_E, LD_H_H, LD_H_L, LD_H_HL, LD_H_A,
    /* 68+ */ LD_L_B, LD_L_C, LD_L_D, LD_L_E, LD_L_H, LD_L_L, LD_L_HL, LD_L_A,
    /* 70+ */ LD_HL_B, LD_HL_C, LD_HL_D, LD_HL_E, LD_HL_H, LD_HL_L, HALT, LD_HL_A,
    /* 78+ */ LD_A_B, LD_A_C, LD_A_D, LD_A_E, LD_A_H, LD_A_L, LD_A_HL, LD_A_A,
    /* 80+ */ ADD_A_B, ADD_A_C, ADD_A_D, ADD_A_E, ADD_A_H, ADD_A_L, ADD_A_HL, ADD_A_A,
    /* 88+ */ ADC_A_B, ADC_A_C, ADC_A_D, ADC_A_E, ADC_A_H, ADC_A_L, ADC_A_HL, ADC_A_A,
    /* 90+ */ SUB_A_B, SUB_A_C, SUB_A_D, SUB_A_E, SUB_A_H, SUB_A_L, SUB_A_HL, SUB_A_A,
    /* 98+ */ SBC_A_B, SBC_A_C, SBC_A_D, SBC_A_E, SBC_A_H, SBC_A_L, SBC_A_HL, SBC_A_A,
    /* A0+ */ AND_A_B, AND_A_C, AND_A_D, AND_A_E, AND_A_H, AND_A_L, AND_A_HL, AND_A_A,
    /* A8+ */ XOR_A_B, XOR_A_C, XOR_A_D, XOR_A_E, XOR_A_H, XOR_A_L, XOR_A_HL, XOR_A_A,
    /* B0+ */ OR_A_B, OR_A_C, OR_A_D, OR_A_E, OR_A_H, OR_A_L, OR_A_HL, OR_A_A,
    /* B8+ */ CP_A_B, CP_A_C, CP_A_D, CP_A_E, CP_A_H, CP_A_L, CP_A_HL, CP_A_A,
    /* C0+ */ RET_NZ, POP_BC, JP_NZ_u16, JP_u16, CALL_NZ_u16, PUSH_BC, ADD_A_u8, RST_00h,
    /* C8+ */ RET_Z, RET, JP_Z_u16, PREFIX_CB, CALL_Z_u16, CALL_u16, ADC_A_u8, RST_08h,
    /* D0+ */ RET_NC, POP_DE, JP_NC_u16, NULL_D3, CALL_NC_u16, PUSH_DE, SUB_A_u8, RST_10h,
    /* D8+ */ RET_C, RETI, JP_C_u16, NULL_DB, CALL_C_u16, NULL_DD, SBC_A_u8, RST_18h,
    /* E0+ */ LD_FF00pu8_A, POP_HL, LD_FF00pC_A, NULL_E3, NULL_E4, PUSH_HL, AND_A_u8, RST_20h,
    /* E8+ */ ADD_SP_i8, JP_HL, LD_u16_A, NULL_EB, NULL_EC, NULL_ED, XOR_A_u8, RST_28h,
    /* F0+ */ LD_A_FF00pu8, POP_AF, LD_A_FF00pC, DI, NULL_F4, PUSH_AF, OR_A_u8, RST_30h,
    /* F8+ */ LD_HL_SPpi8, LD_SP_HL, LD_A_u16, EI, NULL_FC, NULL_FD, CP_A_u8, RST_38h,
}

@ExperimentalUnsignedTypes
class Emulator {
    companion object {
        private const val MEMORY_SIZE = 0x10000 // 64KiB

        private const val STACK_SIZE = 0x10 // 16 stacks
        private const val TARGET_FREQ = 500u
    }

    private val mem = UByteArray(MEMORY_SIZE) { 0u }

    // Registers
    private var AF: UShort = 0x0000u
    private var BC: UShort = 0x0000u
    private var DE: UShort = 0x0000u
    private var HL: UShort = 0x0000u
    private var SP: UShort = 0x0000u
    private var PC: UShort = 0x0000u

    private fun initializeRegisters() {
        AF = 0x0100u // FIXME
        BC = 0x0013u
        DE = 0x00D8u
        HL = 0x014Du
        SP = 0xFFFEu
        PC = 0x0100u

        Instruction.values()
    }

    private fun read(): UByte {
        val b = mem[PC.toInt()]
        PC++
        return b
    }

    private fun fetchInstruction(): Instruction {
        throw NotImplementedError()
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
