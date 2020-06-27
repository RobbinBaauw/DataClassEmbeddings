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
import javax.lang.model.util.ElementFilter
import javax.tools.StandardLocation

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("nl.robbinbaauw.embedding.Embed", "nl.robbinbaauw.embedding.Embeddable")
class Processor : AbstractProcessor() {

    private fun getClass(element: Element?): Element? {
        return when {
            element == null -> null
            element.kind == ElementKind.CLASS -> element
            else -> getClass(element.enclosingElement)
        }
    }

    data class Field(val name: String)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val classFields: MutableMap<String, List<Field>> = HashMap()

        roundEnv.getElementsAnnotatedWith(Embeddable::class.java).forEach { element ->
            val elementFields = ElementFilter.fieldsIn(element.enclosedElements)
            val fields = elementFields.map {
                Field(it.simpleName.toString())
            }

            classFields[element.asType().toString()] = fields
        }

        roundEnv.getElementsAnnotatedWith(Embed::class.java).forEach { element ->
            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()

            val classElement = getClass(element) ?: throw IllegalStateException("Cannot find class containing the field ${element.simpleName}")
            val className = classElement.simpleName.toString()
            val fileName = "${className}Embed.kt"

            val fieldTypeName = element.asType().toString()
            val currFields = classFields[fieldTypeName] ?: throw IllegalStateException("Did not parse type name $fieldTypeName. Is it marked @Embeddable?")

            val getters = currFields.joinToString(separator = "\n") { field ->
                "val $className.${field.name} get() = ${element.simpleName}.${field.name}"
            }

            processingEnv.filer
                .createResource(StandardLocation.SOURCE_OUTPUT, packageName, fileName)
                .openWriter()
                .use {
                    it.write(
                    """
                    ${if (packageName.isNotEmpty()) "package $packageName" else "" }
                    
                    $getters
                    """.trimIndent()
                    )
                }
        }
        return true
    }
}
