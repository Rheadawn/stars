package tools.aqua.stars.data.av.dataclasses

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Double = 0.0
        private set
    var secondaryValue: Double = 0.0
        private set
    var tertiaryValue: Double = 0.0
        private set
    var addJunctions: Boolean = false
        private set
    var valueList: List<Double> = listOf()
        private set
    var secondaryValueList: List<Double> = listOf()
        private set

    constructor(type: Type = Type.BY_BLOCK, value: Double = 0.0, secondaryValue: Double = 0.0, tertiaryValue: Double = 0.0, valueList: List<Double> = listOf(), secondaryValueList: List<Double> = listOf(), addJunctions: Boolean = false) : this() {
        this.type = type
        this.value = value
        this.secondaryValue = secondaryValue
        this.tertiaryValue = tertiaryValue
        this.valueList = valueList
        this.secondaryValueList = secondaryValueList
        this.addJunctions = addJunctions
    }

    @Suppress("FunctionName")
    companion object {
        private fun STATIC_SEGMENT_LENGTH_SECONDS(windowSize: Double, stepSize: Double): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_SECONDS, value = windowSize, secondaryValue = stepSize)
        private fun STATIC_SEGMENT_LENGTH_METERS(windowSize: Double, stepSize: Double): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_METERS, value = windowSize, secondaryValue = stepSize)
        private fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(lookAhead: Double, scalar: Double, stepSize: Double): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED, value = lookAhead, secondaryValue = scalar, tertiaryValue = stepSize)
        private fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1(lookAhead: Double, stepSize: Double): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1, value = lookAhead, secondaryValue = stepSize)
        private fun SLIDING_WINDOW_MULTISTART_METERS(windowSizes: List<Double>): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_METERS, valueList = windowSizes)
        private fun SLIDING_WINDOW_MULTISTART_SECONDS(windowSizes: List<Double>): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_SECONDS, valueList = windowSizes)
        private fun SLIDING_WINDOW_MULTISTART_SPEED(lookaheads: List<Double>, scalars: List<Double>): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_SPEED, valueList = lookaheads, secondaryValueList = scalars)
        private fun SLIDING_WINDOW_MULTISTART_SPEED_ACCELERATION1(lookaheads: List<Double>): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_SPEED_ACCELERATION1, valueList = lookaheads)
        //==============================================================================================================
        private fun DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION(stepSize: Double): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION, value = stepSize)
        private fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2(stepSize: Double): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2, value = stepSize)
        private val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        private val NONE = Segmentation(Type.NONE)
        private fun EVEN_SIZE(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.EVEN_SIZE, value, addJunctions = addJunctions)
        private fun BY_LENGTH(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_LENGTH, value, addJunctions = addJunctions)
        private fun BY_TICKS(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_TICKS, value, addJunctions = addJunctions)
        private fun BY_SPEED_LIMITS(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_SPEED_LIMITS, addJunctions = addJunctions)
        private fun BY_DYNAMIC_SPEED(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_SPEED, addJunctions = addJunctions)
        private fun BY_DYNAMIC_ACCELERATION(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_ACCELERATION, addJunctions = addJunctions)
        private fun BY_DYNAMIC_TRAFFIC_DENSITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_TRAFFIC_DENSITY, addJunctions = addJunctions)
        private fun BY_DYNAMIC_PEDESTRIAN_PROXIMITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY, addJunctions = addJunctions)
        private fun BY_DYNAMIC_LANE_CHANGES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_LANE_CHANGES, addJunctions = addJunctions)
        private fun BY_DYNAMIC_VARIABLES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_VARIABLES, addJunctions = addJunctions)
        private fun SLIDING_WINDOW(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW, size,stepSize, addJunctions = addJunctions)
        private fun SLIDING_WINDOW_METERS(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_METERS, size,stepSize, addJunctions = addJunctions)
        private fun SLIDING_WINDOW_BY_BLOCK(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_BLOCK, size, stepSize, addJunctions = addJunctions)
        private val SLIDING_WINDOW_HALVING = Segmentation(Type.SLIDING_WINDOW_HALVING)
        private fun SLIDING_WINDOW_HALF_OVERLAP(size: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_HALF_OVERLAP, size, addJunctions = addJunctions)
        private fun SLIDING_WINDOW_ROTATING(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_ROTATING, secondaryValue = stepSize, addJunctions = addJunctions)
        private fun SLIDING_WINDOW_BY_TRAFFIC_DENSITY(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_TRAFFIC_DENSITY, secondaryValue = stepSize, addJunctions = addJunctions)

        fun fromConsole(segmentationType: String, segmentationValue: Double?, secondarySegmentationValue: Double?, tertiarySegmentationValue: Double?, valueList: List<Double>?, secondaryValueList: List<Double>?, addJunctions: Boolean): Segmentation {
            return when (segmentationType) {
                "STATIC_SEGMENT_LENGTH_SECONDS" -> STATIC_SEGMENT_LENGTH_SECONDS(segmentationValue?: 60.0, secondarySegmentationValue?: 2.0)
                "STATIC_SEGMENT_LENGTH_METERS" -> STATIC_SEGMENT_LENGTH_METERS(segmentationValue?: 70.0, secondarySegmentationValue?: 2.0)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(segmentationValue?: 2.0, secondarySegmentationValue?: 0.0, tertiarySegmentationValue?: 2.0)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1(segmentationValue?: 30.0, secondarySegmentationValue?: 2.0)
                "SLIDING_WINDOW_MULTISTART_METERS" -> SLIDING_WINDOW_MULTISTART_METERS(valueList?: listOf(8.0))
                "SLIDING_WINDOW_MULTISTART_SECONDS" -> SLIDING_WINDOW_MULTISTART_SECONDS(valueList?: listOf(2.5))
                "SLIDING_WINDOW_MULTISTART_SPEED" -> SLIDING_WINDOW_MULTISTART_SPEED(valueList?: listOf(8.0), secondaryValueList?: listOf(0.0))
                "SLIDING_WINDOW_MULTISTART_SPEED_ACCELERATION1" -> SLIDING_WINDOW_MULTISTART_SPEED_ACCELERATION1(valueList?: listOf(0.5))
                //======================================================================================================
                "DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION" -> DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION(segmentationValue?: 5.0)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2(segmentationValue?: 5.0)
                "NONE" -> NONE
                "BY_BLOCK" -> BY_BLOCK
                "EVEN_SIZE" -> EVEN_SIZE(segmentationValue?: 2.0, addJunctions)
                "BY_LENGTH" -> BY_LENGTH(segmentationValue?: 100.0, addJunctions)
                "BY_TICKS" -> BY_TICKS(segmentationValue?: 100.0, addJunctions)
                "BY_SPEED_LIMITS" -> BY_SPEED_LIMITS(addJunctions)
                "BY_DYNAMIC_SPEED" -> BY_DYNAMIC_SPEED(addJunctions)
                "BY_DYNAMIC_ACCELERATION" -> BY_DYNAMIC_ACCELERATION(addJunctions)
                "BY_DYNAMIC_TRAFFIC_DENSITY" -> BY_DYNAMIC_TRAFFIC_DENSITY(addJunctions)
                "BY_DYNAMIC_PEDESTRIAN_PROXIMITY" -> BY_DYNAMIC_PEDESTRIAN_PROXIMITY(addJunctions)
                "BY_DYNAMIC_LANE_CHANGES" -> BY_DYNAMIC_LANE_CHANGES(addJunctions)
                "BY_DYNAMIC_VARIABLES" -> BY_DYNAMIC_VARIABLES(addJunctions)
                "SLIDING_WINDOW" -> SLIDING_WINDOW(segmentationValue?: 100.0, secondarySegmentationValue?: 1.0, addJunctions)
                "SLIDING_WINDOW_METERS" -> SLIDING_WINDOW_METERS(segmentationValue?: 100.0, secondarySegmentationValue?: 10.0, addJunctions)
                "SLIDING_WINDOW_BY_BLOCK" -> SLIDING_WINDOW_BY_BLOCK(segmentationValue?: 100.0, secondarySegmentationValue?: 1.0, addJunctions)
                "SLIDING_WINDOW_HALVING" -> SLIDING_WINDOW_HALVING
                "SLIDING_WINDOW_HALF_OVERLAP" -> SLIDING_WINDOW_HALF_OVERLAP(segmentationValue?: 100.0, addJunctions)
                "SLIDING_WINDOW_ROTATING" -> SLIDING_WINDOW_ROTATING(secondarySegmentationValue?: 5.0, addJunctions )
                "SLIDING_WINDOW_BY_TRAFFIC_DENSITY" -> SLIDING_WINDOW_BY_TRAFFIC_DENSITY(secondarySegmentationValue?: 5.0, addJunctions)
                else -> BY_BLOCK
            }
        }
    }

    enum class Type {
        STATIC_SEGMENT_LENGTH_SECONDS,
        STATIC_SEGMENT_LENGTH_METERS,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1,
        SLIDING_WINDOW_MULTISTART_METERS,
        SLIDING_WINDOW_MULTISTART_SECONDS,
        SLIDING_WINDOW_MULTISTART_SPEED,
        SLIDING_WINDOW_MULTISTART_SPEED_ACCELERATION1,
        //==============================================================================================================
        DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2,
        NONE,
        BY_BLOCK,
        EVEN_SIZE,
        BY_LENGTH,
        BY_TICKS,
        SLIDING_WINDOW,
        SLIDING_WINDOW_BY_BLOCK,
        BY_SPEED_LIMITS,
        BY_DYNAMIC_SPEED,
        BY_DYNAMIC_ACCELERATION,
        BY_DYNAMIC_TRAFFIC_DENSITY,
        BY_DYNAMIC_VARIABLES,
        BY_DYNAMIC_PEDESTRIAN_PROXIMITY,
        BY_DYNAMIC_LANE_CHANGES,
        SLIDING_WINDOW_HALVING,
        SLIDING_WINDOW_HALF_OVERLAP,
        SLIDING_WINDOW_METERS,
        SLIDING_WINDOW_ROTATING,
        SLIDING_WINDOW_BY_TRAFFIC_DENSITY
    }
}