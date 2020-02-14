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

  class TwitterActor extends Actor {

    //  TODO: replace!!
    //  val BrokerList: String = System.getenv("KAFKA_BROKERS")
    //  for test
    val BrokerList: String = "localhost:9092"
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
        val prod_rec = new ProducerRecord[String, String](Topic, twitter_data)
        producer.send(prod_rec)
      }
    }

    override def postStop():Unit = {
      producer.close()
    }
  }

//  for test
  def get_current_date() : Date = {
    import java.text.SimpleDateFormat

    val formatter = new SimpleDateFormat("dd/MM/yyyy")
    formatter.parse(formatter.format(new Date()))
  }

  def get_yesterday_date(): Date = {
    new Date( get_current_date().getTime() -24*60*60*1000 )
  }

  val twitterActor = system.actorOf(Props[TwitterActor], "twitter-actor")

  system.scheduler.schedule(Duration.Zero, 1 minutes, twitterActor, get_yesterday_date())

}
