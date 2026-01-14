package com.setlone.app;

import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.setlone.app.steps.Steps.createNewWallet;
import static com.setlone.app.steps.Steps.gotoSettingsPage;
import static com.setlone.app.steps.Steps.selectMenu;
import static com.setlone.app.util.Helper.click;

import org.junit.Test;

public class I18nTest extends BaseE2ETest
{

    @Test
    public void should_switch_language()
    {
        createNewWallet();
        gotoSettingsPage();

        selectMenu("Change Language");
        click(withText("Chinese"));
        pressBack();

        selectMenu("?´æ¢è¯??");
        click(withText("è¥¿ç­?™è?"));
        pressBack();

        selectMenu("Cambiar idioma");
        click(withText("FrancÃ©s"));
        pressBack();

        selectMenu("Changer Langue");
        click(withText("Vietnamien"));
        pressBack();

        selectMenu("Thay Ä‘á»•i ngÃ´n ngá»?);
        click(withText("Tiáº¿ng Miáº¿n Äiá»‡n"));
        pressBack();

        selectMenu("?˜á€¬á€á€¬á€…á€€?¬á€¸á€•á€¼á€±á€¬á€„á€ºá€¸á€™á€Šá€?);
        click(withText("?¡á€„á€ºá€’á€?€?€”á€?€¸á€›á€¾á€¬á€?));
        pressBack();
    }
}
