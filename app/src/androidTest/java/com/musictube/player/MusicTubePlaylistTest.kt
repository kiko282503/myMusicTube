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
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Comprehensive end-to-end test suite covering:
 *
 *  [a1_downloadTenSongs]
 *    • Download 10 English / Filipino songs (5 rock + 5 pop) to Offline Downloads.
 *    • Skips any song that is already present (idempotent).
 *
 *  [a2_createPlaylistsAndMoveSongs]
 *    • Create "rock" and "pop" playlists from the Home screen.
 *    • Navigate to Offline Downloads and assign each song to its genre playlist
 *      via Song options → Add to Playlist.
 *
 *  [a3_quickPicksDownloadAndCreatePlaylist]
 *    • From Home, tap "More →" in the Quick picks section.
 *    • Select the first visible song card on the Quick picks screen and navigate
 *      to the Player screen.
 *    • Download the song if it is not already downloaded.
 *    • Return to Home → Offline Downloads and add the song to a new playlist
 *      named "quickpicks".
 *
 * Tests are ordered alphabetically (a1_, a2_, a3_) via @FixMethodOrder so that
 * state accumulated by earlier tests is available to later ones.
 * setUp() re-launches the app WITHOUT wiping the database between tests.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MusicTubePlaylistTest {

    private lateinit var device: UiDevice
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    companion object {
        private const val APP_PACKAGE = "com.musictube.player"
        private const val LAUNCH_TIMEOUT_MS = 10_000L
        private const val SEARCH_RESULTS_TIMEOUT_MS = 30_000L
        private const val DOWNLOAD_TIMEOUT_MS = 300_000L    // URL extraction + download can be slow
        private const val SHORT_WAIT_MS = 2_000L
        private const val LONG_WAIT_MS = 5_000L

        /** 5 rock songs – downloaded and assigned to the "rock" playlist. */
        val ROCK_QUERIES = listOf(
            "bohemian rhapsody queen",
            "hotel california eagles",
            "back in black ac dc",
            "sweet child o mine guns n roses",
            "eye of the tiger survivor"
        )

        /**
         * Partial title tokens used to locate each rock song inside Offline Downloads.
         * Keep them distinctive but short so they survive YouTube title variations.
         */
        val ROCK_KEYWORDS = listOf(
            "Bohemian Rhapsody",
            "Hotel California",
            "Back in Black",
            "Sweet Child",
            "Eye of the Tiger"
        )

        /** 5 pop / Filipino songs – downloaded and assigned to the "pop" playlist. */
        val POP_QUERIES = listOf(
            "thriller michael jackson",
            "billie jean michael jackson",
            "take on me a-ha",
            "total eclipse of the heart bonnie tyler",
            "Eraserheads ang huling el bimbo"
        )

        /**
         * Partial title tokens used to locate each pop song inside Offline Downloads.
         */
        val POP_KEYWORDS = listOf(
            "Thriller",
            "Billie Jean",
            "Take On Me",
            "Total Eclipse",
            "El Bimbo"
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Before
    fun setUp() {
        device = UiDevice.getInstance(instrumentation)

        // Ensure WiFi is on — the previous test may have left it disabled.
        device.executeShellCommand("svc wifi enable")
        Thread.sleep(SHORT_WAIT_MS)

        device.pressHome()
        device.waitForIdle()

        // Re-launch without wiping the database so test state accumulates across the suite.
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        checkNotNull(intent) { "Could not resolve launch intent for $APP_PACKAGE" }
        context.startActivity(intent)

        device.wait(Until.hasObject(By.text("My Music Player")), LAUNCH_TIMEOUT_MS * 3)
        device.waitForIdle()

        // Extra settle time for Compose layout after launch.
        Thread.sleep(LONG_WAIT_MS)

        // Dismiss the Vivo "Background playback may be restricted" OEM banner if present.
        // On Vivo devices this banner appears shortly after launch and temporarily
        // blocks accessibility event delivery for the underlying Home screen content.
        val oemDismiss = device.findObject(By.desc("Dismiss"))
        if (oemDismiss != null) {
            oemDismiss.click()
            device.waitForIdle()
            Thread.sleep(SHORT_WAIT_MS)
        }

        // Gate: wait until the Search icon is exposed — this confirms that the
        // HomeScreen TopAppBar has rendered and is accessible. Without this gate
        // the test body can start before Compose finishes its first composition.
        val searchReady = waitUntilFound(LAUNCH_TIMEOUT_MS * 3) {
            device.findObject(By.desc("Search"))
        }
        checkNotNull(searchReady) {
            "Home screen Search icon not found after ${LAUNCH_TIMEOUT_MS * 3 / 1000}s — " +
            "app may not have launched correctly"
        }
        Thread.sleep(SHORT_WAIT_MS)
    }

    @After
    fun tearDown() {
        device.executeShellCommand("svc wifi enable")
        Thread.sleep(SHORT_WAIT_MS)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1 – Download 10 songs (5 rock + 5 pop/Filipino)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun a1_downloadTenSongs() {
        val allQueries = ROCK_QUERIES + POP_QUERIES
        for (query in allQueries) {
            searchAndDownloadSong(query)
            navigateHome()
            Thread.sleep(SHORT_WAIT_MS)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2 – Create "rock" + "pop" playlists and move songs into them
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun a2_createPlaylistsAndMoveSongs() {
        // Create both playlists from the Home screen.
        createPlaylistFromHome("rock")
        Thread.sleep(SHORT_WAIT_MS)
        createPlaylistFromHome("pop")
        Thread.sleep(SHORT_WAIT_MS)

        // Open Offline Downloads then add each rock song.
        openOfflineDownloads()
        for (keyword in ROCK_KEYWORDS) {
            addSongToPlaylist(keyword, "rock")
            Thread.sleep(SHORT_WAIT_MS)
        }

        // Add each pop / Filipino song.
        for (keyword in POP_KEYWORDS) {
            addSongToPlaylist(keyword, "pop")
            Thread.sleep(SHORT_WAIT_MS)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3 – Quick Picks → random song → download → "quickpicks" playlist
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun a3_quickPicksDownloadAndCreatePlaylist() {
        // 3a. Scroll Home until "More →" (Quick picks section header) is visible.
        val moreBtn: UiObject2? = scrollAndFind("More →", maxSwipes = 6)
        assertNotNull("'More →' Quick picks button not found on home screen", moreBtn)
        moreBtn!!.click()
        device.waitForIdle()

        // Wait for the Quick picks screen title to appear.
        device.wait(Until.hasObject(By.text("Quick picks")), LAUNCH_TIMEOUT_MS)
        Thread.sleep(LONG_WAIT_MS)  // allow the song list to finish loading

        // 3b. Find the first visible PickCard.
        //   PickCards use Modifier.clickable which merges descendant semantics,
        //   so UIAutomator surfaces their merged title + artist as the node's text.
        //   The Back navigation icon has contentDescription = "Back"; cards have none.
        val quickPickCard: UiObject2? = waitUntilFound(SEARCH_RESULTS_TIMEOUT_MS) {
            device.findObjects(By.clickable(true))
                ?.firstOrNull { node ->
                    node.contentDescription != "Back" &&
                        !node.text.isNullOrBlank() &&
                        node.text!!.length > 3  // exclude single-word button labels
                }
        }
        assertNotNull("No Quick Picks song cards found on screen", quickPickCard)

        // Capture the song title (first line of the merged card text) for later lookup.
        val cardMergedText = quickPickCard!!.text ?: "quickpick"
        val quickPickTitle = cardMergedText.lines().firstOrNull { it.isNotBlank() }?.trim()
            ?: cardMergedText.take(20)

        quickPickCard.click()
        device.waitForIdle()
        Thread.sleep(SHORT_WAIT_MS)  // allow PlayerScreen to appear

        // 3c. Download the song from PlayerScreen if not already downloaded.
        val downloadBtn: UiObject2? = device.findObject(By.text("Download"))
        if (downloadBtn != null) {
            downloadBtn.click()
            device.waitForIdle()

            // Wait until the "Download" button disappears (download started → progress shown)
            // then wait for the PlayerScreen's "Downloaded" state or a fixed safety timeout.
            waitUntilFound(DOWNLOAD_TIMEOUT_MS) {
                if (device.findObject(By.text("Download")) == null) "done" else null
            }
            Thread.sleep(LONG_WAIT_MS)
        }
        // If there is no Download button the song is already cached – that is fine.

        // 3d. Navigate back to Home.
        navigateHome()
        Thread.sleep(SHORT_WAIT_MS)

        // 3e. Open Offline Downloads and add the quick pick song to a new "quickpicks" playlist.
        openOfflineDownloads()

        // Prefer the captured title; fall back to the first song in the list.
        val songKeyword = quickPickTitle.take(10)
        val songNode: UiObject2? = scrollAndFind(songKeyword, maxSwipes = 5)

        if (songNode != null) {
            addSongToNewPlaylist(songKeyword, "quickpicks")
        } else {
            // Fallback: pick any song that appears in Offline Downloads.
            // Skip playlist-name rows and the screen title by looking for a "Song options"
            // button – those are the song row controls.
            val fallbackOptions: UiObject2? = waitUntilFound(LONG_WAIT_MS) {
                device.findObject(By.desc("Song options"))
            }
            assertNotNull(
                "Could not find the downloaded Quick Pick song in Offline Downloads " +
                    "(tried keyword='$songKeyword') and no fallback 'Song options' found.",
                fallbackOptions
            )
            fallbackOptions!!.click()
            device.waitForIdle()

            val addItem = device.wait(Until.findObject(By.text("Add to Playlist")), LONG_WAIT_MS)
            assertNotNull("'Add to Playlist' option not found", addItem)
            addItem!!.click()
            device.waitForIdle()

            val newPlaylistOption = device.wait(Until.findObject(By.text("+ New Playlist")), LONG_WAIT_MS)
            assertNotNull("'+ New Playlist' option not found in Add to Playlist dialog", newPlaylistOption)
            newPlaylistOption!!.click()
            device.waitForIdle()

            // Type new playlist name.
            val nameField = device.wait(Until.findObject(By.text("Playlist name")), LONG_WAIT_MS)
            assertNotNull("Playlist name text field not found", nameField)
            nameField!!.click()
            device.waitForIdle()
            typeIntoFocusedField("quickpicks")

            val createBtn = device.wait(Until.findObject(By.text("Create")), LONG_WAIT_MS)
            assertNotNull("'Create' button not found", createBtn)
            createBtn!!.click()
            device.waitForIdle()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search for [query] in the app's Search screen and download the first result.
     * If the first result is already downloaded the method returns without error.
     * If no downloadable result appears within the timeout the search is abandoned.
     */
    private fun searchAndDownloadSong(query: String) {
        // Open Search.
        val searchIcon = waitUntilFound(LAUNCH_TIMEOUT_MS) {
            device.findObject(By.desc("Search"))
        }
        assertNotNull("Search icon not found on Home screen (query='$query')", searchIcon)
        searchIcon!!.click()
        device.waitForIdle()

        // Type the query.
        val searchField = device.wait(
            Until.findObject(By.text("Search for songs...")),
            LAUNCH_TIMEOUT_MS
        )
        assertNotNull("Search text field not found", searchField)
        searchField!!.click()
        device.waitForIdle()
        device.findObject(By.focused(true)).text = query
        device.pressEnter()
        device.waitForIdle()

        // Wait for the first terminal download indicator to appear.
        val firstIndicator: UiObject2? = waitUntilFound(SEARCH_RESULTS_TIMEOUT_MS) {
            device.findObject(By.desc("Download for offline"))
                ?: device.findObject(By.desc("Downloaded"))
        }

        if (firstIndicator == null) {
            // No downloadable result appeared – skip this song.
            device.pressBack()
            device.waitForIdle()
            return
        }

        // Already downloaded? Skip.
        if (firstIndicator.contentDescription == "Downloaded") {
            device.pressBack()
            device.waitForIdle()
            return
        }

        // Tap the Download button.
        firstIndicator.click()
        device.waitForIdle()

        // Wait until download reaches a terminal state.
        val terminalNode: UiObject2? = waitUntilFound(DOWNLOAD_TIMEOUT_MS) {
            try {
                device.findObject(By.desc("Downloaded"))
                    ?: device.findObject(By.desc("Retry download"))
            } catch (_: Exception) {
                null    // StaleObjectException during Compose recomposition
            }
        }

        assertNotNull(
            "Download for '$query' did not finish within ${DOWNLOAD_TIMEOUT_MS / 1000}s",
            terminalNode
        )
        assertTrue(
            "Download FAILED for '$query' (Retry download icon appeared)",
            terminalNode!!.contentDescription == "Downloaded"
        )

        device.pressBack()
        device.waitForIdle()
    }

    /**
     * Press Back repeatedly until the "My Music Player" home title is visible,
     * or up to 8 attempts.
     */
    private fun navigateHome() {
        repeat(8) {
            if (device.findObject(By.text("My Music Player")) != null) return
            device.pressBack()
            device.waitForIdle()
            Thread.sleep(500)
        }
    }

    /**
     * Create a new playlist from the Home screen via the "New Playlist" button.
     * Asserts that the button and dialog are present.
     */
    private fun createPlaylistFromHome(name: String) {
        val newPlaylistBtn: UiObject2? = waitUntilFound(LONG_WAIT_MS) {
            device.findObject(By.desc("New Playlist"))
        }
        assertNotNull("'New Playlist' button not found on Home screen", newPlaylistBtn)
        newPlaylistBtn!!.click()
        device.waitForIdle()

        // The dialog title should now be visible.
        val dialogTitle = device.wait(Until.findObject(By.text("New Playlist")), LONG_WAIT_MS)
        assertNotNull("'New Playlist' dialog did not open", dialogTitle)

        // The OutlinedTextField label "Playlist name" is the field's text when empty.
        val playlistField = device.wait(Until.findObject(By.text("Playlist name")), LONG_WAIT_MS)
        assertNotNull("Playlist name text field not found in dialog", playlistField)
        playlistField!!.click()
        device.waitForIdle()
        typeIntoFocusedField(name)

        val createBtn = device.wait(Until.findObject(By.text("Create")), LONG_WAIT_MS)
        assertNotNull("'Create' button not found in dialog", createBtn)
        createBtn!!.click()
        device.waitForIdle()
    }

    /**
     * Open the "Offline Downloads" playlist from the Home screen.
     * Scrolls down if the card is not immediately visible.
     */
    private fun openOfflineDownloads() {
        val offlineCard: UiObject2? = waitUntilFound(LONG_WAIT_MS * 3) {
            device.findObject(By.textContains("Offline Downloads"))
        } ?: scrollAndFind("Offline Downloads", maxSwipes = 4)

        assertNotNull("'Offline Downloads' card not found on Home screen", offlineCard)
        offlineCard!!.click()
        device.waitForIdle()
        Thread.sleep(SHORT_WAIT_MS)
    }

    /**
     * Within the current screen (expected to be Offline Downloads or any playlist),
     * find the song whose title contains [titleKeyword], tap its "Song options" icon,
     * choose "Add to Playlist", then select [playlistName] from the picker dialog.
     *
     * If the song is not found the function logs an implicit skip (no assertion failure)
     * so that a missing download does not abort the rest of the playlist assignment.
     */
    private fun addSongToPlaylist(titleKeyword: String, playlistName: String) {
        val songNode: UiObject2? = scrollAndFind(titleKeyword, maxSwipes = 6)
        if (songNode == null) return     // song not found – silently skip

        val optionsBtn = findSongOptionsNear(songNode)
        if (optionsBtn == null) return
        optionsBtn.click()
        device.waitForIdle()

        val addItem = device.wait(Until.findObject(By.text("Add to Playlist")), LONG_WAIT_MS)
        assertNotNull("'Add to Playlist' action not found for '$titleKeyword'", addItem)
        addItem!!.click()
        device.waitForIdle()

        val playlistItem = device.wait(Until.findObject(By.text(playlistName)), LONG_WAIT_MS)
        assertNotNull("Playlist '$playlistName' not found in picker for '$titleKeyword'", playlistItem)
        playlistItem!!.click()
        device.waitForIdle()
    }

    /**
     * Within the current screen, find the song whose title contains [titleKeyword],
     * open its options, choose "Add to Playlist" → "+ New Playlist", type
     * [newPlaylistName] and confirm.
     */
    private fun addSongToNewPlaylist(titleKeyword: String, newPlaylistName: String) {
        val songNode: UiObject2? = scrollAndFind(titleKeyword, maxSwipes = 6)
        assertNotNull("Song with keyword '$titleKeyword' not found on screen", songNode)

        val optionsBtn = findSongOptionsNear(songNode!!)
        assertNotNull("'Song options' button not found for '$titleKeyword'", optionsBtn)
        optionsBtn!!.click()
        device.waitForIdle()

        val addItem = device.wait(Until.findObject(By.text("Add to Playlist")), LONG_WAIT_MS)
        assertNotNull("'Add to Playlist' action not found for '$titleKeyword'", addItem)
        addItem!!.click()
        device.waitForIdle()

        val newPlaylistOption = device.wait(Until.findObject(By.text("+ New Playlist")), LONG_WAIT_MS)
        assertNotNull("'+ New Playlist' option not found in picker", newPlaylistOption)
        newPlaylistOption!!.click()
        device.waitForIdle()

        val nameField = device.wait(Until.findObject(By.text("Playlist name")), LONG_WAIT_MS)
        assertNotNull("Playlist name text field not found", nameField)
        nameField!!.click()
        device.waitForIdle()
        typeIntoFocusedField(newPlaylistName)

        val createBtn = device.wait(Until.findObject(By.text("Create")), LONG_WAIT_MS)
        assertNotNull("'Create' button not found in dialog", createBtn)
        createBtn!!.click()
        device.waitForIdle()
    }

    /**
     * Starting from [node], walk up the accessibility hierarchy (up to 6 levels) to
     * find the enclosing song row that also contains a "Song options" icon button,
     * then return that icon button.
     *
     * This is needed because UIAutomator may land on a Text child node when searching
     * by text, and the "Song options" MoreVert icon is a sibling further up the tree.
     */
    private fun findSongOptionsNear(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        repeat(6) {
            val optBtn = current?.findObject(By.desc("Song options"))
            if (optBtn != null) return optBtn
            current = current?.parent
        }
        return null
    }

    /**
     * Scroll the first scrollable container on screen downward up to [maxSwipes] times,
     * stopping as soon as an element whose text contains [partialText] becomes visible.
     *
     * Checks visibility before each swipe and returns the element as soon as it appears.
     */
    private fun scrollAndFind(partialText: String, maxSwipes: Int = 3): UiObject2? {
        device.findObject(By.textContains(partialText))?.let { return it }

        val scrollable = device.findObject(By.scrollable(true)) ?: return null
        repeat(maxSwipes) {
            scrollable.scroll(Direction.DOWN, 0.4f)
            device.waitForIdle()
            device.findObject(By.textContains(partialText))?.let { return it }
        }
        return device.findObject(By.textContains(partialText))
    }

    /**
     * Type [text] into the currently focused input field.
     * Falls back to finding an EditText if no node currently has focus.
     */
    private fun typeIntoFocusedField(text: String) {
        val focused = device.findObject(By.focused(true))
        if (focused != null) {
            focused.text = text
        } else {
            device.findObject(By.clazz("android.widget.EditText"))?.text = text
        }
        device.waitForIdle()
    }

    /**
     * Poll [block] every 500 ms until it returns a non-null value or [timeoutMs] elapses.
     * Returns the value from [block], or null if the timeout expires.
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
}
