import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import Emulator.Companion.add16
import Emulator.Companion.add8
import Emulator.Companion.sub16
import Emulator.Companion.sub8

@ExperimentalUnsignedTypes
internal class EmulatorTest {
    @Test
    fun testAdd16() {
        assertThat(add16(0u, 0u)).isEqualTo(Triple(0u.toUShort(), false, false))
        assertThat(add16(0xFFFFu, 1u)).isEqualTo(Triple(0u.toUShort(), true, true))
        assertThat(add16(0xFFFFu, 0xFFFFu)).isEqualTo(Triple(0xFFFEu.toUShort(), true, true))
        assertThat(add16(0x00FFu, 0x0001u)).isEqualTo(Triple(0x0100u.toUShort(), false, true))
    }
    @Test
    fun testSub16() {
        assertThat(sub16(0u, 0u)).isEqualTo(Pair(0u.toUShort(), false))
        assertThat(sub16(0u, 1u)).isEqualTo(Pair(0xFFFFu.toUShort(), true))
        assertThat(sub16(0u, 0xFFFFu)).isEqualTo(Pair(1u.toUShort(), true))
    }
    @Test
    fun testAdd8() {
        assertThat(add8(0u, 0u)).isEqualTo(Triple(0u.toUByte(), false, false))
        assertThat(add8(0xFFu, 1u)).isEqualTo(Triple(0u.toUByte(), true, true))
        assertThat(add8(0xFFu, 0xFFu)).isEqualTo(Triple(0xFEu.toUByte(), true, true))
        assertThat(add8(0x0Fu, 0x01u)).isEqualTo(Triple(0x10u.toUByte(), false, true))
    }
    @Test
    fun testSub8() {
        assertThat(sub8(0u, 0u)).isEqualTo(Triple(0u.toUByte(), false, false))
        assertThat(sub8(0u, 1u)).isEqualTo(Triple(0xFFu.toUByte(), true, true))
        assertThat(sub8(0u, 0xFFu)).isEqualTo(Triple(1u.toUByte(), true, true))
        assertThat(sub8(0x10u, 0x01u)).isEqualTo(Triple(0x0Fu.toUByte(), false, true))
    }

    @Test
    fun testJumpI8() {
        Emulator().apply {
            PC = 0x0100u
            jumpI8(0x01u)
            assertThat(PC).isEqualTo(0x0101u.toUShort())

            PC = 0x0100u
            jumpI8(0xFFu)
            assertThat(PC).isEqualTo(0x00FFu.toUShort())
        }
    }

}
