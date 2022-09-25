package io.lindhagen.batchfileprocessor.util

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class S3ObjectUploadedEvent(
  @JsonProperty("Records")
  val records: List<Record>,
) {
  data class Record(
    val eventSource: String,
    val eventTime: ZonedDateTime,
    val eventName: String,
    val awsRegion: String,
    val s3: S3Record,
  )

  data class S3Record(
    val configurationId: String,
    /**
     * @var this is the object-property in the JSON object
     *
     * Renamed because "object" is a reserved keyword in Kotlin
     * */
    @JsonProperty("object")
    val fileObject: FileObject,
  ) {
    data class FileObject(
      val key: String,
      val size: Long,
      val eTag: String,
      val sequencer: String,
    )
  }
}
