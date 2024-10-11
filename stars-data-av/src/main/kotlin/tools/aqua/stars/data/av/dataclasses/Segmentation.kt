package tools.aqua.stars.data.av.dataclasses

import kotlin.math.max

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Int = 0
        private set
    var secondaryValue: Int = 0
        private set
    var addJunctions: Boolean = false
        private set

    private constructor(type: Type = Type.BY_BLOCK, value: Int = 0, secondaryValue: Int = 0, addJunctions: Boolean = false) : this() {
        this.type = type
        this.value = value
        this.secondaryValue = secondaryValue
        this.addJunctions = addJunctions
    }

    companion object {
        fun STATIC_SEGMENT_LENGTH_TICKS(windowSize: Int, overlapPercentage: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_TICKS, value = windowSize, secondaryValue = max(((1-overlapPercentage)*windowSize).toInt(),1), addJunctions = addJunctions)
        fun STATIC_SEGMENT_LENGTH_METERS(windowSize: Int, overlapPercentage: Double, addJunctions: Boolean): Segmentation = Segmentation(type = Type.STATIC_SEGMENT_LENGTH_METERS, value = windowSize, secondaryValue = max(((1-overlapPercentage)*windowSize).toInt(),1), addJunctions = addJunctions)
        fun DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(type = Type.DYNAMIC_SEGMENT_LENGTH_METERS_SPEED, value = stepSize, addJunctions = addJunctions)
        //==============================================================================================================
        val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        val NONE = Segmentation(Type.NONE)
        fun EVEN_SIZE(value: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.EVEN_SIZE, value, addJunctions = addJunctions)
        fun BY_LENGTH(value: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_LENGTH, value, addJunctions = addJunctions)
        fun BY_TICKS(value: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.BY_TICKS, value, addJunctions = addJunctions)
        fun BY_SPEED_LIMITS(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_SPEED_LIMITS, addJunctions = addJunctions)
        fun BY_DYNAMIC_SPEED(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_SPEED, addJunctions = addJunctions)
        fun BY_DYNAMIC_ACCELERATION(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_ACCELERATION, addJunctions = addJunctions)
        fun BY_DYNAMIC_TRAFFIC_DENSITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_TRAFFIC_DENSITY, addJunctions = addJunctions)
        fun BY_DYNAMIC_PEDESTRIAN_PROXIMITY(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY, addJunctions = addJunctions)
        fun BY_DYNAMIC_LANE_CHANGES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_LANE_CHANGES, addJunctions = addJunctions)
        fun BY_DYNAMIC_VARIABLES(addJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_VARIABLES, addJunctions = addJunctions)
        fun SLIDING_WINDOW(size: Int, stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW, size,stepSize, addJunctions)
        fun SLIDING_WINDOW_METERS(size: Int, stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_METERS, size,stepSize, addJunctions)
        fun SLIDING_WINDOW_BY_BLOCK(size: Int, stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_BLOCK, size, stepSize, addJunctions)
        val SLIDING_WINDOW_HALVING = Segmentation(Type.SLIDING_WINDOW_HALVING)
        fun SLIDING_WINDOW_HALF_OVERLAP(size: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_HALF_OVERLAP, size, addJunctions = addJunctions)
        fun SLIDING_WINDOW_ROTATING(stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_ROTATING, secondaryValue = stepSize, addJunctions = addJunctions)
        fun SLIDING_WINDOW_BY_TRAFFIC_DENSITY(stepSize: Int, addJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_TRAFFIC_DENSITY, secondaryValue = stepSize, addJunctions = addJunctions)

        fun fromConsole(segmentationType: String, segmentationValue: Int?, secondarySegmentationValue: Int?, overlapPercentage: Double?, addJunctions: Boolean): Segmentation {
            return when (segmentationType) {
                "STATIC_SEGMENT_LENGTH_TICKS" -> STATIC_SEGMENT_LENGTH_TICKS(segmentationValue?: 120, overlapPercentage?:0.25, addJunctions)
                "STATIC_SEGMENT_LENGTH_METERS" -> STATIC_SEGMENT_LENGTH_METERS(segmentationValue?: 70, overlapPercentage?:0.25, addJunctions)
                "DYNAMIC_SEGMENT_LENGTH_METERS_SPEED" -> DYNAMIC_SEGMENT_LENGTH_METERS_SPEED(segmentationValue?: 5, addJunctions)
                //======================================================================================================
                "NONE" -> NONE
                "BY_BLOCK" -> BY_BLOCK
                "EVEN_SIZE" -> EVEN_SIZE(segmentationValue?: 2, addJunctions)
                "BY_LENGTH" -> BY_LENGTH(segmentationValue?: 100, addJunctions)
                "BY_TICKS" -> BY_TICKS(segmentationValue?: 100, addJunctions)
                "BY_SPEED_LIMITS" -> BY_SPEED_LIMITS(addJunctions)
                "BY_DYNAMIC_SPEED" -> BY_DYNAMIC_SPEED(addJunctions)
                "BY_DYNAMIC_ACCELERATION" -> BY_DYNAMIC_ACCELERATION(addJunctions)
                "BY_DYNAMIC_TRAFFIC_DENSITY" -> BY_DYNAMIC_TRAFFIC_DENSITY(addJunctions)
                "BY_DYNAMIC_PEDESTRIAN_PROXIMITY" -> BY_DYNAMIC_PEDESTRIAN_PROXIMITY(addJunctions)
                "BY_DYNAMIC_LANE_CHANGES" -> BY_DYNAMIC_LANE_CHANGES(addJunctions)
                "BY_DYNAMIC_VARIABLES" -> BY_DYNAMIC_VARIABLES(addJunctions)
                "SLIDING_WINDOW" -> SLIDING_WINDOW(segmentationValue?: 100, secondarySegmentationValue?: 1, addJunctions)
                "SLIDING_WINDOW_METERS" -> SLIDING_WINDOW_METERS(segmentationValue?: 100, secondarySegmentationValue?: 10, addJunctions)
                "SLIDING_WINDOW_BY_BLOCK" -> SLIDING_WINDOW_BY_BLOCK(segmentationValue?: 100, secondarySegmentationValue?: 1, addJunctions)
                "SLIDING_WINDOW_HALVING" -> SLIDING_WINDOW_HALVING
                "SLIDING_WINDOW_HALF_OVERLAP" -> SLIDING_WINDOW_HALF_OVERLAP(segmentationValue?: 100, addJunctions)
                "SLIDING_WINDOW_ROTATING" -> SLIDING_WINDOW_ROTATING(secondarySegmentationValue?: 5, addJunctions )
                "SLIDING_WINDOW_BY_TRAFFIC_DENSITY" -> SLIDING_WINDOW_BY_TRAFFIC_DENSITY(secondarySegmentationValue?: 5, addJunctions)
                else -> BY_BLOCK
            }
        }
    }

    enum class Type {
        STATIC_SEGMENT_LENGTH_TICKS,
        STATIC_SEGMENT_LENGTH_METERS,
        DYNAMIC_SEGMENT_LENGTH_METERS_SPEED,
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