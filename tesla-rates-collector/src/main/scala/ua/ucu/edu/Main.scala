//package ua.ucu.edu
//
////import ua.ucu.edu.kafka.DummyDataProducer
//import java.nio.charset.StandardCharsets
//import java.util.Properties
//
//import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
////import ua.ucu.edu.Main.{ActorRequest, tickActor}
//import ua.ucu.edu.kafka.DummyDataProducer.logger
//
////import akka.actor.{Actor, Props, Terminated}
//
//import scala.concurrent.duration._
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//
//import scala.concurrent.Future
//import scala.util.{Failure, Success}
//import java.time.Instant
//
//import akka.stream.ActorMaterializer
//
//import play.api.libs.json._
//// \TODO replace localhost
////TODO change date to yesterday date
////TODO wrap in actor
////TODO case class for news data
//
//object Main extends App {
//
//  implicit val system = akka.actor.ActorSystem()
//  implicit val executionContext = system.dispatcher
//  implicit val materializer = ActorMaterializer()
//
//  //  val Tick = "tick"
//  //
//  //  case class ActorRequest(date: DateTime)
//  //
//  //  class TickActor extends Actor {
//  //    def receive = {
//  //      case ActorRequest => getNewsByDate(x)
//  //    }
//  //  }
//  //  val tickActor = system.actorOf(Props(classOf[TickActor], this))
//  //
//  //  val now = ActorRequest(DateTime.now)
//  //
//  //  system.scheduler.schedule(Duration.Zero, 1 minute, tickActor, now)
//
//  //  case class News(crated_at: String, header: String)
//
//  //  case class InputNews(created_at: DateTime, title: String, url: String, author:String,)
//
//  val BrokerList: String = "localhost:2181"
//  val Topic = "news-data"
//  val props = new Properties()
//  props.put("bootstrap.servers", BrokerList)
//  //  props.put("client.id", "news")
//  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//  //  logger.info("initializing producer")
//  val producer = new KafkaProducer[String, String](props)
//
//
//  def getNewsByDate(date: DateTime): Future[HttpResponse] = {
//    val yesterday = Instant.now.getEpochSecond()
//    val endpoint = Uri(s"https://hn.algolia.com/api/v1/search_by_date?query=Tesla&tags=story&numericFilters=created_at_i<${yesterday}")
//    Http().singleRequest(HttpRequest(uri = endpoint))
//  }
//
//  def processNews(input_data: String): String = {
//    val json: JsValue = Json.parse(input_data)
//    val hits = json  \\ "title"
//    //          take every second element of hits
//    hits.zipWithIndex
//      .filter { case (_, i) => (i + 1) % 2 != 1 }
//      .map { case (e, _) => e.toString() }
//      .reduce((a,b)=> a+b).replace("\"", "").toLowerCase()
//    //          TODO processing
//  }
//
//  system.scheduler.schedule(0 seconds, 30 seconds , new Runnable {
//    override def run(): Unit = {
//      getNewsByDate(DateTime.now)
//        .flatMap(res => res.entity.toStrict(5.seconds))
//        .andThen {
//          case Success(entity) => {
//            val res_titles = processNews(entity.data.decodeString(StandardCharsets.UTF_8))
//            val data = new ProducerRecord[String, String](Topic, res_titles)
//            producer.send(data)
//            println("done")
//
//          }
//          case Failure(e) => println("something went wrong: " + e)
//        }
//      //        .flatMap(_ => Http().shutdownAllConnectionPools())
//      //        .flatMap(_ => system.terminate())
//    }
//  })
//
//  //  producer.close()
//
//
//  //  object Config {
//  //    val KafkaBrokers = "KAFKA_BROKERS"
//  //  }
//}
