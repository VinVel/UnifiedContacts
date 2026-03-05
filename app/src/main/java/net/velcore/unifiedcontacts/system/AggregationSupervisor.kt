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

package net.velcore.unifiedcontacts.system

class AggregationSupervisor {
    fun buildContactListUiData(
        names: List<String>,
        photoThumbnailUris: List<String?>,
    ): ContactListUiData {
        val grouped = names.groupBy { it.first().uppercaseChar() }.toSortedMap()
        var nextKey = 0L
        var itemIndex = 0
        val rows = buildList {
            grouped.forEach { (letter, entries) ->
                add(ContactRow.Header(letter = letter, stableKey = nextKey++))
                entries.forEach { name ->
                    val photoUri = photoThumbnailUris.getOrNull(itemIndex)
                    add(
                        ContactRow.Item(
                            name = name,
                            photoThumbnailUri = photoUri,
                            stableKey = nextKey++,
                        ),
                    )
                    itemIndex++
                }
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

    fun nearestAvailableLetter(
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
}

data class ContactListUiData(
    val sections: List<Char>,
    val rows: List<ContactRow>,
    val firstIndexByLetter: Map<Char, Int>,
)

sealed interface ContactRow {
    val stableKey: Long
    val contentType: Int

    data class Header(
        val letter: Char,
        override val stableKey: Long,
    ): ContactRow {
        override val contentType: Int = 0
    }

    data class Item(
        val name: String,
        val photoThumbnailUri: String?,
        override val stableKey: Long,
    ): ContactRow {
        override val contentType: Int = 1
    }
}
