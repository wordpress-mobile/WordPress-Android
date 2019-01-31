package org.wordpress.android.fluxc.model.plans

data class PlanOfferModel(
    var planIds: List<Int>?,
    var features: List<Feature>?,
    var name: String?,
    var shortName: String?,
    var tagline: String?,
    var description: String?,
    var iconUrl: String?
) {
    data class Feature(
        var id: String?,
        var name: String?,
        var description: String?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (other == null || other !is Feature) {
                return false
            }

            return id == other.id && name == other.name &&
                    description == other.description
        }

        override fun hashCode(): Int {
            var result = id?.hashCode() ?: 0
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + (description?.hashCode() ?: 0)
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || other !is PlanOfferModel) {
            return false
        }

        return name == other.name && shortName == other.shortName &&
                tagline == other.tagline && description == other.description &&
                iconUrl == other.iconUrl && planIds == other.planIds && features == other.features
    }

    override fun hashCode(): Int {
        var result = planIds?.hashCode() ?: 0
        result = 31 * result + (features?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (shortName?.hashCode() ?: 0)
        result = 31 * result + (tagline?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (iconUrl?.hashCode() ?: 0)
        return result
    }
}
