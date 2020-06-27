package nl.robbinbaauw.embedding

import javax.lang.model.element.Element
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

class MethodProcessor : ProxyProcessor<Processor.Proxy.Method> {
    override fun parseType(embeddedClass: Element): List<Processor.Proxy.Method> {
        val classMethods = ElementFilter.methodsIn(embeddedClass.enclosedElements)
        return classMethods.mapNotNull { method ->
            // TODO check whether it's user defined. Maybe add a annotation for that
            if (!method.simpleName.toString().startsWith("x")) return@mapNotNull null

            val params = method.parameters.map {
                Processor.Type(it.simpleName.toString(), parseType(it, it.asType()))
            }

            val typeParams = method.typeParameters.map { typeParam ->
                val bounds = typeParam.bounds.map {
                    parseType(typeParam, it)
                }
                Processor.TypeParameter(typeParam.simpleName.toString(), bounds)
            }

            val returnType = if (method.returnType.kind != TypeKind.VOID) {
                parseType(method, method.returnType)
            } else {
                null
            }

            Processor.Proxy.Method(method.simpleName.toString(), returnType, params, typeParams)
        }
    }

    override fun writeType(type: Processor.Proxy, embeddedField: Element, contextClassName: String): String? {
        if (type is Processor.Proxy.Method) {
            // TODO make this look nicer
            return "fun ${getTypeParamsDef(type)}${contextClassName}.${type.name}(${getParamsDef(type)})${getReturnType(type)}${getTypeParamsWhere(type)} = " +
                    "${embeddedField.simpleName}.${type.name}(${getParams(type)})"
        }

        return null
    }

    private fun getTypeParamsDef(proxy: Processor.Proxy.Method): String {
        if (proxy.typeParams.isEmpty()) return ""

        return "<" + proxy.typeParams.joinToString(", ") {
            it.name
        } + "> "
    }

    private fun getTypeParamsWhere(proxy: Processor.Proxy.Method): String {
        if (proxy.typeParams.isEmpty()) return ""

        return " where " + proxy.typeParams.flatMap { typeParam ->
            typeParam.bounds.map {
                Processor.Type(typeParam.name, it)
            }
        }.joinToString(", ") {
            "${it.name}: ${it.type}"
        }
    }

    private fun getParamsDef(proxy: Processor.Proxy.Method): String {
        return proxy.params.joinToString(", ") {
            "${it.name}: ${it.type}"
        }
    }

    private fun getParams(proxy: Processor.Proxy.Method): String {
        return proxy.params.joinToString(", ") {
            it.name
        }
    }

    private fun getReturnType(proxy: Processor.Proxy.Method): String {
        return if (proxy.returnType != null) {
            ": ${proxy.returnType}"
        } else {
            ""
        }
    }
}
