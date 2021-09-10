import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InstructionTest {
    @Test
    fun numberOfValues() {
        assertThat(instructions.size).isEqualTo(16*16)
    }

    @Test
    fun someValues() {
        assertThat(instructions[0x40]).isInstanceOf(LD8::class.java) // .isEqualTo(LD_B_B)
        assertThat(instructions[0x80]).isInstanceOf(BINOP8_A::class.java)// .isEqualTo(ADD_A_B)
        assertThat(instructions[0xC0]).isInstanceOf(RET::class.java)// .isEqualTo(RET_NZ)
        assertThat(instructions[0xE0]).isInstanceOf(LD_FF00pu8_A::class.java)// .isEqualTo(LD_FF00pu8_A)
        assertThat(instructions[0xE8]).isInstanceOf(ADD_SP_i8::class.java)// .isEqualTo(ADD_SP_i8)
        assertThat(instructions[0xF0]).isInstanceOf(LD_A_FF00pu8::class.java)// .isEqualTo(LD_A_FF00pu8)
        assertThat(instructions[0xF8]).isInstanceOf(LD_HL_SPpi8::class.java)// .isEqualTo(LD_HL_SPpi8)
    }
}
