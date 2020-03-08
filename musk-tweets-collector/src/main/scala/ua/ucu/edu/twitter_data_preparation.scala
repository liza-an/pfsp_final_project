package ua.ucu.edu

// For date formatting
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.BufferedSource

// For json parsing
import spray.json._
import DefaultJsonProtocol._

object twitter_data_preparation {

    // Change to your path
    val path_to_tweets_json = "tweets_final.json"

    val tweets_json: BufferedSource = scala.io.Source.fromResource(path_to_tweets_json)
    val tweets_json_str: String = try tweets_json.mkString finally tweets_json.close()
    var tweets_array: JsValue = tweets_json_str.stripMargin.parseJson

    private val dateFormat = "dd-MM-yyyy"

    case class TwitterRecord(date: String,
                             time: String,
                             tweet: String)

//    case class TwitterRecordProcessed(date: Date,
//                                      time: String,
//                                      tweet: String,
//                                      bag_of_words: Map[String, Int])

    implicit val tweetFormat: RootJsonFormat[TwitterRecord] = jsonFormat3(TwitterRecord)

    val musk_twitter_records: List[TwitterRecord] = tweets_array.convertTo[List[TwitterRecord]]
//    var musk_twitter_records_processed: List[TwitterRecordProcessed] = List()
    var musk_twitter_records_processed: Map[Date, String] = Map()

    for (twitter_record <- musk_twitter_records) {
        val date_transformed = twitter_record.date.replace('.', '-')

        val date_changed_type = new SimpleDateFormat(dateFormat).parse(date_transformed)
        musk_twitter_records_processed += date_changed_type -> twitter_record.tweet
//        val tweet_filtered = twitter_record.tweet
//          .replaceAll("""[.!?\\/;,—\-_():]""", "")
//
//          .replaceAll("""(?m)\s+$""", "")
//          .toLowerCase

//        val words = tweet_filtered.split(" ")
//        val bag_of_words = words.groupBy((word: String) => word).mapValues(_.length)

//        musk_twitter_records_processed = musk_twitter_records_processed :+ TwitterRecordProcessed(date_changed_type, twitter_record.time, tweet_filtered, bag_of_words)
    }

    def getTweetByDate(date: Date): Option[String] = musk_twitter_records_processed.get(date)

//    musk_twitter_records_processed.keys.foreach{
//        date =>
//        println(date)
//            println("Date: %s - Tweet: %s"
//              .format(
//                  new SimpleDateFormat(dateFormat).format(date),
//                  getTweetByDate(date))
//            )
//    }

}
