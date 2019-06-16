package com.elliptic.medm.common.pipeline

import com.elliptic.medm.common.StreamingPipeline
import com.elliptic.medm.common.pipeline.processor.ProcessorBluePrint
import com.elliptic.medm.common.pipeline.sink.KafkaStreamingSink
import com.elliptic.medm.common.pipeline.source.JsonFileStreamingSource
import com.elliptic.medm.utils.AppConfig

class StreamingFileToKafkaPipeline(configuration: AppConfig, processor: ProcessorBluePrint)
  extends StreamingPipeline(configuration, processor)
    with JsonFileStreamingSource
    with KafkaStreamingSink {

}