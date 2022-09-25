package io.lindhagen.batchfileprocessor.exception

/**
 * Used to signify that the uploaded file is malformed
 * in some way.
 * */
class MalformedUploadedFileException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
