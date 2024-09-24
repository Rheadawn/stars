package tools.aqua.stars.data.av.dataclasses

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Int = 0
        private set
    var segmentJunctions: Boolean = false
        private set

    private constructor(type: Type = Type.BY_BLOCK, value: Int = 0, segmentJunctions: Boolean = false) : this() {
        this.type = type
        this.value = value
        this.segmentJunctions = segmentJunctions
    }

    companion object {
        val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        val NONE = Segmentation(Type.NONE)
        fun EVEN_SIZE(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.EVEN_SIZE, value, segmentJunctions)
        fun BY_LENGTH(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_LENGTH, value, segmentJunctions)
        fun BY_TICKS(value: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.BY_TICKS, value, segmentJunctions)
        fun SLIDING_WINDOW(size: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW, size, segmentJunctions)
        fun SLIDING_WINDOW_BY_BLOCK(size: Int, segmentJunctions: Boolean): Segmentation = Segmentation(Type.SLIDING_WINDOW_BY_BLOCK, size, segmentJunctions)

        fun fromConsole(segmentationType: String, segmentationValue: Int?, segmentJunctions: Boolean): Segmentation {
            return when (segmentationType) {
                "NONE" -> NONE
                "BY_BLOCK" -> BY_BLOCK
                "EVEN_SIZE" -> EVEN_SIZE(segmentationValue?: 2, segmentJunctions)
                "BY_LENGTH" -> BY_LENGTH(segmentationValue?: 20, segmentJunctions)
                "BY_TICKS" -> BY_TICKS(segmentationValue?: 50, segmentJunctions)
                "SLIDING_WINDOW" -> SLIDING_WINDOW(segmentationValue?: 50, segmentJunctions)
                "SLIDING_WINDOW_BY_BLOCK" -> SLIDING_WINDOW_BY_BLOCK(segmentationValue?: 50, segmentJunctions)
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
    }
}