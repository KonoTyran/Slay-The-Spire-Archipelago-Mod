package ArchipelagoMW.patches;

import ArchipelagoMW.LocationTracker;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import gg.archipelago.client.parts.NetworkItem;
import javassist.CtBehavior;

import java.util.ArrayList;
import java.util.Iterator;

public class CombatRewardScreenPatch {

    @SpirePatch(clz = CombatRewardScreen.class, method = "open", paramtypez = {})
    public static class openPatch {

        @SpireInsertPatch(locator = Locator.class, localvars = {})
        public static void Insert(CombatRewardScreen __instance, ArrayList<RewardItem> ___rewards) {
            Iterator<RewardItem> rewardItemIterator = ___rewards.iterator();
            ArrayList<RewardItem> toAdd = new ArrayList<>();
            while (rewardItemIterator.hasNext()) {
                RewardItem reward = rewardItemIterator.next();
                NetworkItem item = null;
                switch (reward.type) {
                    case CARD:
                        item = LocationTracker.sendCardDraw(reward);
                        break;
                    case RELIC:
                        item = LocationTracker.sendRelic();
                        break;
                }

                if (item != null) {
                    rewardItemIterator.remove();
                    RewardItem replacementReward = new RewardItem(1);
                    replacementReward.goldAmt = 0;
                    replacementReward.text = item.itemName + " NL " + item.playerName;
                    replacementReward.type = RewardItemPatch.RewardType.ARCHIPELAGO_LOCATION;
                    toAdd.add(replacementReward);
                }
            }
            ___rewards.addAll(toAdd);
        }

        private static class Locator extends SpireInsertLocator {

            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher reward = new Matcher.MethodCallMatcher(CombatRewardScreen.class, "setupItemReward");
                int[] match = LineFinder.findInOrder(ctBehavior, reward);
                for (int i = 0; i < match.length; i++) {
                    match[i]++;
                }
                return match;
            }
        }
    }
}
