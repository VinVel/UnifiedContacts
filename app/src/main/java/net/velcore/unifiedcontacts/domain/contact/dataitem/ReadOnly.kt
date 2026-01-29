package net.velcore.unifiedcontacts.domain.contact.dataitem

import net.velcore.unifiedcontacts.domain.util.MimeTypes

data class ReadOnly(
    override val id: Long,
    val status: Boolean

): DataItem() {
    override val mimeType = MimeTypes.READ_ONLY
}
