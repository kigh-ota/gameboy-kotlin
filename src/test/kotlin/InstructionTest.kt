import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InstructionTest {
    @Test
    fun numberOfValues() {
        assertThat(INSTRUCTIONS.size).isEqualTo(16*16)
    }

    @Test
    fun someValues() {
        assertThat(INSTRUCTIONS[0x40]).isInstanceOf(LD8::class.java) // .isEqualTo(LD_B_B)
        assertThat(INSTRUCTIONS[0x80]).isInstanceOf(ADD_A::class.java)// .isEqualTo(ADD_A_B)
        assertThat(INSTRUCTIONS[0xC0]).isInstanceOf(RET::class.java)// .isEqualTo(RET_NZ)
        assertThat(INSTRUCTIONS[0xE0]).isInstanceOf(LD8::class.java)// .isEqualTo(LD_FF00pu8_A)
        assertThat(INSTRUCTIONS[0xE8]).isInstanceOf(ADD_SP_n::class.java)// .isEqualTo(ADD_SP_i8)
        assertThat(INSTRUCTIONS[0xF0]).isInstanceOf(LD8::class.java)// .isEqualTo(LD_A_FF00pu8)
        assertThat(INSTRUCTIONS[0xF8]).isInstanceOf(LDHL16::class.java)// .isEqualTo(LD_HL_SPpi8)
    }
}
