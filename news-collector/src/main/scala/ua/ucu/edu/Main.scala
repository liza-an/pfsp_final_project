package ua.ucu.edu

import java.nio.charset.StandardCharsets
import java.util.{Date, Properties}

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

import java.text.DateFormat
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

      def getYesterdayNewsByCurrentDate(current_date: Date): Future[HttpResponse] = {

        val formatter = new SimpleDateFormat("dd/MM/yyyy")
        val todayWithZeroTime = formatter.parse(formatter.format(current_date))

        val today_timestamp = todayWithZeroTime.getTime()/1000
        val yesterday_timestamp = today_timestamp - 24*60*60

        val endpoint = Uri(s"https://hn.algolia.com/api/v1/search_by_date?query=Tesla&tags=story&numericFilters=created_at_i>=${yesterday_timestamp},created_at_i<${today_timestamp}")
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
        case date: Date => {
          getYesterdayNewsByCurrentDate(date)
            .flatMap(res => res.entity.toStrict(5.seconds))
            .andThen {
              case Success(entity) => {
                val res_titles = processNews(entity.data.decodeString(StandardCharsets.UTF_8))
//                println(entity)
                res_titles.foreach(title => {
                  val data = new ProducerRecord[String, String](Topic, title)
                  producer.send(data)
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

  def get_current_date() : Date = {
    new Date()
  }

//  val day_duration = System.getenv("DAY_DURATION_SECONDS").toInt
  
  val newsActor = system.actorOf(Props[NewsActor], "news-actor")

  system.scheduler.schedule(Duration.Zero, 1 minute, newsActor, get_current_date())

}
