package ua.ucu.edu

// For date formatting
import java.text.SimpleDateFormat
import java.util.Date

// For json parsing
import spray.json._
import DefaultJsonProtocol._

object stocks_data_preparation extends App {

  // Change to your path
  val path_to_stocks_json = "C:\\Users\\orest.rehusevych\\Documents\\Masters\\Scala\\pfsp_final_project\\data\\tesla_stocks_final.json"

  val stocks_json = scala.io.Source.fromFile(path_to_stocks_json)
  val stocks_json_str = try stocks_json.mkString finally stocks_json.close()
  var stocks_array = stocks_json_str.stripMargin.parseJson

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

  case class StocksRecordProcessed(date: Date,
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
  var tesla_stocks_processed: List[StocksRecordProcessed] = List()

  for (tesla_stock_record <- tesla_stocks) {
    val date_changed_type = new SimpleDateFormat(dateFormat).parse(tesla_stock_record.date)
    tesla_stocks_processed = tesla_stocks_processed :+
      StocksRecordProcessed(
        date_changed_type, tesla_stock_record.open, tesla_stock_record.high,
        tesla_stock_record.low, tesla_stock_record.close, tesla_stock_record.adjusted_close,
        tesla_stock_record.volume, tesla_stock_record.dividend_amount, tesla_stock_record.split_coefficient)
  }

  tesla_stocks_processed.foreach{
    record =>
      println("Date: %s - Open: %s - Close: %s"
        .format(
          new SimpleDateFormat(dateFormat).format(record.date),
          record.open,
          record.close)
      )
  }
}
