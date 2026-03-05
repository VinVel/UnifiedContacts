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

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import net.velcore.unifiedcontacts.system.ContactRow

class ContactRowsAdapter : ListAdapter<ContactRow, RecyclerView.ViewHolder>(DiffCallback) {
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

private fun Int.dpToPx(view: AppCompatTextView): Int {
    return (this * view.resources.displayMetrics.density).toInt()
}