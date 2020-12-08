/*
 * Copyright 2020 Russell Wolf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalSettingsImplementation::class)

package com.russhwolf.settings

import java.util.concurrent.CountDownLatch
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

private val preferences = Preferences.userRoot()
private val factory = JvmPreferencesSettings.Factory(preferences)

class JvmPreferencesSettingsTest : BaseSettingsTest(
    platformFactory = factory,
    syncListeners = preferences::syncListeners
) {
    @Test
    fun constructor_preferences() {
        val preferences = Preferences.userRoot().node("Settings")
        val settings = JvmPreferencesSettings(preferences)

        preferences.putInt("a", 3)
        assertEquals(3, settings["a", 0])
    }


    @Test
    fun factory_name() {
        val preferences = Preferences.userRoot().node("Settings")
        val settings = factory.create("Settings")

        preferences.putInt("a", 3)
        assertEquals(3, settings["a", 0])
    }

    @Test
    fun factory_noName() {
        val settings = factory.create()

        preferences.putInt("a", 3)
        assertEquals(3, settings["a", 0])
    }
}

/**
 * This is a workaround for the fact that Preferences listeners are forcibly executed on a background thread. We call
 * this function before verifying listener calls in our test in order to give that background thread a chance to run
 * first. This should clear out the listener queue because the internal listener here is being added last.
 */
private fun Preferences.syncListeners() {
    val latch = CountDownLatch(1)
    val newValue = 1 + getInt("sync", 0)
    val preferenceChangeListener = object : PreferenceChangeListener {
        override fun preferenceChange(it: PreferenceChangeEvent) {
            if (it.key == "sync" && it.newValue == newValue.toString()) {
                latch.countDown()
                removePreferenceChangeListener(this)
            }
        }
    }
    addPreferenceChangeListener(preferenceChangeListener)
    putInt("sync", newValue)
    latch.await()
}
