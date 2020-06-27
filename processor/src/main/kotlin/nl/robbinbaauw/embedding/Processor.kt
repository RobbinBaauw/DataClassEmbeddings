package nl.robbinbaauw.embedding

import java.lang.IllegalStateException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
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

    data class Field(val name: String, val type: String, val isFinal: Boolean)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val classFields: MutableMap<String, List<Field>> = HashMap()

        roundEnv.getElementsAnnotatedWith(Embeddable::class.java).forEach { element ->
            val elementFields = ElementFilter.fieldsIn(element.enclosedElements)
            val fields = elementFields.map {
                val isFinal = it.modifiers.contains(Modifier.FINAL)

                val type = when (it.asType().kind) {
                    TypeKind.BOOLEAN -> "Boolean"
                    TypeKind.BYTE -> "Byte"
                    TypeKind.SHORT -> "Short"
                    TypeKind.INT -> "Int"
                    TypeKind.LONG -> "Long"
                    TypeKind.CHAR -> "Char"
                    TypeKind.FLOAT -> "Float"
                    TypeKind.DOUBLE -> "Double"
                    else -> it.asType().toString()
                }

                Field(it.simpleName.toString(), type, isFinal)
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

            fun getSetter(field: Field): String {
                return if (!field.isFinal) {
                    """
                    |
                    |    set(value) {
                    |        ${element.simpleName}.${field.name} = value
                    |    }
                    """.trimMargin()
                } else {
                    ""
                }
            }

            fun getModifier(field: Field): String {
                return if (field.isFinal) {
                    "val"
                } else {
                    "var"
                }
            }

            val gettersAndSetters = currFields.joinToString(separator = "\n\n") { field ->
                val setter = getSetter(field)
                """ |${getModifier(field)} $className.${field.name}: ${field.type}
                    |    get() = ${element.simpleName}.${field.name}$setter
                """.trimMargin()
            }

            processingEnv.filer
                .createResource(StandardLocation.SOURCE_OUTPUT, packageName, fileName)
                .openWriter()
                .use {
                    it.write(
                    """ 
                    |${if (packageName.isNotEmpty()) "package $packageName" else "" }
                    |
                    |$gettersAndSetters
                    """.trimMargin()
                    )
                }
        }
        return true
    }
}
