/*
 * Copyright (c) 2026 VinVel
 * SPDX-License-Identifier: MPL-2.0
 * Incompatible With Secondary Licenses.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: unifiedcontacts.velcore.net
 */

package net.velcore.unifiedcontacts.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.velcore.unifiedcontacts.R
import net.velcore.unifiedcontacts.data.repository.ContactsRepository

@Composable
fun ContactListScreen(
    repository: ContactsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var contactListUiData by remember { mutableStateOf<ContactListUiData?>(null) }
    var requestPermissionNow by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(requestPermissionNow) {
        if (requestPermissionNow) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            requestPermissionNow = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        isLoading = true
        contactListUiData = withContext(Dispatchers.IO) {
            repository.getAllContactNames().let { names ->
                withContext(Dispatchers.Default) {
                    buildContactListUiData(names)
                }
            }
        }
        isLoading = false
    }

    fun refreshContacts(forceRefresh: Boolean) {
        scope.launch {
            isRefreshing = true
            contactListUiData = withContext(Dispatchers.IO) {
                repository.getAllContactNames(forceRefresh = forceRefresh).let { names ->
                    withContext(Dispatchers.Default) {
                        buildContactListUiData(names)
                    }
                }
            }
            isRefreshing = false
        }
    }

    when {
        !hasPermission -> PermissionRequired(
            onRequestPermission = { requestPermissionNow = true },
        )
        isLoading || contactListUiData == null -> LoadingState()
        else -> RefreshableContactList(
            uiData = contactListUiData!!,
            isRefreshing = isRefreshing,
            onRefresh = { refreshContacts(forceRefresh = true) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshableContactList(
    uiData: ContactListUiData,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        AlphabeticalContactList(uiData = uiData)
    }
}

@Composable
private fun AlphabeticalContactList(
    uiData: ContactListUiData,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val recyclerAdapter = remember { ContactRowsAdapter() }
    var recyclerView by remember { mutableStateOf<RecyclerView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = recyclerAdapter
                    setHasFixedSize(true)
                    itemAnimator = null
                    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    clipToPadding = false
                    setItemViewCacheSize(40)
                    setPadding(0, 0, with(density) { 44.dp.roundToPx() }, 0)
                    recyclerView = this
                }
            },
            update = {
                recyclerAdapter.submitList(uiData.rows)
                recyclerView = it
            },
        )

        AlphabetSlider(
            modifier = Modifier.align(Alignment.CenterEnd),
            sections = uiData.sections,
            firstIndexByLetter = uiData.firstIndexByLetter,
            onJump = { position ->
                recyclerView?.scrollToPosition(position)
            },
        )
    }
}

