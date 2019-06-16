package com.elliptic.medm.application.transactioncollection

import com.elliptic.medm.common.SparkSessionHelper
import com.elliptic.medm.utils.{AppConfig, Utilities}
import org.scalatest.FunSuite

class TransactionCollectionApplicationSpec extends FunSuite with SparkSessionHelper {
  private val configuration: AppConfig = Utilities.loadConfig(Array("configurations/transaction_collection.yml"))
  protected val processor = new TransactionCollectionApplication(configuration)

  test("test successful api call") {
    val inputUrl = "https://shapeshift.io/txStat/19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF"
    val expected = s"""{"status":"complete","address":"19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF","withdraw":"0x53e85c83f5dc0ee6670d9a32ab9cadb9d46f1a3b","incomingCoin":0.00413633,"incomingType":"BTC","outgoingCoin":"0.05515456","outgoingType":"ETH","transaction":"0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c","transactionURL":"https://etherscan.io/tx/0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c"}"""
    val actual = processor.queryApi(inputUrl)
    assert(actual == expected)

  }

  test("test timed out api call") {
    val inputUrl = "https://shapeshift2.io/txStat/19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF"
    val expected = s"""{"error": "unable to obtain response from $inputUrl"}"""
    val actual = processor.queryApi(inputUrl)
    assert(actual == expected)

  }


}
