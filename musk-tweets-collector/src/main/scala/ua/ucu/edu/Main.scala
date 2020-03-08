package ua.ucu.edu


import java.util.{Date, Properties}

import akka.actor.{Actor, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import akka.stream.ActorMaterializer
import org.slf4j.{Logger, LoggerFactory}
import java.time.{LocalDate, ZoneId}


object Main extends App {

  implicit val system = akka.actor.ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logger: Logger = LoggerFactory.getLogger(getClass)

  class TwitterActor extends Actor {

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
        val prod_rec = new ProducerRecord[String, String](Topic, date.toString(), twitter_data)

        producer.send(prod_rec)
        logger.info(s"[$Topic] $date $twitter_data")
      }
    }

    override def postStop():Unit = {
      producer.close()
    }
  }

  def get_start_date() : LocalDate = {
    LocalDate.parse("2019-02-02")
  }

  def get_end_date() : LocalDate = {
    LocalDate.parse("2019-02-22")
  }


  val twitterActor = system.actorOf(Props[TwitterActor], "twitter-actor")

//TODO read from the confug file
  val day_duration = 20

  val start_date = get_start_date()
  val end_date = get_end_date()


//  system.scheduler.schedule(Duration.Zero, day_duration.toInt seconds, twitterActor, get_current_date())

  /**
   * Generate an infinite stream of dates starting at `fromDate`.
   */
  def dates(fromDate: LocalDate): Stream[LocalDate] = {
    fromDate #:: dates(fromDate plusDays 1 )
  }

  Thread.sleep(30000);
  for (i<-dates(start_date).takeWhile(_.isBefore(end_date)).toList){
    twitterActor ! java.util.Date.from(i.atStartOfDay()
      .atZone(ZoneId.systemDefault())
      .toInstant())

    Thread.sleep(day_duration*1000);
  }


}
