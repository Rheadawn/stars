package tools.aqua.stars.data.av.dataclasses

class Segmentation() {
    var type: Type = Type.BY_BLOCK
        private set
    var value: Int = 0
        private set

    private constructor(type: Type = Type.BY_BLOCK, value: Int = 0) : this() {
        this.type = type
        this.value = value
    }

    companion object {
        val BY_BLOCK = Segmentation(Type.BY_BLOCK)
        val NONE = Segmentation(Type.NONE)
        fun EVEN_SIZE(value: Int): Segmentation = Segmentation(Type.EVEN_SIZE, value)
        fun BY_LENGTH(value: Int): Segmentation = Segmentation(Type.BY_LENGTH, value)
        fun BY_TICKS(value: Int): Segmentation = Segmentation(Type.BY_TICKS, value)

        fun fromConsole(segmentationType: String, segmentationValue: Int?): Segmentation {
            return when (segmentationType) {
                "NONE" -> NONE
                "BY_BLOCK" -> BY_BLOCK
                "EVEN_SIZE" -> EVEN_SIZE(segmentationValue?: 2)
                "BY_LENGTH" -> BY_LENGTH(segmentationValue?: 20)
                "BY_TICKS" -> BY_TICKS(segmentationValue?: 50)
                else -> BY_BLOCK
            }
        }
    }

    enum class Type {
        NONE,
        BY_BLOCK,
        EVEN_SIZE,
        BY_LENGTH,
        BY_TICKS
    }
}