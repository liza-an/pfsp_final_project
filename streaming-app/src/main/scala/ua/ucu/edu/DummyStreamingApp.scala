package ua.ucu.edu

import java.util.{Date, Properties}
import java.util.concurrent.TimeUnit

import org.apache.kafka.streams.kstream.{JoinWindows, KStream}
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration

object DummyStreamingApp extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming_app")
//  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Config.KafkaBrokers))

  props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, Long.box(5 * 1000))
  props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, Long.box(0))

  import Serdes._

  val builder = new StreamsBuilder

  Thread.sleep(30000);

  val newsStream = builder.stream[String, String]("news-data")
  val tweetsStream = builder.stream[String, String]("twitter-data")
  val stocksStream = builder.stream[String, String]("stocks-data")


  def text_processing(text:String):String ={
      val text_filtered = text
        .replaceAll("""[.!?\\/;,—\-_():]""", "")

        // TODO Improve stripping extra whitespaces
        .replaceAll("""(?m)\s+$""", "")
        .toLowerCase

      val words = text_filtered.split(" ")
      val bag_of_words = words.groupBy((word: String) => word).mapValues(_.length)

    bag_of_words.toString()
  }


  val resultStream = tweetsStream.join(newsStream)(
    ((tweet_text, news_text) => {
      text_processing(tweet_text + news_text)
    }),
    JoinWindows.of(20000)
  ).join(stocksStream)(
    ((news_tweets, stock) => {
      s"bag of words: $news_tweets; stock: $stock"
    }),
    JoinWindows.of(20000)
  )

  resultStream.foreach { (k, v) =>
    logger.info(s"RESULT record processed $k->$v")
  }

  resultStream.to("output-topic")

  newsStream.foreach { (k, v) =>
    logger.info(s"NEWS record processed $k->$v")
  }

  tweetsStream.foreach { (k, v) =>
    logger.info(s"TWEETS record processed $k->$v")
  }

  stocksStream.foreach { (k, v) =>
    logger.info(s"STOCKS record processed $k->$v")
  }
  val streams = new KafkaStreams(builder.build(), props)
  streams.cleanUp()
  streams.start()

  sys.addShutdownHook {
    streams.close(10, TimeUnit.SECONDS)
  }

  object Config {
    val KafkaBrokers = "KAFKA_BROKERS"
  }

}
