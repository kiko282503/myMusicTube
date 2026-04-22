package com.musictube.player

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test:
 *  1. Search for "t.n.t"
 *  2. Download the AC/DC result
 *  3. Open Offline Downloads playlist
 *  4. Play the downloaded song
 *  5. Turn off WiFi
 *  6. Assert playback continues (Pause button still visible)
 */
@RunWith(AndroidJUnit4::class)
class OfflinePlaybackTest {

    private lateinit var device: UiDevice
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    companion object {
        private const val APP_PACKAGE = "com.musictube.player"
        private const val LAUNCH_TIMEOUT_MS = 10_000L
        private const val SEARCH_RESULTS_TIMEOUT_MS = 30_000L
        private const val DOWNLOAD_TIMEOUT_MS = 300_000L   // URL extraction + file download can be slow
        private const val PLAYBACK_START_TIMEOUT_MS = 30_000L
        private const val SHORT_WAIT_MS = 2_000L
        private const val LONG_WAIT_MS = 5_000L
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(instrumentation)

        // Erase stale downloads and DB so each run starts clean.
        // The app process must NOT be force-stopped here because this test runs inside
        // the same UID and force-stop would kill the instrumentation process too.
        // Instead we rely on the app process having been stopped by tearDown's relaunch.
        device.executeShellCommand("run-as $APP_PACKAGE rm -rf files/downloads")
        device.executeShellCommand("run-as $APP_PACKAGE rm databases/music_database")
        device.executeShellCommand("run-as $APP_PACKAGE rm databases/music_database-shm")
        device.executeShellCommand("run-as $APP_PACKAGE rm databases/music_database-wal")
        Thread.sleep(SHORT_WAIT_MS)


        // Make sure WiFi is on before the test starts
        device.executeShellCommand("svc wifi enable")
        Thread.sleep(SHORT_WAIT_MS)

        // Dismiss any lingering system dialogs
        device.pressHome()
        device.waitForIdle()

        // Launch the app
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        checkNotNull(intent) { "Could not resolve launch intent for $APP_PACKAGE" }
        context.startActivity(intent)

        // Wait for the home screen — give extra time after DB wipe
        device.wait(Until.hasObject(By.text("My Music Player")), LAUNCH_TIMEOUT_MS * 3)
    }

    @After
    fun tearDown() {
        // Always restore WiFi so the device is left in a usable state
        device.executeShellCommand("svc wifi enable")
        Thread.sleep(SHORT_WAIT_MS)
    }

