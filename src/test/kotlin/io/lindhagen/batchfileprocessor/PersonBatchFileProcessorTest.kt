package io.lindhagen.batchfileprocessor

import io.lindhagen.batchfileprocessor.domain.person.PersonBatchFileService
import io.lindhagen.batchfileprocessor.exception.MalformedUploadedFileException
import io.lindhagen.batchfileprocessor.testutils.TestContainersWrapper
import io.lindhagen.batchfileprocessor.testutils.TestContext
import io.lindhagen.batchfileprocessor.testutils.TestDataHelper
import io.lindhagen.batchfileprocessor.util.EnvConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEmpty

/**
 * Tests the overall business workflow
 * by treating all internal components as black-boxes,
 * and only by considering the input and outcome of the processing
 * */
internal class PersonBatchFileProcessorTest : TestContainersWrapper() {
  companion object {
    private const val TEST_BUCKET_NAME = "test"
    private const val TEST_S3_FILE_PATH = "person/batch_persons.xml"
  }

  private val mockedEventBridgeClient = mockk<EventBridgeClient>()

  private val slotPutEventsRequest = slot<PutEventsRequest>()

  @BeforeEach
  fun setUp() {
    slotPutEventsRequest.clear()

    createS3Bucket(s3Client, TEST_BUCKET_NAME)

    uploadFileToS3Bucket(
      s3Client,
      TEST_BUCKET_NAME,
      TEST_S3_FILE_PATH,
      // This is located inside src/test/resources
      // Be aware that JUnit does not like when we load massive files
      "/samples/batch_persons_small.xml",
    )

    every { mockedEventBridgeClient.putEvents(capture(slotPutEventsRequest)) }.answers {
      PutEventsResponse
        .builder()
        .entries(emptyList())
        .failedEntryCount(0)
        .build()
    }
  }

  /**
   * Verifies that our processor is able to communicate with the
   * non-mocked EventBridge client.
   * */
  @Test
  fun `Processor is able to publish to EventBridge`() {
    // Use the non-mocked EventBridge client
    val batchFileProcessor = configureProcessor(eventBridgeClient)

    val input = TestDataHelper.buildSqsEvent(TestDataHelper.buildFileUploadedEvent(
      bucketName = TEST_BUCKET_NAME,
      fileName = TEST_S3_FILE_PATH,
    ))

    // If we fail to publish the test should throw an exception
    batchFileProcessor.handleRequest(input, TestContext())
  }

  /**
   * Verifies that the entries in the file uploaded to S3
   * is published to EventBridge, with our expected format
   * */
  @Test
  fun `Contents of batch-file in S3 is published with correct information`() {
    val batchFileProcessor = configureProcessor()

    val input = TestDataHelper.buildSqsEvent(TestDataHelper.buildFileUploadedEvent(
      bucketName = TEST_BUCKET_NAME,
      fileName = TEST_S3_FILE_PATH,
    ))

    // Trigger processing
    batchFileProcessor.handleRequest(input, TestContext())

    // Make sure EventBridge has been called
    verify { mockedEventBridgeClient.putEvents(any<PutEventsRequest>()) }

    // Verify the data published is formatted as we expect
    val detail = slotPutEventsRequest.captured.entries().first().detail()
    expectThat(detail).isEmpty()
  }

  /**
   * Verify that files we are not interested in is ignored
   * and not result in failure.
   * */
  @Test
  fun `Non-XML files are ignored and marked as succeeded`() {
    val s3Key = "person/non_xml_file.json"
    uploadFileToS3Bucket(
      s3Client,
      TEST_BUCKET_NAME,
      s3Key,
      // This is located inside src/test/resources
      // Be aware that JUnit does not like when we load massive files
      "/samples/batch_persons_small.xml",
    )

    val batchFileProcessor = configureProcessor()

    val input = TestDataHelper.buildSqsEvent(TestDataHelper.buildFileUploadedEvent(
      bucketName = TEST_BUCKET_NAME,
      fileName = s3Key,
    ))

    batchFileProcessor.handleRequest(input, TestContext())

    verify(exactly = 0) { mockedEventBridgeClient.putEvents(any<PutEventsRequest>()) }
  }

  /**
   * Verify that our processor fails properly,
   * when we load a file that is not properly structured
   * */
  @Test
  fun `Malformed files will fail processor`() {
    uploadFileToS3Bucket(
      s3Client,
      TEST_BUCKET_NAME,
      TEST_S3_FILE_PATH,
      "/samples/batch_persons_malformed.xml",
    )

    val batchFileProcessor = configureProcessor()

    val input = TestDataHelper.buildSqsEvent(TestDataHelper.buildFileUploadedEvent(
      bucketName = TEST_BUCKET_NAME,
      fileName = TEST_S3_FILE_PATH,
    ))

    // Malformed files should result in the processing failing hard
    expectThrows<MalformedUploadedFileException> { batchFileProcessor.handleRequest(input, TestContext()) }

    verify(exactly = 0) { mockedEventBridgeClient.putEvents(any<PutEventsRequest>()) }
  }

  /**
   * Ensures that our code process large files in the best way.
   * */
  @Test
  fun `Processor is able to handle massive files`() {

  }

  @Test
  fun `Processor throws exception when publish to EventBridge fails`() {

  }

  @Test
  fun `Processor aborts when file does not exist in S3`() {
    val batchFileProcessor = configureProcessor()

    val input = TestDataHelper.buildSqsEvent(TestDataHelper.buildFileUploadedEvent(
      bucketName = TEST_BUCKET_NAME,
      fileName = "non-existing-file.xml",
    ))

    // We expect the original exception from AWS S3
    expectThrows<NoSuchKeyException> { batchFileProcessor.handleRequest(input, TestContext()) }
  }

  private fun configureProcessor(
    eventBridgeClient: EventBridgeClient = mockedEventBridgeClient,
  ): PersonBatchFileProcessor {
    val configuration = buildTestConfiguration(eventBridgeBusArn = "arn::::test")

    return PersonBatchFileProcessor(
      config = configuration,
      batchFileService = PersonBatchFileService(
        envConfig = configuration,
        eventBridgeClient = eventBridgeClient,
        s3Client = s3Client,
      )
    )
  }

}

private fun buildTestConfiguration(eventBridgeBusArn: String): EnvConfig {
  return EnvConfig.load().copy(
    eventBridgeBusArn = eventBridgeBusArn,
  )
}

