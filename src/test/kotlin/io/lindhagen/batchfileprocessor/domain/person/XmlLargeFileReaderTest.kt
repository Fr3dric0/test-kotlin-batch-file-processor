package io.lindhagen.batchfileprocessor.domain.person

import java.lang.IllegalArgumentException
import org.junit.jupiter.api.Test

internal class XmlLargeFileReaderTest {
  @Test
  fun `'readFile' calls function when a new entity is discovered`() {
    val reader = this::class.java
      .getResourceAsStream("/samples/batch_persons_small.xml")
      ?.bufferedReader(Charsets.UTF_8)
      ?: throw IllegalArgumentException("File does not exist!")

    XmlLargeFileReader.readFile(reader)
  }
}
