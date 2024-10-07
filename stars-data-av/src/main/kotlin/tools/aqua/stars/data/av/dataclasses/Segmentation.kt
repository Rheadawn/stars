package tools.aqua.stars.data.av.dataclasses

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Int = 0
        private set
    var secondaryValue: Int = 0
        private set
    var segmentJunctions: Boolean = false
        private set

    private constructor(type: Type = Type.BY_BLOCK, value: Int = 0, secondaryValue: Int = 0, segmentJunctions: Boolean = false) : this() {
        this.type = type
        this.value = value
        this.secondaryValue = secondaryValue
        this.segmentJunctions = segmentJunctions
    }

    companion object {
        val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        val NONE = Segmentation(Type.NONE)
        fun EVEN_SIZE(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.EVEN_SIZE, value, segmentJunctions = segmentJunctions)
        fun BY_LENGTH(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_LENGTH, value, segmentJunctions = segmentJunctions)
        fun BY_TICKS(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_TICKS, value, segmentJunctions = segmentJunctions)
        fun BY_SPEED_LIMITS(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_SPEED_LIMITS, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_SPEED(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_SPEED, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_ACCELERATION(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_ACCELERATION, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_TRAFFIC_DENSITY(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_TRAFFIC_DENSITY, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_PEDESTRIAN_PROXIMITY(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_PEDESTRIAN_PROXIMITY, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_LANE_CHANGES(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_LANE_CHANGES, segmentJunctions = segmentJunctions)
        fun BY_DYNAMIC_VARIABLES(segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_DYNAMIC_VARIABLES, segmentJunctions = segmentJunctions)
        fun SLIDING_WINDOW(size: Int, stepSize: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW, size,stepSize, segmentJunctions)
        fun SLIDING_WINDOW_BY_BLOCK(size: Int, stepSize: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_BLOCK, size, stepSize, segmentJunctions)
        val SLIDING_WINDOW_HALVING = Segmentation(Type.SLIDING_WINDOW_HALVING)
        fun SLIDING_WINDOW_HALF_OVERLAP(size: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_HALF_OVERLAP, size, segmentJunctions = segmentJunctions)

        fun fromConsole(segmentationType: String, segmentationValue: Int?, secondarySegmentationValue: Int?,segmentJunctions: Boolean): Segmentation {
            return when (segmentationType) {
                "NONE" -> NONE
                "BY_BLOCK" -> BY_BLOCK
                "EVEN_SIZE" -> EVEN_SIZE(segmentationValue?: 2, segmentJunctions)
                "BY_LENGTH" -> BY_LENGTH(segmentationValue?: 100, segmentJunctions)
                "BY_TICKS" -> BY_TICKS(segmentationValue?: 100, segmentJunctions)
                "BY_SPEED_LIMITS" -> BY_SPEED_LIMITS(segmentJunctions)
                "BY_DYNAMIC_SPEED" -> BY_DYNAMIC_SPEED(segmentJunctions)
                "BY_DYNAMIC_ACCELERATION" -> BY_DYNAMIC_ACCELERATION(segmentJunctions)
                "BY_DYNAMIC_TRAFFIC_DENSITY" -> BY_DYNAMIC_TRAFFIC_DENSITY(segmentJunctions)
                "BY_DYNAMIC_PEDESTRIAN_PROXIMITY" -> BY_DYNAMIC_PEDESTRIAN_PROXIMITY(segmentJunctions)
                "BY_DYNAMIC_LANE_CHANGES" -> BY_DYNAMIC_LANE_CHANGES(segmentJunctions)
                "BY_DYNAMIC_VARIABLES" -> BY_DYNAMIC_VARIABLES(segmentJunctions)
                "SLIDING_WINDOW" -> SLIDING_WINDOW(segmentationValue?: 100, secondarySegmentationValue?: 1, segmentJunctions)
                "SLIDING_WINDOW_BY_BLOCK" -> SLIDING_WINDOW_BY_BLOCK(segmentationValue?: 100, secondarySegmentationValue?: 1, segmentJunctions)
                "SLIDING_WINDOW_HALVING" -> SLIDING_WINDOW_HALVING
                "SLIDING_WINDOW_HALF_OVERLAP" -> SLIDING_WINDOW_HALF_OVERLAP(segmentationValue?: 100, segmentJunctions)
                else -> BY_BLOCK
            }
        }
    }

    enum class Type {
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
        SLIDING_WINDOW_HALF_OVERLAP
    }
}