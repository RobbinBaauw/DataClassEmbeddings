package nl.robbinbaauw.embedding

data class X(val y: Double) {
    fun hi() {}
}

@Embeddable
data class BaseClass(
    @field:TestAnnotation
    val id: Int,

    @field:TestAnnotation
    var number2: Int,

    @field:TestAnnotation
    var x: X?
) {
    fun y(a: Int, b: BaseClass): List<String>? {
        return emptyList()
    }

    fun z() {
        println(2)
    }
}

@HasEmbeds
data class TestClass(
    val testClass: BaseClass,
    val id: Int,
    val otherNumber: Int
) {
    fun z() {
        println(1)
    }
}

fun main() {
    val x = TestClass(BaseClass(1, 2, X(1.0)), 2, 2)
    println(x.id)
    println(x.x?.y)
    x.number2 = 1

    x.z()
}
