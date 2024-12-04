/*
 * Copyright 2023-2024 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.core.metric.metrics.postEvaluation

import tools.aqua.stars.core.metric.metrics.evaluation.ValidTSCInstancesPerTSCMetric
import tools.aqua.stars.core.metric.providers.*
import tools.aqua.stars.core.metric.serialization.SerializableCVResult
import java.util.logging.Logger
import tools.aqua.stars.core.metric.utils.ApplicationConstantsHolder
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.types.*
import kotlin.math.pow
import kotlin.math.sqrt

class SegmentLengthMetric<
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>>(
    override val dependsOn: ValidTSCInstancesPerTSCMetric<E, T, S, U, D>,
    override val loggerIdentifier: String = "segment-length-metric",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) : PostEvaluationMetricProvider<E, T, S, U, D>, Serializable, Loggable {
    var tscList: List<TSC<E, T, S, U, D>> = listOf()

    private var segmentLengthsInSeconds: List<Double> = listOf()
    private var segmentLengthsInMeters: List<Double> = listOf()
    private var coefficientVariationForSeconds: Double = 0.0
    private var conformityRateSeconds: Double = 0.0
    private var coefficientVariationForMeters: Double = 0.0
    private var conformityRateMeters: Double = 0.0

    override fun postEvaluate(){
        tscList = dependsOn.getState().keys.toList()

        segmentLengthsInSeconds = ApplicationConstantsHolder.segmentLengthsInSeconds
        segmentLengthsInMeters = ApplicationConstantsHolder.segmentLengthsInMeters

        coefficientVariationForSeconds = calculateCV(segmentLengthsInSeconds)
        conformityRateSeconds = calculateConformityRate(10.0, 60.0, segmentLengthsInSeconds)

        coefficientVariationForMeters = calculateCV(segmentLengthsInMeters)
        conformityRateMeters = calculateConformityRate(5.0, 50.0, segmentLengthsInMeters)
    }

    private fun calculateCV(segmentLengths: List<Double>): Double {
        val mean = segmentLengths.average()
        val stdDev = sqrt(segmentLengths.map { (it - mean).pow(2) }.average())
        return (stdDev / mean)
    }

    private fun calculateConformityRate(lowerBound: Double, upperBound: Double, segmentLengths: List<Double>): Double {
        val count = segmentLengths.count { it in lowerBound..upperBound }
        return count.toDouble() / segmentLengths.size
    }

    override fun getSerializableResults(): List<SerializableCVResult> =
        tscList.map { tsc ->
            SerializableCVResult(
                identifier = tsc.identifier,
                source = loggerIdentifier,
                value = 0,
                cvForSeconds = coefficientVariationForSeconds,
                conformityRateSeconds = conformityRateSeconds,
                minSeconds = segmentLengthsInSeconds.minOrNull() ?: 0.0,
                maxSeconds = segmentLengthsInSeconds.maxOrNull() ?: Double.MAX_VALUE,
                averageSeconds = segmentLengthsInSeconds.average(),
                cvForMeters = coefficientVariationForMeters,
                conformityRateMeters = conformityRateMeters,
                minMeters = segmentLengthsInMeters.minOrNull() ?: 0.0,
                maxMeters = segmentLengthsInMeters.maxOrNull() ?: Double.MAX_VALUE,
                averageMeters = segmentLengthsInMeters.average()
            )
        }

    override fun printPostEvaluationResult() {}
}