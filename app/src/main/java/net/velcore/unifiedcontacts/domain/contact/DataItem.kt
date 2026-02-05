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

package net.velcore.unifiedcontacts.domain.contact

import net.velcore.unifiedcontacts.domain.util.MimeTypes
import java.time.LocalDate

sealed class DataItem{
    abstract val id: Long
    abstract val mimeType: String

    data class AddressItem(
        override val id: Long,
        val street: String?,
        val pobox: String?,
        val neighborhood: String?,
        val city: String?,
        val region: String?,
        val postcode: String?,
        val country: String?,
        val type: Int,              //e.g. if the address is for home, work or others
        val label: String?,
        val formattedAddress: String?
    ): DataItem() {
        override val mimeType = MimeTypes.POSTAL_ADDRESS
    }

    data class EmailItem(
        override val id: Long,
        val address: String,
        val type: Int
    ): DataItem() {
        override val mimeType = MimeTypes.EMAIL
    }

    data class EventItem(
        override val id: Long,
        val type: String,
        val date: LocalDate,
    ): DataItem() {
        override val mimeType = MimeTypes.EVENT
    }

    data class GroupItem(
        override val id: Long,
        val groupId: Long
    ): DataItem() {
        override val mimeType = MimeTypes.GROUP
    }

    data class NameItem(
        override val id: Long,
        val givenName: String?,
        val familyName: String?,
        val middleName: String?,
        val prefix: String?,
        val suffix: String?,
        val displayName: String?,
        val phoneticGivenName: String?,
        val phoneticFamilyName: String?
    ): DataItem() {
        override val mimeType = MimeTypes.NAME
    }

    data class NoteItem(
        override val id: Long,
        val note: String?
    ): DataItem() {
        override val mimeType = MimeTypes.NOTE
    }

    data class OrgItem(
        override val id: Long,
        val organisation: String?,
        val division: String?,
        val title: String?
    ): DataItem() {
        override val mimeType = MimeTypes.ORGANIZATION
    }

    data class PhoneItem(
        override val id: Long,
        val number: String,
        val type: Int, //used to categorise what type of phone number it is, e.g. Home, Mobile, Work, etc.
        val label: String?
    ): DataItem() {
        override val mimeType: String = MimeTypes.PHONE
    }

    data class PhotoItem(
        override val id: Long,
        val uri: String //TODO: overhaul the uri
    ): DataItem() {
        override val mimeType = MimeTypes.PHOTO
    }

    data class ReadOnly(
        override val id: Long,
        val status: Boolean
    ): DataItem() {
        override val mimeType = MimeTypes.READ_ONLY
    }

    data class RelationItem(
        override val id: Long,
        val relation: String,
        val type: Int //What type of relation such as colleague, friend, etc.
    ): DataItem() {
        override val mimeType = MimeTypes.RELATION
    }

    data class WebsiteItem(
        override val id: Long,
        val url: String
    ): DataItem() {
        override val mimeType = MimeTypes.WEBSITE
    }
}