    @Test
    fun searchDownloadAndPlayOffline() {
        // ── Step 1: Open Search ─────────────────────────────────────────────
        val searchIcon = device.wait(Until.findObject(By.desc("Search")), LAUNCH_TIMEOUT_MS)
        assertNotNull("Search icon not found on home screen", searchIcon)
        searchIcon.click()
        device.waitForIdle()

        // ── Step 2: Type the query ──────────────────────────────────────────
        val searchField = device.wait(
            Until.findObject(By.text("Search for songs...")),
            LAUNCH_TIMEOUT_MS
        )
        assertNotNull("Search text field not found", searchField)
        searchField.click()
        device.waitForIdle()
        device.findObject(By.focused(true)).text = "t.n.t"
        device.pressEnter()
        device.waitForIdle()

        // ── Step 3: Wait for AC/DC result ───────────────────────────────────
        // The result list loads asynchronously. We need a card whose accessibility
        // node text contains both "T.N.T" and "AC" (from AC/DC).
        val acdcCard: UiObject2? = waitUntilFound(SEARCH_RESULTS_TIMEOUT_MS) {
            findAcdcTntCard()
        }
        assertNotNull(
            "Could not find an AC/DC T.N.T. result within ${SEARCH_RESULTS_TIMEOUT_MS / 1000}s",
            acdcCard
        )

        // ── Step 4: Download the AC/DC item if not already downloaded ───────
        val alreadyDownloaded = acdcCard!!.findObject(By.desc("Downloaded"))
        if (alreadyDownloaded == null) {
            val downloadBtn: UiObject2? = acdcCard.findObject(By.desc("Download for offline"))
            assertNotNull("Neither Downloaded nor Download button found for AC/DC T.N.T.", downloadBtn)
            downloadBtn!!.click()
            device.waitForIdle()

        // ── Step 5: Wait for download to complete ─────────────────────────
            val downloadFinalNode: UiObject2? = waitUntilFound(DOWNLOAD_TIMEOUT_MS) {
                try {
                    acdcCard.findObject(By.desc("Downloaded"))
                        ?: acdcCard.findObject(By.desc("Retry download"))
                } catch (_: Exception) {
                    // UI recomposed (StaleObjectException) — re-find the card and try again
                    val fresh = findAcdcTntCard()
                    if (fresh != null) fresh.findObject(By.desc("Downloaded"))
                        ?: fresh.findObject(By.desc("Retry download"))
                    else null
                }
            }
            assertNotNull(
                "Download did not reach a terminal state within ${DOWNLOAD_TIMEOUT_MS / 1000}s",
                downloadFinalNode
            )
            assertTrue(
                "Download FAILED for AC/DC T.N.T (Retry icon appeared) — check network and YouTube access",
                downloadFinalNode!!.contentDescription == "Downloaded"
            )
        }
        // If already downloaded, we trust the file is valid (skip re-download)

        // ── Step 6: Navigate back to Home ───────────────────────────────────
        // Relaunch the app to get a clean HomeScreen with all playlists loaded.
        device.pressHome()
        Thread.sleep(SHORT_WAIT_MS)
        val context2: Context = ApplicationProvider.getApplicationContext()
        val relaunchIntent = context2.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        checkNotNull(relaunchIntent)
        context2.startActivity(relaunchIntent)
        device.wait(Until.hasObject(By.text("My Music Player")), LAUNCH_TIMEOUT_MS)
        device.waitForIdle()
        Thread.sleep(LONG_WAIT_MS) // let DB flows settle

        // ── Step 7: Open Offline Downloads playlist ─────────────────────────
        // Playlist cards are in a horizontal-scroll Row near the top of the
        // home screen. The card's merged text includes playlist.name so use textContains.
        val offlineCard: UiObject2? = waitUntilFound(15_000L) {
            device.findObject(By.textContains("Offline Downloads"))
        }
        assertNotNull("Offline Downloads playlist card not found on home screen", offlineCard)
        offlineCard!!.click()
        device.waitForIdle()

        // ── Step 8: Find the T.N.T. song in the playlist and play it ────────
        val tntNode: UiObject2? = waitUntilFound(LONG_WAIT_MS) {
            device.findObject(By.textContains("T.N.T"))
        }
        assertNotNull("T.N.T song not found in Offline Downloads playlist", tntNode)
        // Walk up to find the clickable SongItem card if the text node itself isn't clickable
        var tntItem: UiObject2? = tntNode
        if (tntItem != null && !tntItem.isClickable) {
            var p = tntItem.parent
            for (i in 0 until 5) {
                val parent = p ?: break
                if (parent.isClickable) { tntItem = parent; break }
                p = parent.parent
            }
        }
        assertNotNull("T.N.T clickable item not found", tntItem)
        tntItem!!.click()
        device.waitForIdle()
        Thread.sleep(SHORT_WAIT_MS)  // give navigation + ExoPlayer a moment to start

        // ── Step 9: Wait for playback to start (Pause button appears) ───────
        // PlayerScreen shows "Pause" when isPlaying==true.
        val pauseBtn: UiObject2? = waitUntilFound(PLAYBACK_START_TIMEOUT_MS) {
            device.findObject(By.desc("Pause"))
        }
        assertNotNull("Playback did not start (Pause button never appeared)", pauseBtn)

        // ── Step 10: Turn off WiFi ───────────────────────────────────────────
        device.executeShellCommand("svc wifi disable")
        Thread.sleep(LONG_WAIT_MS)    // give the system a moment to drop the connection

        // ── Step 11: Verify song is still playing with WiFi off ──────────────
        val pauseStillVisible = device.findObject(By.desc("Pause"))
        assertTrue(
            "Song stopped playing after WiFi was disabled — offline playback FAILED",
            pauseStillVisible != null
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Poll [block] every 500 ms until it returns a non-null value or [timeoutMs] elapses.
     */
    private fun <T> waitUntilFound(timeoutMs: Long, block: () -> T?): T? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = block()
            if (result != null) return result
            Thread.sleep(500)
        }
        return null
    }

    /**
     * Return the search-result Card whose merged text contains both "T.N.T" and
     * "AC" (from AC/DC).
     *
     * In Compose with mergeDescendants, the Card node collects all descendant
     * text into one node. If UIAutomator returns a Text child node instead, we
     * walk up to find the enclosing card-level node that also holds the
     * download button.
     *
     * Tries scrolling down once if the item is not visible on the first attempt.
     */
    private fun findAcdcTntCard(): UiObject2? {
        fun search(): UiObject2? {
            for (node in device.findObjects(By.textContains("T.N.T")) ?: emptyList()) {
                val nodeText = node.text ?: ""
                // Case 1: merged card node — its text includes artist name too
                if (nodeText.replace("/", "").contains("ACDC", ignoreCase = true)) {
                    return node
                }
                // Case 2: text-only child node — walk up to find a parent that
                // also exposes the download button.
                var parent: UiObject2? = node.parent
                for (i in 0 until 5) {
                    val p = parent ?: break
                    val parentText = p.text ?: ""
                    if (parentText.replace("/", "").contains("ACDC", ignoreCase = true)) {
                        return p
                    }
                    if (p.findObject(By.desc("Download for offline")) != null) {
                        return p
                    }
                    parent = p.parent
                }
            }
            return null
        }

        search()?.let { return it }

        // Scroll down a bit and retry
        device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.4f)
        device.waitForIdle()
        return search()
    }
}
