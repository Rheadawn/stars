package tools.aqua.stars.core.evaluation

import tools.aqua.stars.core.types.*

class PredicateCache<
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> {
    val nullaryPredicateCache: MutableMap<
            Triple<
                    NullaryPredicate<E, T, S, U, D>,
                    U,
                    String
                    >,
            Boolean> = mutableMapOf()

    val unaryPredicateCache: MutableMap<
            Triple<
                    UnaryPredicate<*, E, T, S, U, D>,
                    Pair<U, Int>,
                    String
                    >,
            Boolean> = mutableMapOf()

    val binaryPredicateCache: MutableMap<
            Triple<
                    BinaryPredicate<*, *, E, T, S, U, D>,
                    Triple<U, Int, Int>,
                    String
                    >,
            Boolean> = mutableMapOf()

    var lastSegmentSource: String = ""

    fun clearCachesForNewSegmentSource (currentSegmentSource: String) {
        if (lastSegmentSource != currentSegmentSource) {
            nullaryPredicateCache.clear()
            unaryPredicateCache.clear()
            binaryPredicateCache.clear()
            lastSegmentSource = currentSegmentSource
        }
    }
}