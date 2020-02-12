package ua.ucu.edu

import spray.json._
import DefaultJsonProtocol._

object twitter_data_preparation extends App {

    // Change to your path
    val path_to_tweets_json = "C:\\Users\\orest.rehusevych\\Documents\\Masters\\Scala\\pfsp_final_project\\data\\tweets_final.json"

    val tweets_json = scala.io.Source.fromFile(path_to_tweets_json)
    val tweets_json_str = try tweets_json.mkString finally tweets_json.close()
    var tweets_array = tweets_json_str.stripMargin.parseJson

    case class TwitterRecord(date: String,
                             time: String,
                             tweet: String)

    case class TwitterRecordProcessed(date: String,
                             time: String,
                             tweet: String,
                             bag_of_words: Map[String, Int])

    implicit val tweetFormat: RootJsonFormat[TwitterRecord] = jsonFormat3(TwitterRecord)

    val musk_twitter_records: List[TwitterRecord] = tweets_array.convertTo[List[TwitterRecord]]
    var musk_twitter_processed_records: List[TwitterRecordProcessed] = List()

    for (twitter_record <- musk_twitter_records) {
        val date_transformed = twitter_record.date.replace('.', '-')

        val tweet_filtered = twitter_record.tweet
            .replaceAll("""[.!?\\/;,—\-_():]""", "")
            .replaceAll("""(?m)\s+$""", "")
            .toLowerCase

        val words = tweet_filtered.split(" ")
        val bag_of_words = words.groupBy((word: String) => word).mapValues(_.length)

        musk_twitter_processed_records = musk_twitter_processed_records :+ TwitterRecordProcessed(date_transformed, twitter_record.time, tweet_filtered, bag_of_words)
    }

    musk_twitter_processed_records.foreach(record => println("Date: %s - Time: %s - Bag-of-Words: %s".format(record.date, record.time, record.bag_of_words)))
}
