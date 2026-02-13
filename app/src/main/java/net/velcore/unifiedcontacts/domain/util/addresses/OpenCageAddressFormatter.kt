package net.velcore.unifiedcontacts.domain.util.addresses

import java.util.Locale
import net.velcore.unifiedcontacts.domain.contact.DataItem.AddressItem
import com.bettermile.addressformatter.AddressFormatter as LibraryFormatter

object OpenCageAddressFormatter: AddressFormatter {

    private val formatter = LibraryFormatter(abbreviate = false, appendCountry = false)

    override fun format(
        address: AddressItem,
        locale: Locale
    ): String {

        val iso = CountryNormalizer.normalizeToIso(address.country)
            ?: locale.country

        val components = mutableMapOf<String, String>()

        // ISO country code (required for proper formatting)
        iso?.let {
            components["country_code"] = it.lowercase(Locale.ROOT)
        }

        // Street OR PO Box (if both exist, OpenCage will handle ordering)
        address.street
            ?.takeIf { it.isNotBlank() }
            ?.let { components["road"] = it.trim() }

        address.pobox
            ?.takeIf { it.isNotBlank() }
            ?.let { components["po_box"] = it.trim() }

        address.neighborhood
            ?.takeIf { it.isNotBlank() }
            ?.let { components["suburb"] = it.trim() }

        address.city
            ?.takeIf { it.isNotBlank() }
            ?.let { components["city"] = it.trim() }

        address.region
            ?.takeIf { it.isNotBlank() }
            ?.let { components["state"] = it.trim() }

        address.postcode
            ?.takeIf { it.isNotBlank() }
            ?.let { components["postcode"] = it.trim() }

        address.country
            ?.takeIf { it.isNotBlank() }
            ?.let { components["country"] = it.trim() }

        return formatter.format(components)
    }
}
