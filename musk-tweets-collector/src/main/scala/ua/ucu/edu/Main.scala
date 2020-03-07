package ua.ucu.edu

import scala.concurrent.duration._
import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import akka.stream.ActorMaterializer
import ua.ucu.edu.twitter_data_preparation.TwitterRecord

//TODO dates
//TODO add logs

object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  class TwitterActor extends Actor {

    //  TODO: replace!!
    val BrokerList: String = System.getenv("KAFKA_BROKERS")
    //  for test
//    val BrokerList: String = "localhost:9092"
    val Topic = "twitter-data"
    val props = new Properties()
    props.put("bootstrap.servers", BrokerList)
    props.put("client.id", "news-collector")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    def receive = {
      case date: Date => {
        val twitter_data = twitter_data_preparation.getTweetByDate(date).getOrElse("")
//        TwitterRecord
        val prod_rec = new ProducerRecord[String, String](Topic, s"${date} - ${twitter_data}")

        producer.send(prod_rec)
        println("message is sent")
      }
    }

    override def postStop():Unit = {
      producer.close()
    }
  }

//  for test
//  def update_date() : Date = {
//    get_start_date()
//  }

  def get_start_date(): Date = {
    import java.text.SimpleDateFormat

    val current_date = System.getenv("START_DATE")
    val parser = new SimpleDateFormat("MM/dd/yyyy")
    val date = parser.parse(current_date)

    date
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

  val twitterActor = system.actorOf(Props[TwitterActor], "twitter-actor")

  println("twitter-actor")

//TODO read from the confug file
  val day_duration = 60

//  val current_date = get_start_date()

  system.scheduler.schedule(Duration.Zero, day_duration seconds, twitterActor, get_yesterday_date())

}
