package ua.ucu.edu

import java.nio.charset.StandardCharsets
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
//import kafka.utils.Json
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
//import spray.json.JsValue
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
//import play.libs.Json

//import java.text.DateFormat
import java.text.SimpleDateFormat

//TODO dates
//TODO add logs

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  object Config {
    val KafkaBrokers = "KAFKA_BROKERS"
  }

    class NewsActor extends Actor {

      case class NewsRecord(created_at: Date, title: String)

//      news created after date
      def geNewsByDate(date: Date): Future[HttpResponse] = {

        val formatter = new SimpleDateFormat("dd/MM/yyyy")
//        val dayWithZeroTime = formatter.parse(formatter.format(date))

//        val date_timestamp = dayWithZeroTime.getTime()/1000
        val date_timestamp = 1547251200
//        val yesterday_timestamp = today_timestamp - 24*60*60

        val endpoint = Uri(s"https://hn.algolia.com/api/v1/search_by_date?query=Tesla&tags=story&numericFilters=created_at_i>=${date_timestamp}")
        Http().singleRequest(HttpRequest(uri = endpoint))
      }

      def processNews(input_data: String): Seq[String] = {
        val json = Json.parse(input_data)
        val hits = json  \\ "title"
        //          take every second element of hits
        hits.zipWithIndex
          .filter { case (_, i) => (i + 1) % 2 != 1 }
          .map { case (e, _) => e.toString() }
      }

      //  TODO: replace!!
        val BrokerList: String = System.getenv(Config.KafkaBrokers)
      //  for test
//      val BrokerList: String = "localhost:9092"
      val Topic = "news-data"
      val props = new Properties()
      props.put("bootstrap.servers", BrokerList)
      props.put("client.id", "news-collector")
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      val producer = new KafkaProducer[String, String](props)

      def receive = {
        case date: Date => {
          geNewsByDate(date)
            .flatMap(res => res.entity.toStrict(5.seconds))
            .andThen {
              case Success(entity) => {
                val res_titles = processNews(entity.data.decodeString(StandardCharsets.UTF_8))
                res_titles.foreach(title => {
                  val data = new ProducerRecord[String, String](Topic, title)
                  producer.send(data)
                  println("message is sent")
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

  //  for test
  def get_current_date() : Date = {
    import java.text.SimpleDateFormat

    val input = "Wed Feb 02 00:00:00 EET 2019"
    val parser = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy")
    val date = parser.parse(input)

    val formatter = new SimpleDateFormat("dd/MM/yyyy")
    formatter.parse(formatter.format(date))
  }

  def get_yesterday_date(): Date = {
    new Date( get_current_date().getTime() - 24*60*60*1000 )
  }

  //TODO read from the confug file
  val day_duration = 60

  val newsActor = system.actorOf(Props[NewsActor], "news-actor")

  system.scheduler.schedule(Duration.Zero, day_duration seconds, newsActor, get_yesterday_date())

}
