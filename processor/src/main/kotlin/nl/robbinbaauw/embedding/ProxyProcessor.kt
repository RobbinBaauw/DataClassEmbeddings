package nl.robbinbaauw.embedding

import javax.lang.model.element.Element

interface ProxyProcessor<X : Processor.Proxy> {
    fun parseType(embeddedClass: Element): List<X>
    fun writeType(type: Processor.Proxy, embeddedField: Element, contextClassName: String): String?
}
