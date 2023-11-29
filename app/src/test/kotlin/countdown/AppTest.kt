package countdown

import org.junit.jupiter.api.Test
import kotlin.test.*

class RemoveTests {
    @Test
    fun testRemoveNotPresent() {
        assertEquals(listOf(1,2,3,4).remove(8), listOf(1,2,3,4))
    }

    @Test
    fun testRemovePresent() {
        assertEquals(listOf(1,2,3,4,2).remove(2), listOf(1,3,4,2))
    }
}

class SolveTests {
    @Test
    fun exact() {
        exact(834, 10, 9, 8, 7, 6, 5)(5)
        exact(378, 50, 7, 4, 3, 2, 1)(3)
        exact(493, 50, 25, 4, 3, 2, 4)(6)
        exact(803, 50, 4, 9, 6, 6, 1)(6)
        exact(827, 25, 8, 5, 8, 1, 2)(6)
        exact(401, 10, 4, 5, 2, 3, 3)(6)
        exact(100, 100, 4, 5, 2, 3, 3)(1)
    }

    fun exact(target: Int, vararg numbers: Int): (Int) -> Unit =
        {expectedCount -> solve(target, *numbers).let {e ->
                assertNotNull(e)
                assertEquals(e.value, target)
                assertEquals(e.count, expectedCount)
            }
        }

    @Test
    fun nonExact() {
        solve(954, 50, 75, 25, 100, 5, 8).let {e ->
            assertNotNull(e)
            assertEquals(e.value, 955)
            assertEquals(e.count, 5)
        }
    }

    @Test
    fun noSolution() {
        assertNull(solve(999, 1, 2, 3, 4, 5, 6))
    }
}
