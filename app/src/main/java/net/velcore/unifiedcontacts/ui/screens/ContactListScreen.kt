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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import net.velcore.unifiedcontacts.R
import net.velcore.unifiedcontacts.data.repository.ContactsRepository

@Composable
fun ContactListScreen(
    repository: ContactsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
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
            repository.getAllContactNames()
                .filter { it.isNotBlank() }
                .sortedBy { it.lowercase() }
                .let { sortedNames ->
                    withContext(Dispatchers.Default) {
                        buildContactListUiData(sortedNames)
                    }
                }
        }
        isLoading = false
    }

    fun refreshContacts(forceRefresh: Boolean) {
        scope.launch {
            isRefreshing = true
            contactListUiData = withContext(Dispatchers.IO) {
                repository.getAllContactNames(forceRefresh = forceRefresh)
                    .filter { it.isNotBlank() }
                    .sortedBy { it.lowercase() }
                    .let { sortedNames ->
                        withContext(Dispatchers.Default) {
                            buildContactListUiData(sortedNames)
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
            listState = listState,
            isRefreshing = isRefreshing,
            onRefresh = { refreshContacts(forceRefresh = true) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshableContactList(
    uiData: ContactListUiData,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        AlphabeticalContactList(
            uiData = uiData,
            listState = listState,
        )
    }
}

@Composable
private fun AlphabeticalContactList(
    uiData: ContactListUiData,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp),
            state = listState,
        ) {
            itemsIndexed(
                items = uiData.rows,
                key = { index, row ->
                    when (row) {
                        is ContactRow.Header -> "header_${row.letter}"
                        is ContactRow.Item -> "item_${index}_${row.name}"
                    }
                },
                contentType = { _, row ->
                    when (row) {
                        is ContactRow.Header -> "header"
                        is ContactRow.Item -> "item"
                    }
                },
            ) { _, row ->
                when (row) {
                    is ContactRow.Header -> Text(
                        text = row.letter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    is ContactRow.Item -> Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                }
            }
        }

        AlphabetSlider(
            modifier = Modifier.align(Alignment.CenterEnd),
            sections = uiData.sections,
            firstIndexByLetter = uiData.firstIndexByLetter,
            listState = listState,
        )
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun AlphabetSlider(
    modifier: Modifier = Modifier,
    sections: List<Char>,
    firstIndexByLetter: Map<Char, Int>,
    listState: LazyListState,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val alphabet = remember { ('A'..'Z').toList() }
    var activeHeightPx by remember { mutableStateOf(0f) }
    var currentLetter by remember { mutableStateOf<Char?>(null) }
    var clearHighlightJob by remember { mutableStateOf<Job?>(null) }
    var lastScrollIndex by remember { mutableStateOf(-1) }
    var lastHapticAtMs by remember { mutableStateOf(0L) }
    var pendingScrollTarget by remember { mutableStateOf<Int?>(null) }
    val bufferPercent = SIDEBAR_BUFFER_PERCENT.coerceIn(0, 49)
    val topBottomWeight = bufferPercent.toFloat()
    val activeWeight = (100 - (bufferPercent * 2)).toFloat()
    val sectionsSet = remember(sections) { sections.toSet() }

    LaunchedEffect(listState) {
        snapshotFlow { pendingScrollTarget }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(SIDEBAR_SCROLL_DEBOUNCE_MS)
            .collectLatest { target ->
                if (listState.firstVisibleItemIndex != target) {
                    listState.scrollToItem(target)
                }
            }
    }

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
        val rawIndex = ((touch.y / activeHeightPx) * alphabet.size).toInt()
        val touchedIndex = rawIndex.coerceIn(0, alphabet.lastIndex)
        val targetLetter = nearestAvailableLetter(
            startIndex = touchedIndex,
            alphabet = alphabet,
            available = sections.toSet(),
        ) ?: return
        if (currentLetter != targetLetter) emitTickHaptic()
        currentLetter = targetLetter
        val target = firstIndexByLetter[targetLetter] ?: return
        if (target == lastScrollIndex) return
        lastScrollIndex = target
        pendingScrollTarget = target
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(topBottomWeight))

            Box(
                modifier = Modifier
                    .weight(activeWeight)
                    .pointerInput(sections, firstIndexByLetter) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                jumpByTouch(offset)
                            },
                            onDragEnd = {
                                lastScrollIndex = -1
                                pendingScrollTarget = null
                                clearHighlightJob?.cancel()
                                clearHighlightJob = scope.launch {
                                    delay(SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS)
                                    currentLetter = null
                                }
                            },
                            onDragCancel = {
                                lastScrollIndex = -1
                                pendingScrollTarget = null
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
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .onSizeChanged { activeHeightPx = it.height.toFloat() },
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val selectedIndex = currentLetter?.let(alphabet::indexOf) ?: -1
                    val rowHeightPx = if (activeHeightPx > 0f) activeHeightPx / alphabet.size else 0f
                    val baseFontPx = with(density) { dynamicTextSize.toPx() }
                    val maxSafeScale = if (rowHeightPx > 0f && baseFontPx > 0f) {
                        ((rowHeightPx * 0.82f) / baseFontPx).coerceIn(1f, 1.35f)
                    } else {
                        1.2f
                    }
                    alphabet.forEachIndexed { index, letter ->
                        val visual = remember(letter, selectedIndex, maxSafeScale, sectionsSet) {
                            computeLetterVisual(
                                letter = letter,
                                selectedIndex = selectedIndex,
                                index = index,
                                maxSafeScale = maxSafeScale,
                                enabled = sectionsSet.contains(letter),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            SidebarLetter(
                                letter = letter,
                                visual = visual,
                                currentLetter = currentLetter,
                                fontSize = dynamicTextSize,
                                onClick = {
                                    clearHighlightJob?.cancel()
                                    val target = firstIndexByLetter[letter] ?: return@SidebarLetter
                                    lastScrollIndex = target
                                    if (currentLetter != letter) emitTickHaptic()
                                    currentLetter = letter
                                    pendingScrollTarget = target
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
    data class Header(val letter: Char) : ContactRow
    data class Item(val name: String) : ContactRow
}

private data class ContactListUiData(
    val sections: List<Char>,
    val rows: List<ContactRow>,
    val firstIndexByLetter: Map<Char, Int>,
)

@Immutable
private data class LetterVisual(
    val enabled: Boolean,
    val targetScale: Float,
    val leftShiftDp: Float,
    val proximity: Float,
)

private fun computeLetterVisual(
    letter: Char,
    selectedIndex: Int,
    index: Int,
    maxSafeScale: Float,
    enabled: Boolean,
): LetterVisual {
    val distance = if (selectedIndex < 0) Int.MAX_VALUE else abs(index - selectedIndex)
    val targetScaleRaw = when {
        selectedIndex < 0 -> 1f
        distance == 0 -> 2.25f
        distance == 1 -> 1.72f
        distance == 2 -> 1.40f
        distance == 3 -> 1.18f
        distance == 4 -> 1.06f
        else -> 1f
    }
    val leftShiftDp = if (selectedIndex < 0) {
        0f
    } else {
        val sigma = 1.8f
        val normalized = exp(-((distance * distance).toFloat()) / (2f * sigma * sigma))
        -40f * normalized
    }
    val proximity = if (selectedIndex < 0) {
        0f
    } else {
        (1f - (distance / 5f)).coerceIn(0f, 1f)
    }
    return LetterVisual(
        enabled = enabled,
        targetScale = minOf(targetScaleRaw, maxSafeScale),
        leftShiftDp = leftShiftDp,
        proximity = proximity,
    )
}

@Composable
private fun SidebarLetter(
    letter: Char,
    visual: LetterVisual,
    currentLetter: Char?,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val animatedScale by animateFloatAsState(
        targetValue = visual.targetScale,
        animationSpec = tween(durationMillis = 90),
        label = "sidebarLetterScale",
    )
    val displayColor = when {
        !visual.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        currentLetter != null -> lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.primary,
            visual.proximity,
        )
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = letter.toString(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = fontSize,
            fontWeight = if (currentLetter == letter) FontWeight.Bold else FontWeight.Normal,
        ),
        color = displayColor,
        modifier = Modifier
            .offset(x = visual.leftShiftDp.dp)
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale,
            )
            .clickable(
                enabled = visual.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

private fun buildContactListUiData(names: List<String>): ContactListUiData {
    val grouped = names.groupBy { it.first().uppercaseChar() }.toSortedMap()
    val rows = buildList {
        grouped.forEach { (letter, entries) ->
            add(ContactRow.Header(letter))
            entries.forEach { add(ContactRow.Item(it)) }
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

private const val SIDEBAR_BUFFER_PERCENT = 10
private const val SIDEBAR_HIGHLIGHT_CLEAR_DELAY_MS = 120L
private const val SIDEBAR_HAPTIC_MIN_INTERVAL_MS = 35L
private const val SIDEBAR_SCROLL_DEBOUNCE_MS = 16L
