package com.setlone.app;

import static com.setlone.app.assertions.Should.shouldSee;
import static com.setlone.app.steps.Steps.createNewWallet;
import static com.setlone.app.steps.Steps.gotoWalletPage;
import static com.setlone.app.steps.Steps.selectCurrency;

import org.junit.Test;

public class CurrencyTest extends BaseE2ETest
{

    @Test
    public void should_switch_currency()
    {
        createNewWallet();

        selectCurrency("CNY");
        gotoWalletPage();
        shouldSee("¥");

        selectCurrency("IDR");
        gotoWalletPage();
        shouldSee("Rp");
    }

}
