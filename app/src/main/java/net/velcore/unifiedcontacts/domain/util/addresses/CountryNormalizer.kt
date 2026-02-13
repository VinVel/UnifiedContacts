/*
 * Copyright (c) 2026 VinVel
 * SPDX-License-Identifier: MPL-2.0
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: unifiedcontacts.velcore.net
 */

package net.velcore.unifiedcontacts.domain.util.addresses

import java.util.Locale

object CountryNormalizer {

    fun normalizeToIso(input: String?): String? {
        if (input.isNullOrBlank()) return null

        val normalized = input.trim().lowercase(Locale.ROOT)

        return Locale.getISOCountries().firstOrNull { iso ->
            @Suppress("DEPRECATION") val locale = Locale("", iso)
            //there is a newer way to get the locale, but it is only supported in android api 36+

            iso.lowercase(Locale.ROOT) == normalized ||
                    locale.getDisplayCountry(Locale.ENGLISH)
                        .lowercase(Locale.ROOT) == normalized ||
                    locale.getDisplayCountry(Locale.GERMAN)
                        .lowercase(Locale.ROOT) == normalized
        }
    }
}
