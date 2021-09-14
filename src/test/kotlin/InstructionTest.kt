import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InstructionTest {
    @Test
    fun numberOfValues() {
        assertThat(INSTRUCTIONS.size).isEqualTo(16*16)
        assertThat(INSTRUCTIONS_CB.size).isEqualTo(16*16)
    }
}
