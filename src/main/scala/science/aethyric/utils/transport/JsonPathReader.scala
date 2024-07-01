package science.aethyric.utils.transport

import java.io.InputStream

import scala.io.Source

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.jayway.jsonpath

object JsonPathReader {

  val jsonPathConfiguration: jsonpath.Configuration = jsonpath.Configuration
    .defaultConfiguration()
    .setOptions(
      jsonpath.Option.DEFAULT_PATH_LEAF_TO_NULL,
      jsonpath.Option.SUPPRESS_EXCEPTIONS)
    .mappingProvider(
      new jsonpath.spi.mapper.JacksonMappingProvider(new ObjectMapper().registerModule(DefaultScalaModule))
    )

  def apply(inputStream: InputStream): JsonPathReader = {
    new JsonPathReader(inputStream)
  }

}


class JsonPathReader(inputStream: InputStream) {

  val ctx: jsonpath.ReadContext = jsonpath.JsonPath
    .using(JsonPathReader.jsonPathConfiguration)
    .parse(Source
      .fromInputStream(inputStream)
      .mkString
    )

  def read(jsonPath: String): Any = {
    ctx.read(jsonPath, classOf[Any])
  }
}