package nl.robbinbaauw.embedding

@Embeddable
data class BaseClass(val id: Int)

data class TestClass(
    @field:Embed val testClass: BaseClass,
    val otherNumber: Int
)

fun main() {
    val x = TestClass(BaseClass(1), 2)
    println(x.id)
}
