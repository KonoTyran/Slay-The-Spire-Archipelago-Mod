package ArchipelagoMW.patches;

import ArchipelagoMW.ui.RewardMenu.ArchipelagoRewardScreen;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CardRewardScreen;

public class CardRewardScreenPatch {

    @SpirePatch(clz = CardRewardScreen.class, method = "takeReward")
    public static class PreviousScreenPatch {

        @SpirePrefixPatch()
        public static SpireReturn<Integer> Prefix(CardRewardScreen __instance, RewardItem ___rItem) {
            if (___rItem != null) {
                if (RewardItemPatch.CustomFields.apReward.get(___rItem)) {
                    ArchipelagoRewardScreen.rewards.remove(___rItem);
                    ArchipelagoRewardScreen.positionRewards();
                    AbstractDungeon.dynamicBanner.appear();
                    AbstractDungeon.overlayMenu.cancelButton.show(ArchipelagoRewardScreen.TEXT[0]);
                    return SpireReturn.Return(null);
                }
            }

            return SpireReturn.Continue();
        }
    }

}
