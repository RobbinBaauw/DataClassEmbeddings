package nl.robbinbaauw.embedding

import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.util.ElementFilter

class FieldProcessor : ProxyProcessor<Processor.Proxy.Field> {
    override fun parseType(embeddedClass: Element): List<Processor.Proxy.Field> {
        val classFields = ElementFilter.fieldsIn(embeddedClass.enclosedElements)
        return classFields.map {
            val isFinal = it.modifiers.contains(Modifier.FINAL)
            val type = parseType(it, it.asType())

            Processor.Proxy.Field(Processor.Type(it.simpleName.toString(), type), isFinal)
        }
    }

    override fun writeType(type: Processor.Proxy, embeddedField: Element, contextClassName: String): String? {
        if (type is Processor.Proxy.Field) {
            val setter = getVariableSetter(embeddedField, type)
            return """  |${getVariableModifier(type)} $contextClassName.${type.type.name}: ${type.type.type}
                        |    get() = ${embeddedField.simpleName}.${type.type.name}$setter
                    """.trimMargin()
        }

        return null
    }

    private fun getVariableSetter(embeddedField: Element, field: Processor.Proxy.Field): String {
        return if (!field.isFinal) {
            """
        |
        |    set(value) {
        |        ${embeddedField.simpleName}.${field.type.name} = value
        |    }
        """.trimMargin()
        } else {
            ""
        }
    }

    private fun getVariableModifier(field: Processor.Proxy.Field): String {
        return if (field.isFinal) {
            "val"
        } else {
            "var"
        }
    }
}

