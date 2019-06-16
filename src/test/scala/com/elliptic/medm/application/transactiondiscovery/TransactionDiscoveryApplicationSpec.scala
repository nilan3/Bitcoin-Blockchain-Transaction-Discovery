package com.elliptic.medm.application.transactiondiscovery

import com.elliptic.medm.common.SparkSessionHelper
import com.elliptic.medm.utils.{AppConfig, Utilities}
import org.scalatest.FunSuite

class TransactionDiscoveryApplicationSpec extends FunSuite with SparkSessionHelper {
  private val configuration: AppConfig = Utilities.loadConfig(Array("configurations/transaction_discovery.yml"))
  protected val processor = new TransactionDiscoveryApplication(configuration)

  test("test remove duplicates method") {
    val inputRows = List(
      InputRow("complete","19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF","0x53e85c83f5dc0ee6670d9a32ab9cadb9d46f1a3b",0.00413633,"BTC","0.05515456","ETH","0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c","https://etherscan.io/tx/0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c"),
      InputRow("complete","19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF","0x53e85c83f5dc0ee6670d9a32ab9cadb9d46f1a3b",0.00413634,"BTC","0.05515457","ETH","0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2","https://etherscan.io/tx/0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2"),
      InputRow("complete","19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF","0x53e85c83f5dc0ee6670d9a32ab9cadb9d46f1a3b",0.00413634,"BTC","0.05515457","ETH","0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2","https://etherscan.io/tx/0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2")
    )
    val inputState = AddressState(List("0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c"))
    val expected = List(InputRow("complete","19uU1ULcuhkexRzZbBAN4QaGL5EznPtPYF","0x53e85c83f5dc0ee6670d9a32ab9cadb9d46f1a3b",0.00413634,"BTC","0.05515457","ETH","0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2","https://etherscan.io/tx/0xbdac05d0356e5e0bb9c2c42acbef7ab9d75e440dea5e266581a61b80ab61261c2"))
    val actual = processor.removeDuplicates(inputRows, inputState)
    assert(actual == expected)

  }


}
