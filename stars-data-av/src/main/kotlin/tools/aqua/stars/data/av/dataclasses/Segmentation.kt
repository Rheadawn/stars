package tools.aqua.stars.data.av.dataclasses

import kotlin.math.max

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Double = 0.0
        private set
    var secondaryValue: Double = 0.0
        private set
    var addJunctions: Boolean = false
        private set

    private constructor(type: Type = Type.BY_BLOCK, value: Double = 0.0, secondaryValue: Double = 0.0, addJunctions: Boolean = false) : this() {
        this.type = type
        this.value = value
        this.secondaryValue = secondaryValue
        this.addJunctions = addJunctions
    }

    companion object {
        fun STATIC_SEGMENT_LENGTH_TICKS(windowSize: Double, overlap: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_TICKS, value = windowSize, secondaryValue = max(((1-overlap)*windowSize),1.0), addJunctions = addJunctions)
        fun STATIC_SEGMENT_LENGTH_METERS(windowSize: Double, overlap: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_METERS, value = windowSize, secondaryValue = max(((1-overlap)*windowSize),1.0), addJunctions = addJunctions)
        fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED, value = stepSize, addJunctions = addJunctions)
        fun DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION, value = stepSize, addJunctions = addJunctions)
        fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1, value = stepSize, addJunctions = addJunctions)
        fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2, value = stepSize, addJunctions = addJunctions)
        fun SLIDING_WINDOW_MULTISTART_METERS(overlap: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_METERS, value = overlap, addJunctions = addJunctions)
        fun SLIDING_WINDOW_MULTISTART_TICKS(overlap: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.SLIDING_WINDOW_MULTISTART_TICKS, value = overlap, addJunctions = addJunctions)
        //==============================================================================================================
        val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        val NONE = Segmentation(Type.NONE)
        fun EVEN_SIZE(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.EVEN_SIZE, value, addJunctions = addJunctions)
        fun BY_LENGTH(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_LENGTH, value, addJunctions = addJunctions)
        fun BY_TICKS(value: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_TICKS, value, addJunctions = addJunctions)
        fun BY_SPEED_LIMITS(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_SPEED_LIMITS, addJunctions = addJunctions)
        fun BY_DYNAMIC_SPEED(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_SPEED, addJunctions = addJunctions)
        fun BY_DYNAMIC_ACCELERATION(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_ACCELERATION, addJunctions = addJunctions)
        fun BY_DYNAMIC_TRAFFIC_DENSITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_TRAFFIC_DENSITY, addJunctions = addJunctions)
        fun BY_DYNAMIC_PEDESTRIAN_PROXIMITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY, addJunctions = addJunctions)
        fun BY_DYNAMIC_LANE_CHANGES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_LANE_CHANGES, addJunctions = addJunctions)
        fun BY_DYNAMIC_VARIABLES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_VARIABLES, addJunctions = addJunctions)
        fun SLIDING_WINDOW(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW, size,stepSize, addJunctions)
        fun SLIDING_WINDOW_METERS(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_METERS, size,stepSize, addJunctions)
        fun SLIDING_WINDOW_BY_BLOCK(size: Double, stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_BLOCK, size, stepSize, addJunctions)
        val SLIDING_WINDOW_HALVING = Segmentation(Type.SLIDING_WINDOW_HALVING)
        fun SLIDING_WINDOW_HALF_OVERLAP(size: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_HALF_OVERLAP, size, addJunctions = addJunctions)
        fun SLIDING_WINDOW_ROTATING(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_ROTATING, secondaryValue = stepSize, addJunctions = addJunctions)
        fun SLIDING_WINDOW_BY_TRAFFIC_DENSITY(stepSize: Double, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_TRAFFIC_DENSITY, secondaryValue = stepSize, addJunctions = addJunctions)

        fun fromConsole(segmentationType: String, segmentationValue: Double?, secondarySegmentationValue: Double?, overlapPercentage: Double?, addJunctions: Boolean): Segmentation {
            return when (segmentationType) {
                "STATIC_SEGMENT_LENGTH_TICKS" -> STATIC_SEGMENT_LENGTH_TICKS(segmentationValue?: 120.0, secondarySegmentationValue?: 0.25, addJunctions)
                "STATIC_SEGMENT_LENGTH_METERS" -> STATIC_SEGMENT_LENGTH_METERS(segmentationValue?: 70.0, secondarySegmentationValue?: 0.25, addJunctions)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(segmentationValue?: 5.0, addJunctions)
                "DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION" -> DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION(segmentationValue?: 5.0, addJunctions)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1(segmentationValue?: 5.0, addJunctions)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2(segmentationValue?: 5.0, addJunctions)
                "SLIDING_WINDOW_MULTISTART_METERS" -> SLIDING_WINDOW_MULTISTART_METERS(segmentationValue?: 25.0, addJunctions)
                "SLIDING_WINDOW_MULTISTART_TICKS" -> SLIDING_WINDOW_MULTISTART_TICKS(segmentationValue?: 25.0, addJunctions)
                //======================================================================================================
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
        STATIC_SEGMENT_LENGTH_TICKS,
        STATIC_SEGMENT_LENGTH_METERS,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED,
        DYNAMIC_SEGMENT_LENGTH_METERS_ACCELERATION,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_1,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED_ACCELERATION_2,
        SLIDING_WINDOW_MULTISTART_METERS,
        SLIDING_WINDOW_MULTISTART_TICKS,
        //==============================================================================================================
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