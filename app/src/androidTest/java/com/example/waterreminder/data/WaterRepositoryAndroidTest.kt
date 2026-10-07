package com.example.waterreminder.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterRepositoryAndroidTest {

    private lateinit var context: Context
    private lateinit var repository: WaterRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = WaterRepository(context)
        repository.resetIntake()
    }

    @Test
    fun testInitialIntakeIsZeroAfterReset() {
        assertEquals(0, repository.getCurrentIntake())
    }

    @Test
    fun testAddWaterIncreasesIntake() {
        val updated = repository.addWater(250)
        assertEquals(250, updated)
        assertEquals(250, repository.getCurrentIntake())

        val secondUpdate = repository.addWater(500)
        assertEquals(750, secondUpdate)
        assertEquals(750, repository.getCurrentIntake())
    }

    @Test
    fun testSetCurrentIntakeDirectly() {
        repository.setCurrentIntake(1500)
        assertEquals(1500, repository.getCurrentIntake())
    }

    @Test
    fun testGoalClamping() {
        repository.setDailyGoal(500) // below MIN_GOAL (1000)
        assertEquals(WaterRepository.MIN_GOAL, repository.getDailyGoal())

        repository.setDailyGoal(5000) // above MAX_GOAL (4000)
        assertEquals(WaterRepository.MAX_GOAL, repository.getDailyGoal())
    }

    @Test
    fun testIntervalClamping() {
        repository.setReminderIntervalMinutes(5) // below MIN_INTERVAL (15)
        assertEquals(WaterRepository.MIN_INTERVAL, repository.getReminderIntervalMinutes())

        repository.setReminderIntervalMinutes(300) // above MAX_INTERVAL (240)
        assertEquals(WaterRepository.MAX_INTERVAL, repository.getReminderIntervalMinutes())
    }

    @Test
    fun testReminderToggle() {
        repository.setReminderEnabled(false)
        assertTrue(!repository.isReminderEnabled())

        repository.setReminderEnabled(true)
        assertTrue(repository.isReminderEnabled())
    }
}
