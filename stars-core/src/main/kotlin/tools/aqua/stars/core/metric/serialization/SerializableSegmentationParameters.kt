package tools.aqua.stars.core.metric.serialization

import kotlinx.serialization.Serializable

/**
 * This class implements the [SerializableResult] interface and stores one [Long] as a [value].
 *
 * @property identifier The identifier of this specific result.
 * @property source The source (i.e. the metric) which produced this result.
 * @property value The value that should be serialized.
 */
@Serializable
data class SerializableSegmentationParameters(
    override val identifier: String,
    override val source: String,
    override val value: Long,
    val segmentationType: String,
    val primarySegmentationValue: Double,
    val secondarySegmentationValue: Double,
    val tertiarySegmentationValue: Double,
    val featureName: String,
    val allEgo: Boolean
): SerializableResult()