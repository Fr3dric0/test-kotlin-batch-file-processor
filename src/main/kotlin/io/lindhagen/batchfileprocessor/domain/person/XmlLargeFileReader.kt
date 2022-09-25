package io.lindhagen.batchfileprocessor.domain.person

import java.io.BufferedReader
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

private data class XmlElement(
  val name: String,
  val text: String,
  val attributes: Map<String, String> = emptyMap()
)

object XmlLargeFileReader {
  private val log = LoggerFactory.getLogger(this::class.qualifiedName)

  fun readFile(reader: BufferedReader) {
    val streamReader = XMLInputFactory.newInstance().createXMLEventReader(reader)

    while (streamReader.hasNext()) {
      val xmlEvent = streamReader.nextEvent()

      if (xmlEvent.isStartElement && xmlEvent.asStartElement().name.localPart == "person") {
        readElement(streamReader, "person")
      }
    }
  }

  private fun readElement(reader: XMLEventReader, wrappingElementName: String) {
    val elementData = mutableListOf<XmlElement>()

    while (reader.hasNext()) {
      val xmlEvent = reader.nextEvent()
      // Continue looping inside the wrappingElement until we have reached the end
      if (xmlEvent.isEndElement && xmlEvent.asEndElement().name.localPart == wrappingElementName) {
        // Break out of the loop to complete the parsing
        break
      }

      if (xmlEvent.isStartElement) {
        val element = xmlEvent.asStartElement()

        val attributes = mutableListOf<Pair<String, String>>()

        element.attributes.forEach {
          attributes.add(it.name.localPart to it.value)
        }

        elementData.add(XmlElement(
          name = element.name.localPart,
          text = reader.elementText,
          attributes = attributes.associate { it },
        ))
      }
    }

    log.info("Resolved the following information", kv("properties", elementData))
  }
}
