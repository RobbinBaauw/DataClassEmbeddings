package nl.robbinbaauw.embedding

data class X(val y: Double)

@Embeddable
data class BaseClass(val id: Int, var number2: Int, var x: X)

data class TestClass(
    @field:Embed val testClass: BaseClass,
    val otherNumber: Int
)

fun main() {
    val x = TestClass(BaseClass(1, 2, X(1.0)), 2)
    println(x.id)
    println(x.x.y)
    x.number2 = 1
}
