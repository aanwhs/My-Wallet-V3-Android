package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.junit.Test

class ChangeCryptoAccountsTest {

    @Test
    fun `can change the base account`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            ChangeCryptoFromAccount(
                from = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "")
            )
        ) {
            assertValue {
                it.from.cryptoValue.currency `should be` CryptoCurrency.BCH
                it.to.cryptoValue.currency `should be` CryptoCurrency.ETHER
                it.to.fiatValue.currencyCode `should be` "CAD"
                true
            }
        }
    }

    @Test
    fun `can change the counter account`() {
        given(
            initial("GBP", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            ChangeCryptoToAccount(
                to = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "")
            )
        ) {
            assertValue {
                it.from.cryptoValue.currency `should be` CryptoCurrency.BTC
                it.to.cryptoValue.currency `should be` CryptoCurrency.BCH
                it.to.fiatValue.currencyCode `should be` "GBP"
                true
            }
        }
    }

    @Test
    fun `if the base currency matches the base, they swap`() {
        given(
            initial(
                "GBP",
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "", ""),
                AccountReference.Ethereum("1", "0x123")
            )
        ).on(
            ChangeCryptoFromAccount(
                from = AccountReference.Ethereum("1", "0x456")
            )
        ) {
            assertValue {
                it.from.cryptoValue.currency `should be` CryptoCurrency.ETHER
                it.to.cryptoValue.currency `should be` CryptoCurrency.BTC
                it.to.fiatValue.currencyCode `should be` "GBP"
                true
            }
        }
    }

    @Test
    fun `if the counter currency matches the base, they swap`() {
        given(
            initial(
                "GBP",
                AccountReference.BitcoinLike(CryptoCurrency.BTC, "1", "xpub123"),
                AccountReference.Ethereum("", "")
            )
        ).on(
            ChangeCryptoToAccount(
                to = AccountReference.BitcoinLike(CryptoCurrency.BTC, "2", "xpub456")
            )
        ) {
            assertValue {
                it.from.cryptoValue.currency `should be` CryptoCurrency.ETHER
                it.to.cryptoValue.currency `should be` CryptoCurrency.BTC
                it.to.fiatValue.currencyCode `should be` "GBP"
                true
            }
        }
    }
}
