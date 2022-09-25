package io.lindhagen.batchfileprocessor

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.lindhagen.batchfileprocessor.domain.person.PersonBatchFileService
import io.lindhagen.batchfileprocessor.util.EnvConfig
import io.lindhagen.batchfileprocessor.util.JacksonConfig
import io.lindhagen.batchfileprocessor.util.S3ObjectUploadedEvent
import java.lang.RuntimeException
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

/**
 * This application will read and parse batch files containing
 * weather information. The file itself
 *
 * @property config Contains all configurations
 * */
class PersonBatchFileProcessor(
  private val config: EnvConfig = EnvConfig.load(),
  private val objectMapper: ObjectMapper = JacksonConfig.objectMapper,
  private val batchFileService: PersonBatchFileService = PersonBatchFileService(envConfig = config),
) : RequestHandler<SQSEvent, Unit> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handleRequest(input: SQSEvent, context: Context) {
    runCatching {
      log.info("Processing event from queue...", kv("inputRecords", input.records.size))
      parseEvent(input)
      log.info("Event successfully processed!")
    }.onFailure { failure ->
      when (failure) {
        // Include
        else -> {
          log.error("Failed to process event", failure)
          throw failure
        }
      }
    }
  }

  private fun parseEvent(input: SQSEvent) {
    // We care only about the first records
    val record = parseEventRecords(input).flatMap { it.records }.first()

    log.info(
      "Processing uploaded file...",
      kv("fileKey", record.s3.fileObject.key),
      kv("fileSize", record.s3.fileObject.size),
      kv("event", mapOf(
        "source" to record.eventSource,
        "name" to record.eventName,
        "time" to record.eventTime,
      )),
    )

    if (!isXmlFile(record.s3.fileObject.key)) {
      log.warn(
        "The uploaded file is not in XML format. Ignoring...",
        kv("fileKey", record.s3.fileObject.key),
        kv("fileSize", record.s3.fileObject.size),
        kv("event", mapOf(
          "source" to record.eventSource,
          "name" to record.eventName,
          "time" to record.eventTime,
        )),
      )
      return
    }

    batchFileService.processBatchFile("test", record.s3.fileObject.key)
  }

  private fun parseEventRecords(input: SQSEvent): List<S3ObjectUploadedEvent> {
    log.trace("Decoding event records as JSON objects...")
    return input.records
      .map { objectMapper.readValue(it.body) }
  }

  private fun isXmlFile(s3Key: String): Boolean {
    return s3Key.endsWith(".xml")
  }
}
