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
import tools.aqua.stars.core.metric.serialization.SerializableSegmentationParameters
import java.util.logging.Logger
import tools.aqua.stars.core.metric.utils.ApplicationConstantsHolder
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.types.*
import kotlin.math.pow
import kotlin.math.sqrt

class SegmentParameters<
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>>(
    override val dependsOn: ValidTSCInstancesPerTSCMetric<E, T, S, U, D>,
    override val loggerIdentifier: String = "segmentation-parameters",
    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
) : PostEvaluationMetricProvider<E, T, S, U, D>, Serializable, Loggable {
    var tscList: List<TSC<E, T, S, U, D>> = listOf()

    override fun postEvaluate(){
        tscList = dependsOn.getState().keys.toList()
    }

    override fun getSerializableResults(): List<SerializableSegmentationParameters> =
        tscList.map { tsc ->
            SerializableSegmentationParameters(
                identifier = tsc.identifier,
                source = loggerIdentifier,
                value = 0,
                segmentationType = ApplicationConstantsHolder.segmentationType,
                primarySegmentationValue = ApplicationConstantsHolder.primarySegmentationValue,
                secondarySegmentationValue = ApplicationConstantsHolder.secondarySegmentationValue,
                tertiarySegmentationValue = ApplicationConstantsHolder.tertiarySegmentationValue,
                featureName = ApplicationConstantsHolder.featureName,
                allEgo = ApplicationConstantsHolder.allEgo
            )
        }

    override fun printPostEvaluationResult() {}
}