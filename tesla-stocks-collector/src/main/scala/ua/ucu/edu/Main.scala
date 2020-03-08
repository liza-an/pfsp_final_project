package ua.ucu.edu

import java.time.{LocalDate, ZoneId}

import scala.concurrent.duration._
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import akka.stream.ActorMaterializer
import org.slf4j.{Logger, LoggerFactory}
//import ua.ucu.edu.Main.{day_duration, newsActor}

//TODO dates
//TODO add logs

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logger: Logger = LoggerFactory.getLogger(getClass)

  class StocksActor extends Actor {

      val BrokerList: String = System.getenv("KAFKA_BROKERS")
    //  for test
//    val BrokerList: String = "localhost:9092"
    val Topic = "stocks-data"
    val props = new Properties()
    props.put("bootstrap.servers", BrokerList)
    props.put("client.id", "news-collector")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    def receive = {
      case date: Date => {
        val stocks_data = stocks_data_preparation.getStocksByDate(date).getOrElse("").toString
//        val prod_rec = new ProducerRecord[String, String](Topic, stocks_data)
        val prod_rec = new ProducerRecord[String, String](Topic, date.toString(), stocks_data)
        producer.send(prod_rec)
        logger.info(s"[$Topic] $date $stocks_data")
      }
    }

    override def postStop():Unit = {
      producer.close()
    }
  }

  //TODO read from the confug file
  val day_duration_seconds = 20

  val stocksActor = system.actorOf(Props[StocksActor], "stocks-actor")

//  system.scheduler.schedule(Duration.Zero, day_duration_seconds seconds, stocksActor, get_yesterday_date())

  def dates(fromDate: LocalDate): Stream[LocalDate] = {
    fromDate #:: dates(fromDate plusDays 1 )
  }

  def get_start_date() : LocalDate = {
    LocalDate.parse("2019-02-02")
  }

  def get_end_date() : LocalDate = {
    LocalDate.parse("2019-02-22")
  }

  val start_date = get_start_date()
  val end_date = get_end_date()

  val day_duration = 20

  Thread.sleep(30000);
  for (i<-dates(start_date).takeWhile(_.isBefore(end_date)).toList){
    stocksActor ! java.util.Date.from(i.atStartOfDay()
      .atZone(ZoneId.systemDefault())
      .toInstant())

    Thread.sleep(day_duration*1000);
  }

}
