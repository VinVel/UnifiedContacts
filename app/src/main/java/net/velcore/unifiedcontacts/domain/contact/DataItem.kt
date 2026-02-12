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

import net.velcore.unifiedcontacts.data.android.MimeTypes
import net.velcore.unifiedcontacts.domain.util.normalizer.*
import net.velcore.unifiedcontacts.domain.util.validator.*

sealed class DataItem{
    abstract val id: Long
    abstract val mimeType: String

    open fun validate(): ValidationResult = ValidationResult.Valid

    open fun normalize(): DataItem = this

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
    ): DataItem() {
        override val mimeType = MimeTypes.ADDRESS
    }

    data class EmailItem(
        override val id: Long,
        val address: String,
        val type: Int
    ): DataItem() {
        override val mimeType = MimeTypes.EMAIL

        override fun validate(): ValidationResult = EmailValidator.validate(address)

        override fun normalize(): DataItem {
            val normalized = EmailNormalizer.normalize(address)
            return if (normalized == address) this else copy(address = normalized)
        }
    }

    data class EventItem(
        override val id: Long,
        val type: String,
        val date: Long // Use epoch milliseconds
    ): DataItem() {
        override val mimeType = MimeTypes.EVENT
    }

    data class GenderItem(
        override val id: Long,
        val gender: String
    ): DataItem() {
        override val mimeType = MimeTypes.GENDER
    }

    data class GroupItem(
        override val id: Long,
        val groupId: Long
    ): DataItem() {
        override val mimeType = MimeTypes.GROUP_MEMBERSHIP
    }

    data class ImItem( //Instant Messaging
        override val id: Long,
        val name: String
    ): DataItem() {
        override val mimeType = MimeTypes.IM
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

    data class NicknameItem(
        override val id: Long,
        val nickname: String
    ): DataItem() {
        override val mimeType = MimeTypes.NICKNAME
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
        val type: Int, //used to categorize what type of phone number it is, e.g. Home, Mobile, Work, etc.
        val label: String?
    ): DataItem() {
        override val mimeType = MimeTypes.PHONE

        override fun normalize(): DataItem {
            val normalized = PhoneNormalizer.normalize(number)
            return if (normalized == number) this else copy(number = normalized)
        }
    }

    data class PhotoItem(
        override val id: Long,
        val uri: ByteArray
    ): DataItem() {
        override val mimeType = MimeTypes.PHOTO

        //auto-generated code by Android Studio to handle proper ByteArray usage (?)
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PhotoItem

            if (id != other.id) return false
            if (!uri.contentEquals(other.uri)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + uri.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
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

    data class SipAddressItem(
        override val id: Long,
        val sipAddress: String?,
        val type: String, //e.g. TYPE_HOME, TYPE_WORK, or TYPE_OTHER
        val customLabel: String?,
        val isPrimary: Boolean, //Indicates if this is the primary SIP address for the contact
        val isSuperPrimary: Boolean //Indicates if this is the primary SIP address for the aggregate contact
    ): DataItem() {
        override val mimeType = MimeTypes.SIP_ADDRESS
    }
}