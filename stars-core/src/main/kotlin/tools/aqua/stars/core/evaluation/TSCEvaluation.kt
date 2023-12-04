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

package tools.aqua.stars.core.evaluation

import java.util.logging.Logger
import kotlin.time.measureTime
import kotlinx.coroutines.*
import tools.aqua.stars.core.metric.metrics.Metrics
import tools.aqua.stars.core.metric.providers.*
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.tsc.instance.TSCInstance
import tools.aqua.stars.core.tsc.instance.TSCInstanceNode
import tools.aqua.stars.core.tsc.projection.TSCProjection
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.core.types.TickDataType

/**
 * This class holds every important data to evaluate a given TSC. The [TSCEvaluation.presentSegment]
 * function evaluates the [TSC] based on the given [SegmentType]. The [TSCProjection]s are filtered
 * by the [projectionIgnoreList]. This class implements [Loggable].
 *
 * @param E [EntityType].
 * @param T [TickDataType].
 * @param S [SegmentType].
 * @property tsc The [TSC].
 * @property segments Sequence of [SegmentType]s.
 * @property projectionIgnoreList List of projections to ignore.
 * @property logger Logger instance.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class TSCEvaluation<E : EntityType<E, T, S>, T : TickDataType<E, T, S>, S : SegmentType<E, T, S>>(
    val tsc: TSC<E, T, S>,
    val segments: Sequence<S>,
    val projectionIgnoreList: List<String> = listOf(),
    override val logger: Logger = Loggable.getLogger("evaluation-time")
) : Loggable {

  /** Holds the [List] of [TSCProjection] based on the base [tsc]. */
  private val tscProjections: MutableList<TSCProjection<E, T, S>> = mutableListOf()

  /** Holds a [List] of all [MetricProvider]s registered by [registerMetricProviders]. */
  private val metrics: Metrics<E, T, S> = Metrics()

  /** Hold all [Deferred] instances returned by [evaluateSegment]. */
  private val segmentEvaluationJobs: MutableList<Deferred<Metrics<E, T, S>>> = mutableListOf()

  /** Coroutine scope for segment evaluations. */
  val scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

  /**
   * Registers a new [MetricProvider] to the list of metrics that should be called during
   * evaluation.
   *
   * @param metricProviders The [MetricProvider]s that should be registered.
   */
  fun registerMetricProviders(vararg metricProviders: MetricProvider<E, T, S>) {
    this.metrics.register(*metricProviders)
  }

  /**
   * Prepares the evaluation by building the projections on the current [TSC] instance. Requires at
   * least one [MetricProvider].
   *
   * @throws IllegalArgumentException When there are no [MetricProvider]s registered.
   */
  fun prepare() {
    require(metrics.any()) { "There needs to be at least one registered MetricProviders." }
    require(tscProjections.isEmpty()) { "TSCEvaluation.prepare() has been called before." }

    // Build all projections of the base TSC
    val tscProjectionCalculationTime = measureTime {
      tscProjections.addAll(tsc.buildProjections(projectionIgnoreList))
      require(tscProjections.isNotEmpty()) { "Found no projections on current TSC." }
    }

    logFine(
        "The calculation of the projections for the given tsc took: $tscProjectionCalculationTime")
  }

  /**
   * Adds the presented segments to the execution worker. Runs the evaluation of the [TSC] based on
   * the [segments]. For each [SegmentType], [TSCProjection] and [TSCInstanceNode], the related
   * [MetricProvider] is called.
   *
   * @param segments Segments to be added to the execution worker.
   * @throws IllegalArgumentException If [prepare] has not been called.
   */
  fun presentSegment(vararg segments: S) {
    if (tscProjections.isEmpty()) prepare()

    segmentEvaluationJobs.addAll(
        segments.map { scope.async { evaluateSegment(it) }.also { it.start() } })
  }

  private suspend fun evaluateSegment(segment: S): Metrics<E, T, S> {
    val metricHolder = metrics.copy()
    val segmentEvaluationTime = measureTime {
      // Run the "evaluate" function for all SegmentMetricProviders on the current segment
      metricHolder.evaluateSegmentMetrics(segment)

      val projectionsEvaluationTime = measureTime {
        val evaluatedProjections = runBlocking { evaluateProjections(segment) }

        evaluatedProjections.forEach { (projection, tscInstance) ->
          val projectionEvaluationTime = measureTime {
            metricHolder.evaluateProjectionMetrics(projection)
            metricHolder.evaluateTSCInstanceMetrics(tscInstance)
            metricHolder.evaluateTSCInstanceAndProjectionMetrics(tscInstance, projection)
          }
          logFine(
              "The evaluation of projection '${projection.id}' for segment '$segment' took: $projectionEvaluationTime.")
        }
      }
      logFine(
          "The evaluation of all projections for segment '$segment' took: $projectionsEvaluationTime")
    }
    logFine("The evaluation of segment '$segment' took: $segmentEvaluationTime")
    return metricHolder
  }

  private suspend fun evaluateProjections(
      segment: S
  ): List<Pair<TSCProjection<E, T, S>, TSCInstance<E, T, S>>> = coroutineScope {
    val jobs =
        tscProjections.map { projection ->
          async { projection to projection.tsc.evaluate(PredicateContext(segment)) }
        }

    jobs.awaitAll()
  }

  /** Closes the [TSCEvaluation] instance printing the results. */
  fun close() {
    tscProjections.clear()

    runBlocking {
      Metrics.merge(segmentEvaluationJobs.awaitAll()).apply {
        printState()
        evaluatePostEvaluationMetrics()
        plotData()
        close()
      }
    }
    closeLogger()
  }
}
