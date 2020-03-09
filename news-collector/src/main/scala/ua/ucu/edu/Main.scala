package ua.ucu.edu

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.{LocalDate, ZoneId}
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.concurrent.duration._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.stream.ActorMaterializer
import play.api.libs.json._
import java.text.SimpleDateFormat

import org.slf4j.{Logger, LoggerFactory}
import com.typesafe.config.ConfigFactory

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.parseFile(new File("/project/application.conf"))

  object Config {
    val KafkaBrokers = "KAFKA_BROKERS"
  }

    class NewsActor extends Actor {

      case class NewsRecord(created_at: Date, title: String)

  def getNewsByDate(date: Date): Future[HttpResponse] = {

    val formatter = new SimpleDateFormat("dd/MM/yyyy")
    val todayWithZeroTime = formatter.parse(formatter.format(date))

    val today_timestamp = todayWithZeroTime.getTime()/1000
    val next_date_timestamp = today_timestamp + 24*60*60

    val endpoint = Uri(s"https://hn.algolia.com/api/v1/search_by_date?query=Tesla&tags=story&numericFilters=created_at_i>=${today_timestamp},created_at_i<${next_date_timestamp}")
    Http().singleRequest(HttpRequest(uri = endpoint))
  }

      def processNews(input_data: String): Seq[String] = {
        val json = Json.parse(input_data)
        val hits = json  \\ "title"
        //          take every second element
        hits.zipWithIndex
          .filter { case (_, i) => (i + 1) % 2 != 1 }
          .map { case (e, _) => e.toString() }
      }

      val BrokerList: String = System.getenv(Config.KafkaBrokers)
      val Topic = "news-data"
      val props = new Properties()
      props.put("bootstrap.servers", BrokerList)
      props.put("client.id", "news-collector")
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      val producer = new KafkaProducer[String, String](props)

      def receive = {
        case date: Date => {
          getNewsByDate(date)
            .flatMap(res => res.entity.toStrict(5.seconds))
            .andThen {
              case Success(entity) => {
                val res_titles = processNews(entity.data.decodeString(StandardCharsets.UTF_8)).toSet.mkString("; ")

                val data = new ProducerRecord[String, String](Topic, date.toString(), res_titles)
                producer.send(data)
                logger.info(s"[$Topic] $date $res_titles")
              }
              case Failure(e) => println("something went wrong: " + e)
            }
        }
      }

      override def postStop():Unit = {
        producer.close()
      }
    }

  val day_duration = config.getInt("simulation.day_duration.value")

  val newsActor = system.actorOf(Props[NewsActor], "news-actor")

  def dates(fromDate: LocalDate): Stream[LocalDate] = {
    fromDate #:: dates(fromDate plusDays 1 )
  }

  def get_start_date() : LocalDate = {
    val start_date = config.getString("simulation.start_date.value")
    LocalDate.parse(start_date)
  }

  def get_end_date() : LocalDate = {
    val start_date = config.getString("simulation.end_date.value")
    LocalDate.parse(start_date)
  }

  val start_date = get_start_date()
  val end_date = get_end_date()

  Thread.sleep(30000);
  for (i<-dates(start_date).takeWhile(_.isBefore(end_date)).toList){
    newsActor ! java.util.Date.from(i.atStartOfDay()
      .atZone(ZoneId.systemDefault())
      .toInstant())

    Thread.sleep(day_duration*1000);
  }

}
