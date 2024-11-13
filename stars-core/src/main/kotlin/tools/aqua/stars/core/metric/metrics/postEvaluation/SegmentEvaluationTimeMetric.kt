package tools.aqua.stars.core.metric.metrics.postEvaluation

import tools.aqua.stars.core.metric.metrics.evaluation.ValidTSCInstancesPerTSCMetric
import tools.aqua.stars.core.metric.providers.Loggable
import tools.aqua.stars.core.metric.providers.PostEvaluationMetricProvider
import tools.aqua.stars.core.metric.providers.Serializable
import tools.aqua.stars.core.metric.serialization.SerializableLongResult
import tools.aqua.stars.core.metric.utils.ApplicationConstantsHolder
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.types.*
import java.util.logging.Logger

@Suppress("unused")
class SegmentEvaluationTimeMetric<
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>>(
    override val dependsOn: ValidTSCInstancesPerTSCMetric<E, T, S, U, D>,
    override val loggerIdentifier: String = "segment-evaluation-time",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) : PostEvaluationMetricProvider<E, T, S, U, D>, Serializable, Loggable {

    var tscList: List<TSC<E, T, S, U, D>> = listOf()

    override fun postEvaluate(){
        tscList = dependsOn.getState().keys.toList()
    }

    override fun printPostEvaluationResult() {}

    override fun getSerializableResults(): List<SerializableLongResult> =
        tscList.map { tsc ->
            SerializableLongResult(
                identifier = tsc.identifier,
                source = loggerIdentifier,
                value = ApplicationConstantsHolder.totalEvaluationTime.inWholeSeconds
            )
        }
}