package tools.aqua.stars.core.metric.serialization

import kotlinx.serialization.Serializable

/**
 * This class implements the [SerializableResult] interface and stores
 * the coefficient of variation (CV) for the length of the segments, both for seconds and meters.
 *
 * @property identifier The identifier of this specific result.
 * @property source The source (i.e. the metric) which produced this result.
 * @property value Not used.
 * @property seconds The CV value for segment length in seconds. [0,1+]
 * @property conformityRateSeconds The conformity rate for segment length in seconds. [0,1]
 * @property meters The CV value for segment length in meters. [0,1+]
 * @property conformityRateMeters The conformity rate for segment length in meters. [0,1]
 */
@Serializable
data class SerializableCVResult(
    override val identifier: String,
    override val source: String,
    override val value: Int,
    val cvForSeconds: Double,
    val conformityRateSeconds: Double,
    val conformityRateMeters: Double,
    val cvForMeters: Double,
): SerializableResult()