package com.example.floatingtimer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    @Test
    fun testTimerAccumulation() {
        var remainingSeconds = 0
        val step = 15

        remainingSeconds += step
        assertEquals(15, remainingSeconds)

        remainingSeconds += step
        assertEquals(30, remainingSeconds)
    }

    @Test
    fun testCustomStepListManagement() {
        val initialSteps = mutableListOf(5, 10, 15, 20, 30, 60)

        // 添加 45s 自定义额度
        if (!initialSteps.contains(45)) {
            initialSteps.add(45)
        }
        initialSteps.sort()

        assertEquals(7, initialSteps.size)
        assertTrue(initialSteps.contains(45))

        // 移除 10s
        initialSteps.remove(10)
        assertEquals(6, initialSteps.size)
    }
}
