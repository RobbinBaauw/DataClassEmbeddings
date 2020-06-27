package nl.robbinbaauw.embedding

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
@SupportedAnnotationTypes("nl.robbinbaauw.embedding.Embed", "nl.robbinbaauw.embedding.Embeddable")
class Processor : AbstractProcessor() {

    private fun getClass(element: Element?): Element? {
        return when {
            element == null -> null
            element.kind == ElementKind.CLASS -> element
            else -> getClass(element.enclosingElement)
        }
    }

    data class TypeParameter(val name: String, val bounds: List<String>)
    data class Type(val name: String, val type: String)

    sealed class Proxy {
        data class Field(val type: Type, val isFinal: Boolean) : Proxy()
        data class Method(val name: String, val returnType: String?, val params: List<Type>, val typeParams: List<TypeParameter>) : Proxy()
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val processors = listOf(FieldProcessor(), MethodProcessor())

        val classFields = roundEnv
            .getElementsAnnotatedWith(Embeddable::class.java)
            .map { element ->
                element.asType().toString() to processors.flatMap { it.parseType(element) }
            }
            .toMap()

        roundEnv.getElementsAnnotatedWith(Embed::class.java).forEach { embeddedField ->
            val packageName = processingEnv.elementUtils.getPackageOf(embeddedField).qualifiedName.toString()

            val classElement = getClass(embeddedField)
                ?: throw IllegalStateException("Cannot find class containing the field ${embeddedField.simpleName}")
            val contextClassName = classElement.simpleName.toString()
            val fileName = "${contextClassName}Embed.kt"

            val fieldTypeName = embeddedField.asType().toString()
            val currFields = classFields[fieldTypeName]
                ?: throw IllegalStateException("Did not parse type name $fieldTypeName. Is it marked @Embeddable?")


            val proxies = currFields.flatMap { type ->
                processors.mapNotNull {
                    it.writeType(type, embeddedField, contextClassName)
                }
            }.joinToString(separator = "\n\n")

            processingEnv.filer
                .createResource(StandardLocation.SOURCE_OUTPUT, packageName, fileName)
                .openWriter()
                .use {
                    it.write(
                        """ 
                    |${if (packageName.isNotEmpty()) "package $packageName" else ""}
                    |
                    |$proxies
                    """.trimMargin()
                    )
                }
        }
        return true
    }


}
