package ua.ucu.edu

// For date formatting
import java.text.SimpleDateFormat
import java.util.Date

import scala.io.BufferedSource

// For json parsing
import spray.json._
import DefaultJsonProtocol._

object stocks_data_preparation {

  // Change to your path
  val path_to_stocks_json = "tesla_stocks_final.json"

  val stocks_json: BufferedSource = scala.io.Source.fromResource(path_to_stocks_json)
  val stocks_json_str: String = try stocks_json.mkString finally stocks_json.close()
  var stocks_array: JsValue = stocks_json_str.stripMargin.parseJson

  private val dateFormat = "yyyy-MM-dd"

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
  var tesla_stocks_processed: Map[Date, Float] = Map()

  for (tesla_stock_record <- tesla_stocks) {
    val date_changed_type = new SimpleDateFormat(dateFormat).parse(tesla_stock_record.date)
    tesla_stocks_processed += date_changed_type -> tesla_stock_record.open
  }

  def getStocksByDate(date: Date): Option[Float] = tesla_stocks_processed.get(date)

  // Unprint if u want to test

//  tesla_stocks_processed.keys.foreach{
//    date =>
//      println("Date: %s - Open: %s"
//        .format(
//          new SimpleDateFormat(dateFormat).format(date),
//          getStocksByDate(date))
//      )
//  }


//  import java.text.SimpleDateFormat
//
//  val input = "Mon Dec 31 00:00:00 EET 2008"
//  val parser = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy")
//  val date = parser.parse(input)
//
//  val formatter = new SimpleDateFormat("dd/MM/yyyy")
//  val date_ = formatter.parse(formatter.format(date))
//
//  println(date_)
//  println(getStocksByDate(date_))

}
