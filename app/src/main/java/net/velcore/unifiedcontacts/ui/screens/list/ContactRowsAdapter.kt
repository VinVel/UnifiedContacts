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

package net.velcore.unifiedcontacts.ui.screens.list

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.velcore.unifiedcontacts.system.ContactRow

class ContactRowsAdapter : ListAdapter<ContactRow, RecyclerView.ViewHolder>(DiffCallback) {
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).stableKey

    override fun getItemViewType(position: Int): Int = getItem(position).contentType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) createHeaderViewHolder(parent) else createItemViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ContactRow.Header -> (holder as HeaderViewHolder).bind(row.letter)
            is ContactRow.Item -> (holder as ItemViewHolder).bind(
                name = row.name,
                photoThumbnailUri = row.photoThumbnailUri,
                scope = adapterScope,
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ItemViewHolder) {
            holder.recycle()
        }
        super.onViewRecycled(holder)
    }

    private fun createHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val textView = AppCompatTextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        return HeaderViewHolder(textView)
    }

    private fun createItemViewHolder(parent: ViewGroup): ItemViewHolder {
        val context = parent.context
        val row = LinearLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dpToPx(this), 8.dpToPx(this), 16.dpToPx(this), 8.dpToPx(this))
        }

        val avatarFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(40.dpToPx(this), 40.dpToPx(this))
            background = createCircleBackground(this)
            clipToOutline = true
        }
        val avatarImage = AppCompatImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val avatarInitial = AppCompatTextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(
                resolveThemeColor(
                    view = this,
                    attr = R.attr.colorOnSurface,
                    fallback = Color.DKGRAY,
                ),
            )
        }
        avatarFrame.addView(avatarImage)
        avatarFrame.addView(avatarInitial)

        val nameView = AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            )
            setTypeface(null, Typeface.NORMAL)
            textSize = 16f
            setTextColor(
                resolveThemeColor(
                    view = this,
                    attr = R.attr.colorOnSurface,
                    fallback = Color.BLACK,
                ),
            )
        }
        (nameView.layoutParams as LinearLayout.LayoutParams).marginStart = 12.dpToPx(nameView)
        row.addView(avatarFrame)
        row.addView(nameView)
        return ItemViewHolder(row, avatarFrame, avatarImage, avatarInitial, nameView)
    }

    private class HeaderViewHolder(
        private val textView: AppCompatTextView,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(letter: Char) {
            textView.text = letter.toString()
            textView.setTypeface(null, Typeface.BOLD)
            textView.textSize = 18f
            textView.setPadding(16.dpToPx(textView), 8.dpToPx(textView), 16.dpToPx(textView), 8.dpToPx(textView))
            textView.setTextColor(
                resolveThemeColor(
                    view = textView,
                    attr = R.attr.colorOnSurface,
                    fallback = Color.BLACK,
                ),
            )
        }
    }

    private class ItemViewHolder(
        itemView: View,
        private val avatarFrame: FrameLayout,
        private val avatarImage: AppCompatImageView,
        private val avatarInitial: AppCompatTextView,
        private val nameView: AppCompatTextView,
    ) : RecyclerView.ViewHolder(itemView) {
        private var decodeJob: Job? = null
        private var boundPhotoUri: String? = null

        fun bind(
            name: String,
            photoThumbnailUri: String?,
            scope: CoroutineScope,
        ) {
            decodeJob?.cancel()
            boundPhotoUri = photoThumbnailUri
            nameView.text = name
            showInitial(name)

            if (photoThumbnailUri.isNullOrBlank()) return

            ContactThumbnailBitmapCache.get(photoThumbnailUri)?.let { cached ->
                showBitmap(cached)
                return
            }

            decodeJob = scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    96.decodeThumbnailBitmap(
                        contentResolver = itemView.context.contentResolver,
                        uriString = photoThumbnailUri,
                    )
                } ?: return@launch
                ContactThumbnailBitmapCache.put(photoThumbnailUri, bitmap)
                if (boundPhotoUri == photoThumbnailUri) {
                    showBitmap(bitmap)
                }
            }
        }

        fun recycle() {
            decodeJob?.cancel()
            decodeJob = null
            boundPhotoUri = null
            avatarImage.setImageDrawable(null)
            avatarInitial.visibility = View.VISIBLE
        }

        private fun showInitial(name: String) {
            avatarFrame.background = createCircleBackground(avatarFrame)
            avatarInitial.visibility = View.VISIBLE
            avatarInitial.text = name
                .trim()
                .split(" ")
                .firstOrNull()
                ?.firstOrNull()
                ?.uppercase() ?: "?"
        }

        private fun showBitmap(bitmap: Bitmap) {
            avatarImage.setImageBitmap(bitmap)
            avatarInitial.visibility = View.GONE
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

private object ContactThumbnailBitmapCache {
    private const val MAX_CACHE_KB = 8 * 1024
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private fun Int.decodeThumbnailBitmap(
    contentResolver: ContentResolver,
    uriString: String,
): Bitmap? {
    val uri = runCatching { uriString.toUri() }.getOrNull() ?: return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
    }.getOrNull()

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sample = calculateInSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        reqWidth = this,
        reqHeight = this,
    )

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }.getOrNull()
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun createCircleBackground(view: View): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(
            resolveThemeColor(
                view = view,
                attr = R.attr.colorSurface,
                fallback = Color.LTGRAY,
            ),
        )
    }
}

private fun resolveThemeColor(view: View, attr: Int, fallback: Int): Int {
    return runCatching { MaterialColors.getColor(view, attr) }.getOrElse { fallback }
}

private fun Int.dpToPx(view: View): Int {
    return (this * view.resources.displayMetrics.density).toInt()
}