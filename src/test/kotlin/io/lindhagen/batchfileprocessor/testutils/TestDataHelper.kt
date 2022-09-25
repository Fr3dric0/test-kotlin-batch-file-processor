package io.lindhagen.batchfileprocessor.testutils

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import java.util.UUID

object TestDataHelper {

  fun buildSqsEvent(payload: String): SQSEvent {
    return SQSEvent().apply {
      records = listOf(
        SQSEvent.SQSMessage().apply {
          messageId = UUID.randomUUID().toString()
          body = payload
        }
      )
    }
  }

  fun buildFileUploadedEvent(bucketName: String, fileName: String): String {
    return """
    {
      "Records":[
        {
          "eventVersion":"2.1",
          "eventSource":"aws:s3",
          "awsRegion":"us-east-1",
          "eventTime":"2022-09-24T18:56:40.600Z",
          "eventName":"ObjectCreated:CompleteMultipartUpload",
          "userIdentity":{"principalId":"A5MV292AYZ1MC"},
          "requestParameters":{"sourceIPAddress":"84.209.5.237"},
          "responseElements":{
            "x-amz-request-id":"XZB1HT4K8DV74793",
            "x-amz-id-2":"dkHtQN1y8GtniMxhhYG5Xn4Yz9X7kopYwDdF1gle8x90o6Ppwj1nzCAFvt0cuwLFlJVIfKr03DPda73sKi9AOil8bDaKxzYP"
          },
          "s3":{
            "s3SchemaVersion":"1.0",
            "configurationId":"created-files",
            "bucket": {
              "name":"$bucketName",
              "ownerIdentity":{"principalId":"A5MV292AYZ1MC"},
              "arn":"arn:aws:s3:::$bucketName"
            },
            "object":{
              "key":"$fileName",
              "size":203142842,
              "eTag":"1b31a00e6b646be515bb41871d845129-12",
              "sequencer":"00632F52C2CE4F9834"
            }
          }
        }
      ]
    }
    """.trimIndent()
  }
}