@Composable
private fun AlphabetSlider(
    modifier: Modifier = Modifier,
    sections: List<Char>,
    firstIndexByLetter: Map<Char, Int>,
    onJump: (Int) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val alphabet = remember { ('A'..'Z').toList() }
    var activeHeightPx by remember { mutableStateOf(0f) }
    var currentLetter by remember { mutableStateOf<Char?>(null) }
    var currentTouchYPx by remember { mutableStateOf(0f) }
    var clearHighlightJob by remember { mutableStateOf<Job?>(null) }
    var lastJumpIndex by remember { mutableStateOf(-1) }
    var lastHapticAtMs by remember { mutableStateOf(0L) }
    val bufferPercent = SIDEBAR_BUFFER_PERCENT.coerceIn(0, 49)
    val topBottomWeight = bufferPercent.toFloat()
    val activeWeight = (100 - (bufferPercent * 2)).toFloat()
    val sectionsSet = remember(sections) { sections.toSet() }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun emitTickHaptic() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHapticAtMs < SIDEBAR_HAPTIC_MIN_INTERVAL_MS) return
        lastHapticAtMs = now
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    24,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                ),
            )
        } catch (_: SecurityException) {
            // Ignore if direct vibrator access is not permitted on this device.
        }
    }

    fun jumpByTouch(touch: Offset) {
        clearHighlightJob?.cancel()
        if (activeHeightPx <= 0f) return
        currentTouchYPx = touch.y.coerceIn(0f, activeHeightPx)
        val rawIndex = ((currentTouchYPx / activeHeightPx) * alphabet.size).toInt()
        val touchedIndex = rawIndex.coerceIn(0, alphabet.lastIndex)
        val targetLetter = nearestAvailableLetter(
            startIndex = touchedIndex,
            alphabet = alphabet,
            available = sectionsSet,
        ) ?: return
        if (currentLetter != targetLetter) emitTickHaptic()
        currentLetter = targetLetter
        val target = firstIndexByLetter[targetLetter] ?: return
        if (target == lastJumpIndex) return
        lastJumpIndex = target
        onJump(target)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(74.dp)
            .padding(end = 8.dp),
    ) {
        val dynamicTextSize = with(density) {
            if (activeHeightPx > 0f) {
                val candidate = ((activeHeightPx / alphabet.size) * 0.72f).toSp()
                when {
                    candidate.value < 6f -> 6.sp
                    candidate.value > 12f -> 12.sp
                    else -> candidate
                }
            } else {
                10.sp
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.weight(topBottomWeight))

            Box(
                modifier = Modifier
                    .weight(activeWeight)
                    .width(74.dp)
                    .pointerInput(sections, firstIndexByLetter) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                jumpByTouch(offset)
                            },
                            onDragEnd = {
                                lastJumpIndex = -1
                                clearHighlightJob?.cancel()
                                clearHighlightJob = scope.launch {
                                    delay(SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS)
                                    currentLetter = null
                                }
                            },
                            onDragCancel = {
                                lastJumpIndex = -1
                                clearHighlightJob?.cancel()
                                clearHighlightJob = scope.launch {
                                    delay(SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS)
                                    currentLetter = null
                                }
                            },
                            onDrag = { change, _ ->
                                jumpByTouch(change.position)
                            },
                        )
                    },
            ) {
                currentLetter?.let { selected ->
                    val bubbleSizePx = with(density) { SIDEBAR_BUBBLE_SIZE_DP.dp.toPx() }
                    val halfBubblePx = bubbleSizePx / 2f
                    val bubbleYPx = (currentTouchYPx - halfBubblePx)
                        .coerceIn(
                            0f,
                            (activeHeightPx - bubbleSizePx).coerceAtLeast(0f),
                        )
                    Box(
                        modifier = Modifier
                            .offset(
                                x = SIDEBAR_BUBBLE_OFFSET_X_DP.dp,
                                y = with(density) { bubbleYPx.toDp() },
                            )
                            .align(Alignment.TopStart)
                            .width(SIDEBAR_BUBBLE_SIZE_DP.dp)
                            .height(SIDEBAR_BUBBLE_SIZE_DP.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape((SIDEBAR_BUBBLE_SIZE_DP / 2f).dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = selected.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        .align(Alignment.CenterEnd)
                        .onSizeChanged { activeHeightPx = it.height.toFloat() },
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    alphabet.forEach { letter ->
                        val enabled = sectionsSet.contains(letter)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dynamicTextSize),
                                color = when {
                                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    currentLetter == letter -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.clickable(enabled = enabled) {
                                    clearHighlightJob?.cancel()
                                    val target = firstIndexByLetter[letter] ?: return@clickable
                                    lastJumpIndex = target
                                    if (currentLetter != letter) emitTickHaptic()
                                    currentLetter = letter
                                    val idx = alphabet.indexOf(letter).coerceAtLeast(0)
                                    currentTouchYPx = (activeHeightPx / alphabet.size) * (idx + 0.5f)
                                    onJump(target)
                                    clearHighlightJob = scope.launch {
                                        delay(SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS)
                                        currentLetter = null
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(topBottomWeight))
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionRequired(
    onRequestPermission: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.contacts_permission_rationale),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRequestPermission) {
                Text(text = stringResource(R.string.contacts_permission_request_button))
            }
        }
    }
}

private sealed interface ContactRow {
    val stableKey: Long
    val contentType: Int

    data class Header(
        val letter: Char,
        override val stableKey: Long,
    ) : ContactRow {
        override val contentType: Int = 0
    }

    data class Item(
        val name: String,
        override val stableKey: Long,
    ) : ContactRow {
        override val contentType: Int = 1
    }
}

private data class ContactListUiData(
    val sections: List<Char>,
    val rows: List<ContactRow>,
    val firstIndexByLetter: Map<Char, Int>,
)

private class ContactRowsAdapter : ListAdapter<ContactRow, RecyclerView.ViewHolder>(DiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).stableKey

    override fun getItemViewType(position: Int): Int = getItem(position).contentType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val textView = AppCompatTextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        return if (viewType == 0) {
            HeaderViewHolder(textView)
        } else {
            ItemViewHolder(textView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ContactRow.Header -> (holder as HeaderViewHolder).bind(row.letter)
            is ContactRow.Item -> (holder as ItemViewHolder).bind(row.name)
        }
    }

    private class HeaderViewHolder(
        private val textView: AppCompatTextView,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(letter: Char) {
            textView.text = letter.toString()
            textView.setTypeface(null, Typeface.BOLD)
            textView.setTextSize(18f)
            textView.setPadding(16.dpToPx(textView), 8.dpToPx(textView), 16.dpToPx(textView), 8.dpToPx(textView))
            textView.setTextColor(
                MaterialColors.getColor(
                    textView,
                    com.google.android.material.R.attr.colorOnSurface,
                ),
            )
        }
    }

    private class ItemViewHolder(
        private val textView: AppCompatTextView,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(name: String) {
            textView.text = name
            textView.setTypeface(null, Typeface.NORMAL)
            textView.setTextSize(16f)
            textView.setPadding(24.dpToPx(textView), 10.dpToPx(textView), 24.dpToPx(textView), 10.dpToPx(textView))
            textView.setTextColor(
                MaterialColors.getColor(
                    textView,
                    com.google.android.material.R.attr.colorOnSurface,
                ),
            )
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<ContactRow>() {
            override fun areItemsTheSame(oldItem: ContactRow, newItem: ContactRow): Boolean {
                return oldItem.stableKey == newItem.stableKey
            }

            override fun areContentsTheSame(oldItem: ContactRow, newItem: ContactRow): Boolean {
                return oldItem == newItem
            }
        }
    }
}

private fun buildContactListUiData(names: List<String>): ContactListUiData {
    val grouped = names.groupBy { it.first().uppercaseChar() }.toSortedMap()
    var nextKey = 0L
    val rows = buildList {
        grouped.forEach { (letter, entries) ->
            add(ContactRow.Header(letter = letter, stableKey = nextKey++))
            entries.forEach { add(ContactRow.Item(name = it, stableKey = nextKey++)) }
        }
    }
    val firstIndexByLetter = buildMap {
        rows.forEachIndexed { index, row ->
            if (row is ContactRow.Header) put(row.letter, index)
        }
    }
    return ContactListUiData(
        sections = grouped.keys.toList(),
        rows = rows,
        firstIndexByLetter = firstIndexByLetter,
    )
}

private fun nearestAvailableLetter(
    startIndex: Int,
    alphabet: List<Char>,
    available: Set<Char>,
): Char? {
    if (available.isEmpty()) return null
    var distance = 0
    while (distance <= alphabet.lastIndex) {
        val backward = startIndex - distance
        if (backward >= 0) {
            val letter = alphabet[backward]
            if (available.contains(letter)) return letter
        }
        val forward = startIndex + distance
        if (forward <= alphabet.lastIndex) {
            val letter = alphabet[forward]
            if (available.contains(letter)) return letter
        }
        distance++
    }
    return null
}

private fun Int.dpToPx(view: AppCompatTextView): Int {
    return (this * view.resources.displayMetrics.density).toInt()
}

private const val SIDEBAR_BUFFER_PERCENT = 10
private const val SIDEBAR_BUBBLE_SIZE_DP = 52
private const val SIDEBAR_BUBBLE_OFFSET_X_DP = (-16)
private const val SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS = 120L
private const val SIDEBAR_HAPTIC_MIN_INTERVAL_MS = 35L