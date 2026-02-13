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

package net.velcore.unifiedcontacts.domain.util.validator


object WebsiteValidator {

    fun validate(url: String): ValidationResult {
        val trimmed = url.trim()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Website cannot be empty.")
        }

        return try {
            val uri = java.net.URI(trimmed)

            if (uri.scheme.isNullOrBlank()) {
                ValidationResult.Invalid("No protocol given (z.B. https://).")
            }
            else if (uri.host.isNullOrBlank()) {
                ValidationResult.Invalid("Invalid Host.")
            }
            else {
                ValidationResult.Valid
            }

        } catch (e: Exception) {
            ValidationResult.Invalid("Invalid URL.")
        }
    }
}
