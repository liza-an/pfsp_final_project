package ua.ucu.edu

import scala.concurrent.duration._
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import akka.stream.ActorMaterializer

//TODO dates
//TODO add logs

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  class StocksActor extends Actor {

    //  TODO: replace!!
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
        val prod_rec = new ProducerRecord[String, String](Topic, s"${date} - ${stocks_data}")
        producer.send(prod_rec)
        println("message is sent")
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
    new Date( get_current_date().getTime() -24*60*60*1000 )
  }

  //TODO read from the confug file
  val day_duration_seconds = 60


  val stocksActor = system.actorOf(Props[StocksActor], "stocks-actor")
//  println("stocks-actor")

  system.scheduler.schedule(Duration.Zero, day_duration_seconds seconds, stocksActor, get_yesterday_date())

}
