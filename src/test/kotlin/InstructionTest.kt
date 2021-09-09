import Instruction.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class InstructionTest {
    @Test
    fun numberOfValues() {
        assertThat(values().size).isEqualTo(16*16)
    }

    @Test
    fun someValues() {
        assertThat(values()[0x40]).isEqualTo(LD_B_B)
        assertThat(values()[0x80]).isEqualTo(ADD_A_B)
        assertThat(values()[0xC0]).isEqualTo(RET_NZ)
        assertThat(values()[0xE0]).isEqualTo(LD_FF00pu8_A)
        assertThat(values()[0xE8]).isEqualTo(ADD_SP_i8)
        assertThat(values()[0xF0]).isEqualTo(LD_A_FF00pu8)
        assertThat(values()[0xF8]).isEqualTo(LD_HL_SPpi8)
    }
}
