package nl.robbinbaauw.embedding

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.jvm.internal.Reflection
import kotlin.reflect.full.declaredMembers

internal class ProcessorTest {

    private fun compile(@Language("kotlin") code: String): KotlinCompilation.Result {
        val kotlinSource = SourceFile.kotlin("EntityClass.kt", code)

        return KotlinCompilation().apply {
            sources = listOf(kotlinSource)

            // pass your own instance of an annotation processor
            annotationProcessors = listOf(Processor())

            inheritClassPath = true
            messageOutputStream = System.out // see diagnostics in real time
        }.compile()
    }

    @Test
    fun testGenerateProxyGetterSetter() {
        val result = compile(
            """
            package nl.test
            
            import nl.robbinbaauw.embedding.HasEmbeds
            import nl.robbinbaauw.embedding.Embeddable

            @Embeddable
            data class BaseClass(val a: Int, var b: String?, val c: List<String?>?)
            
            @HasEmbeds
            data class TestClass(val testClass: BaseClass, val otherNumber: Int)
        """
        )
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val clazz = result.classLoader.loadClass("TestClass")
        val kClazz = Reflection.createKotlinClass(clazz)

        // TODO add assertions
    }

    @Test
    fun testGenerateProxyFunction() {
        val result = compile(
            """
            import nl.robbinbaauw.embedding.Embed
            import nl.robbinbaauw.embedding.Embeddable

            @Embeddable
            data class BaseClass(val id: Int) {
                fun <X: Int> x(a: Int, b: BaseClass): String {}
                fun y() {}
            }
            
            data class TestClass(@field:Embed val testClass: BaseClass, val otherNumber: Int)
        """
        )
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val clazz = result.classLoader.loadClass("TestClass")
        val kClazz = Reflection.createKotlinClass(clazz)

        // TODO add assertions
    }

}
