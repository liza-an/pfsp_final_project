package ua.ucu.edu

import java.nio.charset.StandardCharsets
import java.util.Properties

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
//import ua.ucu.edu.Main.{ActorRequest, tickActor}
//import ua.ucu.edu.kafka.DummyDataProducer.logger

//import akka.actor.{Actor, Props, Terminated}

import scala.concurrent.duration._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import java.time.Instant

import akka.stream.ActorMaterializer

import play.api.libs.json._

//TODO dates
//TODO add logs
//TODO data preprocessing

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  object Config {
    val KafkaBrokers = "KAFKA_BROKERS"
  }

    class NewsActor extends Actor {

//      implicit val system = akka.actor.ActorSystem()
//      implicit val executionContext = system.dispatcher
//      implicit val materializer = ActorMaterializer()

      def getNewsByDate(date: DateTime): Future[HttpResponse] = {
        val yesterday = Instant.now.getEpochSecond()
        val endpoint = Uri(s"https://hn.algolia.com/api/v1/search_by_date?query=Tesla&tags=story&numericFilters=created_at_i<${yesterday}")
        Http().singleRequest(HttpRequest(uri = endpoint))
      }

      def processNews(input_data: String): Seq[String] = {
        val json: JsValue = Json.parse(input_data)
        val hits = json  \\ "title"
        //          take every second element of hits
        hits.zipWithIndex
          .filter { case (_, i) => (i + 1) % 2 != 1 }
          .map { case (e, _) => e.toString() }
      }

      //  TODO: replace!!
      //  val BrokerList: String = System.getenv(Config.KafkaBrokers)
      //  for test
      val BrokerList: String = "localhost:9092"
      val Topic = "news-data"
      val props = new Properties()
      props.put("bootstrap.servers", BrokerList)
      props.put("client.id", "news-collector")
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      val producer = new KafkaProducer[String, String](props)

      def receive = {
        case date: DateTime => {
          getNewsByDate(date)
            .flatMap(res => res.entity.toStrict(5.seconds))
            .andThen {
              case Success(entity) => {
                val res_titles = processNews(entity.data.decodeString(StandardCharsets.UTF_8))
                res_titles.foreach(title => {
                  val data = new ProducerRecord[String, String](Topic, title)
                  producer.send(data)
                  println(DateTime.now)
                })
              }
              case Failure(e) => println("something went wrong: " + e)
            }
          //        .flatMap(_ => Http().shutdownAllConnectionPools())
          //        .flatMap(_ => system.terminate())
        }
      }

      override def postStop():Unit = {
        producer.close()
      }
    }


  val newsActor = system.actorOf(Props[NewsActor], "news-actor")

  system.scheduler.schedule(Duration.Zero, 1 minute, newsActor, DateTime.now)

}
