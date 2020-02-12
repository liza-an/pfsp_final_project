package ua.ucu.edu

import spray.json._
import DefaultJsonProtocol._

object stocks_data_preparation extends App {

  // Change to your path
  val path_to_stocks_json = "C:\\Users\\orest.rehusevych\\Documents\\Masters\\Scala\\pfsp_final_project\\data\\tesla_stocks_final.json"

  val stocks_json = scala.io.Source.fromFile(path_to_stocks_json)
  val stocks_json_str = try stocks_json.mkString finally stocks_json.close()
  var stocks_array = stocks_json_str.stripMargin.parseJson

  case class StocksRecord(date: String,
                           open: Float,
                           high: Float,
                           low: Float,
                           close: Float,
                           adjusted_close: Float,
                           volume: Float,
                           dividend_amount: Float,
                           split_coefficient: Float)

  implicit val StocksRecordFormat: RootJsonFormat[StocksRecord] = jsonFormat9(StocksRecord)

  val tesla_stocks: List[StocksRecord] = stocks_array.convertTo[List[StocksRecord]]

  tesla_stocks.foreach(record => println("Date: %s -  Open: %s - Close: %s".format(record.date, record.open, record.close)))
}
