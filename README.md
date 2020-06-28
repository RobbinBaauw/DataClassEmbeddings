## DataClassEmbeddings

Provides the option to embed data classes like in Golang.

### Example
```kotlin
data class X(val y: Double) {
    fun hi() {}
}

@Embeddable
data class BaseClass(val id: Int, var number2: Int, var x: X?) {
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
    val otherNumber: Int
) {
    fun z() {
        println(1)
    }
}

fun main() {
    val x = TestClass(BaseClass(1, 2, X(1.0)), 2)
    println(x.id)
    println(x.x?.y)
    x.number2 = 1

    x.z() // prints 1
}
```
