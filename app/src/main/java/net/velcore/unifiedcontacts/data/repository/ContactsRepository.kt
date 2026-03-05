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

package net.velcore.unifiedcontacts.data.repository

import android.accounts.Account
import android.content.Context
import contacts.core.Contacts
import contacts.core.ContactsFields
import contacts.core.asc
import net.velcore.unifiedcontacts.domain.contact.AccountRef

class ContactsRepository(private val context: Context) {
    fun getAllContactNames(accountRef: AccountRef? = null): List<String> {
        val query = Contacts(context)
            .query()
            .orderBy(ContactsFields.DisplayNamePrimary.asc())

        val contacts =
            if (accountRef != null) {query.accounts(Account(accountRef.name, accountRef.type)).find()}
            else {query.find()} //this is useful for when the user wants to view their contact from all Accounts simultaneously

        return contacts.mapNotNull { it.displayNamePrimary }
    }
}