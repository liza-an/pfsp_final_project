package ua.ucu.edu

import java.io.File
import java.time.{LocalDate, ZoneId}
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.parseFile(new File("/project/application.conf"))

  class StocksActor extends Actor {

    val BrokerList: String = System.getenv("KAFKA_BROKERS")
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
        val prod_rec = new ProducerRecord[String, String](Topic, date.toString(), stocks_data)
        producer.send(prod_rec)
        logger.info(s"[$Topic] $date $stocks_data")
      }
    }

    override def postStop():Unit = {
      producer.close()
    }
  }

  val stocksActor = system.actorOf(Props[StocksActor], "stocks-actor")

//  system.scheduler.schedule(Duration.Zero, day_duration_seconds seconds, stocksActor, get_yesterday_date())

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

  val day_duration = config.getInt("simulation.day_duration.value")

  Thread.sleep(30000);
  for (i<-dates(start_date).takeWhile(_.isBefore(end_date)).toList){
    stocksActor ! java.util.Date.from(i.atStartOfDay()
      .atZone(ZoneId.systemDefault())
      .toInstant())

    Thread.sleep(day_duration*1000);
  }

}
