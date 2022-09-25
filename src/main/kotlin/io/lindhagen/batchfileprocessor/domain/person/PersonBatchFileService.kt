package io.lindhagen.batchfileprocessor.domain.person

import io.lindhagen.batchfileprocessor.util.EnvConfig
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry
import software.amazon.awssdk.services.s3.S3Client

/**
 * This class does the main workload of processing and publishing items
 * in the file as events
 * */
class PersonBatchFileService(
  private val envConfig: EnvConfig,
  private val eventBridgeClient: EventBridgeClient = createEventBridgeClient(envConfig = envConfig),
  private val s3Client: S3Client = createS3Client(envConfig = envConfig),
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.qualifiedName)

    /**
     * Name of the XML-element containing person information
     * */
    private const val PERSON_XML_ELEMENT_NAME = "person"
  }

  /**
   * Reads the batch file and publishes the contents to EventBridge
   *
   * @throws Exception In case of unrecoverable failures
   * */
  fun processBatchFile(s3Bucket: String, s3Key: String) {
    log.info("Fetching file from S3 Bucket...", kv("s3Bucket", s3Bucket), kv("s3Key", s3Key))

    val reader = s3Client
      .getObject { it.bucket(s3Bucket).key(s3Key) }
      .bufferedReader(Charsets.UTF_8)

//    readXmlFile(reader)

    log.info("Publishing events...")
    // putEvents have a convenience anonymous-function to create the request.
    // However, it is easier to mock and verify .putEvents when we directly pass
    // the PutEventsRequest
    val eventResponse = eventBridgeClient.putEvents(PutEventsRequest
      .builder()
      .entries(
        PutEventsRequestEntry
          .builder()
          .resources(envConfig.eventBridgeBusArn)
          .source(this::class.qualifiedName)
          .detailType("personDetails")
          .detail("""{"name": "Jon", "phone": "4444993"}""")
          .build()
      )
      .build()
    )

    log.info("Events published",
      kv("failedEntryCount", eventResponse.failedEntryCount()),
      kv("entryResponse", eventResponse.entries().map {
        mapOf(
          "eventId" to it.eventId(),
          "errorCode" to it.errorCode(),
          "errorMessage" to it.errorMessage(),
        )
      })
    )

  }

//  private fun readXmlFile(reader: BufferedReader) {
//    val streamReader = XMLInputFactory.newInstance().createXMLStreamReader(reader)
//
//    while (streamReader.hasNext()) {
//      val eventCode = streamReader.next()
//
//      val isPersonElementStart = eventCode == XMLStreamConstants.START_ELEMENT
//        && streamReader.localName.equals(PERSON_XML_ELEMENT_NAME, ignoreCase = true)
//
//      if (!isPersonElementStart) {
//        continue
//      }
//
//      while (streamReader.hasNext()) {
//        val eventCode = streamReader.next()
//        val isPersonElementEnd = eventCode == XMLStreamConstants.END_ELEMENT
//          && streamReader.localName.equals(PERSON_XML_ELEMENT_NAME, ignoreCase = true)
//
//        if (isPersonElementEnd) {
//          break
//        }
//
//
//      }
//    }
//  }
}

private fun createS3Client(envConfig: EnvConfig): S3Client = S3Client
  .builder()
  .region(envConfig.region)
  .build()

private fun createEventBridgeClient(envConfig: EnvConfig) = EventBridgeClient
  .builder()
  .region(envConfig.region)
  .build()
