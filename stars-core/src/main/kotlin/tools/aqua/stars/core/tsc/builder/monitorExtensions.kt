/*
 * Copyright 2024 The STARS Project Authors
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

package tools.aqua.stars.core.tsc.builder

import tools.aqua.stars.core.evaluation.BinaryPredicate
import tools.aqua.stars.core.evaluation.NullaryPredicate
import tools.aqua.stars.core.evaluation.UnaryPredicate
import tools.aqua.stars.core.types.*

/**
 * DSL function for a monitor using [NullaryPredicate].
 *
 * @param E [EntityType].
 * @param T [TickDataType].
 * @param S [SegmentType].
 * @param U [TickUnit].
 * @param D [TickDifference].
 * @param label Name of the edge.
 * @param predicate The monitor predicate.
 */
fun <
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> TSCMonitorsBuilder<E, T, S, U, D>.monitor(
    label: String,
    predicate: NullaryPredicate<E, T, S, U, D>
) = monitor(label) { ctx -> predicate.holds(ctx) }

/**
 * DSL function for a monitor using [UnaryPredicate].
 *
 * @param E1 [EntityType].
 * @param E [EntityType].
 * @param T [TickDataType].
 * @param S [SegmentType].
 * @param U [TickUnit].
 * @param D [TickDifference].
 * @param label Name of the edge.
 * @param entity The entity to evaluate this condition for.
 * @param predicate The monitor predicate.
 */
fun <
    E1 : E,
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> TSCMonitorsBuilder<E, T, S, U, D>.monitor(
    label: String,
    entity: E1,
    predicate: UnaryPredicate<E1, E, T, S, U, D>
) = monitor(label) { ctx -> predicate.holds(ctx, entity) }

/**
 * DSL function for a monitor using [BinaryPredicate].
 *
 * @param E1 [EntityType].
 * @param E2 [EntityType].
 * @param E [EntityType].
 * @param T [TickDataType].
 * @param S [SegmentType].
 * @param U [TickUnit].
 * @param D [TickDifference].
 * @param label Name of the edge.
 * @param entity1 The first entity to evaluate this condition for.
 * @param entity2 The second entity to evaluate this condition for.
 * @param predicate The monitor predicate.
 */
fun <
    E1 : E,
    E2 : E,
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>> TSCMonitorsBuilder<E, T, S, U, D>.monitor(
    label: String,
    entity1: E1,
    entity2: E2,
    predicate: BinaryPredicate<E1, E2, E, T, S, U, D>
) = monitor(label) { ctx -> predicate.holds(ctx, entity1, entity2) }
