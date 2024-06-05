package com.ivy.core.domain.exchange_rates

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.doesNotCorrespond
import com.ivy.MainCoroutineExtension
import com.ivy.core.domain.action.settings.basecurrency.BaseCurrencyFlow
import com.ivy.core.persistence.algorithm.calc.Rate
import com.ivy.exchangeRates.RatesStateFlow
import com.ivy.exchangeRates.data.RateUi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MainCoroutineExtension::class)
class RatesStateFlowTest {
    private lateinit var rateFlow: RatesStateFlow
    private lateinit var baseCurrencyFlow: BaseCurrencyFlow
    private lateinit var ratesDaoFake: RatesDaoFake

    @BeforeEach
    fun setUp() {
        baseCurrencyFlow = mockk()
        every { baseCurrencyFlow.invoke() } returns flowOf("", "EUR")

        ratesDaoFake = RatesDaoFake()

        rateFlow = RatesStateFlow(
            baseCurrencyFlow = baseCurrencyFlow,
            ratesDao = ratesDaoFake
        )
    }

    @Test
    fun `Test rates flow emission`() = runTest {
        rateFlow().test {
            awaitItem()

            val emission1 = awaitItem()

            val overrideRate = RateUi(from = "EUR", to ="USD", rate = 1.3)
            assertThat(emission1.automatic).doesNotContain(overrideRate)
            assertThat(emission1.manual).contains(overrideRate)

            ratesDaoFake.rates.value += Rate(rate = 0.00004, currency = "BTC")

            val emission2 = awaitItem()
            val rate = RateUi(from = "EUR", to = "BTC", rate = 0.00004)
            assertThat(emission2.automatic).contains(rate)
            assertThat(emission2.manual).doesNotContain(rate)
        }
    }
}