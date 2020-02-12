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

    implicit val tweetFormat: RootJsonFormat[TwitterRecord] = jsonFormat3(TwitterRecord)

    val musk_twitter_records: List[TwitterRecord] = tweets_array.convertTo[List[TwitterRecord]]
    var musk_twitter_processed_records: List[TwitterRecord] = List()

    for (twitter_record <- musk_twitter_records) {
        val date_transformed = twitter_record.date.replace('.', '-')

        val tweet_filtered = twitter_record.tweet
          .replaceAll("""[.!?\\/;,—\-_():]""", "")
          .replaceAll("""(?m)\s+$""", "")
          .toLowerCase
        musk_twitter_processed_records = musk_twitter_processed_records :+ TwitterRecord(date_transformed, twitter_record.time, tweet_filtered)
    }

    musk_twitter_processed_records.foreach(record => println("Date: %s - Time: %s - Tweet: %s".format(record.date, record.time, record.tweet)))
}
