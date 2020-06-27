package nl.robbinbaauw.embedding

import java.lang.IllegalStateException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("nl.robbinbaauw.embedding.Embed")
class Processor : AbstractProcessor() {

    fun getClass(element: Element?): Element? {
        return when {
            element == null -> null
            element.kind == ElementKind.CLASS -> element
            else -> getClass(element.enclosingElement)
        }
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(Embed::class.java).forEach { element ->
            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()

            val classElement = getClass(element) ?: throw IllegalStateException()
            val fileName = "${classElement.simpleName}Embed.kt"

            processingEnv.filer
                .createResource(StandardLocation.SOURCE_OUTPUT, packageName, fileName)
                .openWriter()
                .use {
                    it.write(
                    """
                    ${if (packageName.isNotEmpty()) "package $packageName" else "" }
                    
                    // Klappe
                    """.trimIndent()
                    )
                }
        }
        return true
    }
}
