package ArchipelagoMW.ui.RewardMenu;

import ArchipelagoMW.APClient;
import ArchipelagoMW.ArchipelagoMW;
import ArchipelagoMW.patches.RewardItemPatch;
import basemod.abstracts.CustomScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.*;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.RewardGlowEffect;
import gg.archipelago.client.parts.NetworkItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;


public class ArchipelagoRewardScreen  extends CustomScreen {

    public static class Enum {
        @SpireEnum
        public static AbstractDungeon.CurrentScreen ARCHIPELAGO_REWARD_SCREEN;
    }
    private static final Logger logger = LogManager.getLogger(ArchipelagoRewardScreen.class.getName()); // This is our logger! It prints stuff out in the console.

    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    public static ArrayList<RewardItem> rewards = new ArrayList<>();
    public static int rewardsQueued = 0;
    public ArrayList<AbstractGameEffect> effects = new ArrayList<>();
    public boolean hasTakenAll = false;
    private float rewardAnimTimer = 0.2F;
    private float tipY;
    private final Color uiColor;
    private String tip;

    private final ScrollBar scrollBar;
    private final float scrollLowerBound;
    private float scrollUpperBound;
    private float scrollPosition;
    private float scrollTarget;
    private boolean grabbedScreen;
    private float grabStartY;

    public static int index = 0;
    public static boolean apReward = false;
    public static boolean apRareReward = false;

    public static boolean APScreen = false;

    private OrthographicCamera camera = null;

    private AbstractDungeon.CurrentScreen previousScreen;


    @SpirePatch(clz = AbstractDungeon.class, method = "rollRarity", paramtypez = {Random.class})
    public static class RarityRollPatch {

        @SpireInsertPatch(rloc = 2003 - 2001, localvars = {"roll"})
        public static SpireReturn<AbstractCard.CardRarity> Insert(int roll) {
            if (apReward) {
                final int rareRate = 3;
                if (roll < rareRate) {
                    return SpireReturn.Return(AbstractCard.CardRarity.RARE);
                }
                if (roll < 40) {
                    return SpireReturn.Return(AbstractCard.CardRarity.UNCOMMON);
                }
                return SpireReturn.Return(AbstractCard.CardRarity.COMMON);
            }
            if (apRareReward) {
                return SpireReturn.Return(AbstractCard.CardRarity.RARE);
            }
            return SpireReturn.Continue();
        }
    }



    public ArchipelagoRewardScreen() {
        uiColor = Color.BLACK.cpy();
        tipY = -100.0F * Settings.scale;

        scrollLowerBound = 0.0F;// 50
        scrollUpperBound = 0.0F;// 51
        scrollPosition = 0.0F;// 52
        scrollTarget = 0.0F;// 53
        grabbedScreen = false;// 55
        grabStartY = 0.0F;// 56
        scrollBar = new ScrollBar(new ScrollListener(), (float) Settings.WIDTH / 2.0F + 270.0F * Settings.scale, (float) Settings.HEIGHT / 2.0F - 86.0F * Settings.scale, 500.0F * Settings.scale);// 46
    }

    @Override
    public AbstractDungeon.CurrentScreen curScreen() {
        return Enum.ARCHIPELAGO_REWARD_SCREEN;
    }

    public void reopen() {
        rewardsQueued = 0;
        //AbstractDungeon.getCurrRoom().rewardTime = true;
        //rewardAnimTimer = 0.2F;
        //AbstractDungeon.screen = curScreen();
        //AbstractDungeon.topPanel.unhoverHitboxes();
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.dynamicBanner.appear(TEXT[1]);
        AbstractDungeon.overlayMenu.hideCombatPanels();
        AbstractDungeon.overlayMenu.showBlackScreen();
        //AbstractDungeon.overlayMenu.cancelButton.show(TEXT[0]);
//        if(previousScreen != null) {
//            AbstractDungeon.previousScreen = previousScreen;
//            previousScreen = null;
//        }
    }

    @Override
    public void close() {
        APScreen = false;
        AbstractDungeon.dynamicBanner.hide();
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.overlayMenu.hideBlackScreen();
        if(previousScreen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
            AbstractDungeon.previousScreen = null;
        } else if (MonsterRoom.class.isAssignableFrom(AbstractDungeon.getCurrRoom().getClass())) {
            if (!AbstractDungeon.getCurrRoom().isBattleOver) {
                AbstractDungeon.overlayMenu.proceedButton.hide();
                AbstractDungeon.overlayMenu.showCombatPanels();
                AbstractDungeon.previousScreen = null;
            } else {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            }
        } else if (EventRoom.class.isAssignableFrom(AbstractDungeon.getCurrRoom().getClass())) {
            AbstractDungeon.previousScreen = null;
        } else if (AbstractDungeon.getCurrRoom() instanceof TreasureRoom) {
            if(((TreasureRoom) AbstractDungeon.getCurrRoom()).chest.isOpen) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            } else {
                AbstractDungeon.previousScreen = null;
            }
        } else if (AbstractDungeon.getCurrRoom() instanceof TreasureRoomBoss) {
            AbstractDungeon.previousScreen = null;
        } else if(previousScreen != null && previousScreen != AbstractDungeon.CurrentScreen.NONE) {
                AbstractDungeon.previousScreen = previousScreen;
                previousScreen = null;
        } else {
            AbstractDungeon.previousScreen = null;
        }
    }


    public void open() {
        APScreen = true;
        rewardsQueued = 0;
        AbstractDungeon.player.releaseCard();

        logger.info("current map location y: " + AbstractDungeon.getCurrMapNode().y);
        if (AbstractDungeon.getCurrMapNode().y == -1) {
            AbstractDungeon.nextRoom = null; // this is necessary to make the first nodes available in new act (dunno how else to force it)
        }

        rewardAnimTimer = 0.5F;
        CardCrawlGame.sound.play("UI_CLICK_1");
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.showBlackScreen();
        AbstractDungeon.dynamicBanner.appear(TEXT[1]);
        AbstractDungeon.overlayMenu.proceedButton.hide();
        AbstractDungeon.overlayMenu.hideCombatPanels();

        previousScreen = AbstractDungeon.screen;
        AbstractDungeon.screen = Enum.ARCHIPELAGO_REWARD_SCREEN;
        tip = CardCrawlGame.tips.getTip();

        ArrayList<NetworkItem> items = APClient.apClient.getItemManager().getReceivedItems();
        for (int i = index; i < items.size(); ++i) {
            addReward(items.get(i));
        }
    }

    public void update() {
        if (InputHelper.justClickedLeft && Settings.isDebug) {
            tip = CardCrawlGame.tips.getTip();
        }

        rewardViewUpdate();
        updateEffects();
    }

    private void updateEffects() {
        Iterator<AbstractGameEffect> effectsIterator = effects.iterator();

        while (effectsIterator.hasNext()) {
            AbstractGameEffect effect = effectsIterator.next();
            effect.update();
            if (effect.isDone) {
                effectsIterator.remove();
            }
        }
    }

    private void rewardViewUpdate() {
        if (rewardAnimTimer != 0.0F) {
            rewardAnimTimer -= Gdx.graphics.getDeltaTime();
            if (rewardAnimTimer < 0.0F) {
                rewardAnimTimer = 0.0F;
            }

            uiColor.r = 1.0F - rewardAnimTimer / 0.2F;
            uiColor.g = 1.0F - rewardAnimTimer / 0.2F;
            uiColor.b = 1.0F - rewardAnimTimer / 0.2F;
        }

        tipY = MathHelper.uiLerpSnap(tipY, (float) Settings.HEIGHT / 2.0F - 460.0F * Settings.scale);
        updateControllerInput();
        boolean removedSomething = false;
        Iterator<RewardItem> rewardItemIterator = rewards.iterator();

        while (rewardItemIterator.hasNext()) {
            RewardItem rewardItem = rewardItemIterator.next();
            if (doUpdate(rewardItem)) {
                rewardItem.update();
            }
            if (rewardItem.isDone) {
                if (rewardItem.claimReward()) {
                    rewardItemIterator.remove();
                    removedSomething = true;
                } else {
                    rewardItem.isDone = false;
                }
            }
        }

        if (removedSomething) {
            positionRewards();
        }

        if (rewards.size() < 6) {
            scrollTarget = 0.0F;
            scrollPosition = 0.0F;
            positionRewards();
        } else if (!scrollBar.update()) {
            int y = InputHelper.mY;
            if (!grabbedScreen) {
                if (InputHelper.scrolledDown) {
                    scrollTarget = scrollTarget + Settings.SCROLL_SPEED;
                } else if (InputHelper.scrolledUp) {
                    scrollTarget = scrollTarget - Settings.SCROLL_SPEED;
                }

                if (InputHelper.justClickedLeft) {
                    grabbedScreen = true;
                    grabStartY = (float) y - scrollTarget;
                }
            } else if (InputHelper.isMouseDown) {
                scrollTarget = (float) y - grabStartY;
            } else {
                grabbedScreen = false;
            }

            float prev_scrollPosition = scrollTarget;
            scrollPosition = MathHelper.scrollSnapLerpSpeed(scrollPosition, scrollTarget);
            if (scrollTarget < 0.0F) {
                scrollTarget = 0.0F;
            }

            scrollUpperBound = (float) (rewards.size() - 5) * 100.0F * Settings.scale;
            if (scrollTarget > scrollUpperBound) {
                scrollTarget = scrollUpperBound;
            }

            if (scrollPosition != prev_scrollPosition) {
                positionRewards();
            }

            updateBarPosition();
        }
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(scrollLowerBound, scrollUpperBound, scrollPosition);
        scrollBar.parentScrolledToPercent(percent);
    }

    public boolean doUpdate(RewardItem reward) {
        boolean ret = false;// 231
        float upperBounds = (float) Settings.HEIGHT / 2.0F + 204.0F * Settings.scale;// 232
        float lowerBounds = (float) Settings.HEIGHT / 2.0F + -336.0F * Settings.scale;// 233
        if (reward.y < upperBounds && reward.y > lowerBounds) {// 234
            ret = true;// 235
        }

        if (!ret) {// 238
            reward.hb.hovered = false;// 240
            reward.hb.justHovered = false;// 241
            reward.hb.clicked = false;// 242
            reward.hb.clickStarted = false;// 243
            if (reward.flashTimer > 0.0F) {// 246
                reward.flashTimer -= Gdx.graphics.getDeltaTime();// 247
                if (reward.flashTimer < 0.0F) {// 248
                    reward.flashTimer = 0.0F;// 249
                }
            }
        }

        try {
            Field f = RewardItem.class.getDeclaredField("effects");// 255
            f.setAccessible(true);// 256
            ArrayList<AbstractGameEffect> effects = (ArrayList) f.get(reward);// 257
            Iterator it;
            AbstractGameEffect e;
            if (!ret) {// 259
                if (effects.size() == 0) {// 261
                    effects.add(new RewardGlowEffect(reward.hb.cX, reward.hb.cY));// 262
                }

                it = effects.iterator();// 264

                while (it.hasNext()) {
                    e = (AbstractGameEffect) it.next();// 265
                    e.update();// 266
                    if (e.isDone) {// 267
                        it.remove();// 268
                    }
                }
            }

            it = effects.iterator();

            while (it.hasNext()) {
                e = (AbstractGameEffect) it.next();// 272
                if (e instanceof RewardGlowEffect) {// 273
                    moveRewardGlowEffect((RewardGlowEffect) e, reward.hb.cX, reward.hb.cY);// 274
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException var8) {// 277
            var8.printStackTrace();// 278
        }

        return ret;// 281
    }

    private void moveRewardGlowEffect(RewardGlowEffect effect, float x, float y) {
        try {
            Field f = RewardGlowEffect.class.getDeclaredField("x");// 287
            f.setAccessible(true);// 288
            f.setFloat(effect, x);// 289
            f = RewardGlowEffect.class.getDeclaredField("y");// 291
            f.setAccessible(true);// 292
            f.setFloat(effect, y);// 293
        } catch (NoSuchFieldException | IllegalAccessException var4) {// 294
            var4.printStackTrace();// 295
        }

    }// 297

    public void positionRewards() {
        float baseY = (float) Settings.HEIGHT / 2.0F + 124.0F * Settings.scale;// 137
        float spacingY = 100.0F * Settings.scale;// 138

        for (int i = 0; i < rewards.size(); ++i) {// 140
            rewards.get(i).move(baseY - (float) i * spacingY + scrollPosition);// 141
        }

        if (rewards.isEmpty()) {// 143
            hasTakenAll = true;// 144
        }
    }

    public void addReward(RewardItem item) {
        ++index;
        rewards.add(item);
        positionRewards();
    }

    public void addReward(NetworkItem networkItem) {
        long itemID = networkItem.itemID;
        String location = networkItem.locationName;
        String player = networkItem.playerName;
        if (itemID == 8000L) { //card draw
            apReward = true;
            ArrayList<AbstractCard> cards = AbstractDungeon.getRewardCards();
            apReward = false;
            RewardItem reward = new RewardItem(1);
            reward.goldAmt = 0;
            reward.type = RewardItem.RewardType.CARD;
            reward.cards = cards;
            RewardItemPatch.CustomFields.apReward.set(reward, true);
            reward.text = player + " NL " + location;
            addReward(reward);
        } else if (itemID == 8001L) { //rare card draw
            apRareReward = true;
            ArrayList<AbstractCard> rareCards = AbstractDungeon.getRewardCards();
            apRareReward = false;
            RewardItem reward = new RewardItem(1);
            reward.goldAmt = 0;
            reward.type = RewardItem.RewardType.CARD;
            reward.cards = rareCards;
            RewardItemPatch.CustomFields.apReward.set(reward, true);
            try {
                Field f = RewardItem.class.getDeclaredField("isBoss");
                f.setAccessible(true);
                f.set(reward, true);
            } catch (Exception ignored) {
            }

            reward.text = player + " NL " + location;
            addReward(reward);
        } else if (itemID == 8002L) { // Relic
            AbstractRelic relic = AbstractDungeon.returnRandomRelic(getRandomRelicTier());
            RewardItem reward = new RewardItem(relic);
            reward.text = player + " NL " + location;
            RewardItemPatch.CustomFields.apReward.set(reward, true);
            addReward(reward);
        } else if (itemID == 8003L) { // Boss Relic
            ArrayList<AbstractRelic> bossRelics = new ArrayList<AbstractRelic>() {{
                add(AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
                add(AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
                add(AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
            }};
            RewardItem reward = new RewardItem(1);
            reward.goldAmt = 0;
            reward.type = RewardItemPatch.RewardType.BOSS_RELIC;
            RewardItemPatch.CustomFields.bossRelics.set(reward, bossRelics);
            RewardItemPatch.CustomFields.apReward.set(reward, true);
            reward.text = player + " NL " + location;
            addReward(reward);
            //ArchipelagoMW.bossRelicRewardScreen.open(bossRelics);
        }
    }

    public AbstractCard.CardRarity rollRarity(Random rng) {
        int roll = AbstractDungeon.cardRng.random(99);
        final int rareRate = 3;
        if (roll < rareRate) {
            return AbstractCard.CardRarity.RARE;
        }
        if (roll < 40) {
            return AbstractCard.CardRarity.UNCOMMON;
        }
        return AbstractCard.CardRarity.COMMON;
    }

    private AbstractRelic.RelicTier getRandomRelicTier() {
        int roll = AbstractDungeon.relicRng.random(0, 99);
        if (ModHelper.isModEnabled("Elite Swarm")) {
            roll += 10;
        }
        if (roll < 50) {
            return AbstractRelic.RelicTier.COMMON;
        }
        if (roll > 82) {
            return AbstractRelic.RelicTier.RARE;
        }
        return AbstractRelic.RelicTier.UNCOMMON;
    }

    private void updateControllerInput() {
        if (Settings.isControllerMode && !rewards.isEmpty() && !AbstractDungeon.topPanel.selectPotionMode && AbstractDungeon.topPanel.potionUi.isHidden && !AbstractDungeon.player.viewingRelics) {// 161
            int index = 0;
            boolean anyHovered = false;

            for (Iterator<RewardItem> rewardItemIterator = rewards.iterator(); rewardItemIterator.hasNext(); ++index) {
                RewardItem rewardItem = rewardItemIterator.next();
                if (rewardItem.hb.hovered) {
                    anyHovered = true;
                    break;
                }
            }

            if (!anyHovered) {
                index = 0;
                Gdx.input.setCursorPosition((int) (rewards.get(index)).hb.cX, Settings.HEIGHT - (int) (rewards.get(index)).hb.cY);
            } else if (!CInputActionSet.up.isJustPressed() && !CInputActionSet.altUp.isJustPressed()) {
                if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                    ++index;
                    if (index > rewards.size() - 1) {
                        index = 0;
                    }

                    Gdx.input.setCursorPosition((int) (rewards.get(index)).hb.cX, Settings.HEIGHT - (int) (rewards.get(index)).hb.cY);
                }
            } else {
                --index;
                if (index < 0) {
                    index = rewards.size() - 1;
                }

                Gdx.input.setCursorPosition((int) (rewards.get(index)).hb.cX, Settings.HEIGHT - (int) (rewards.get(index)).hb.cY);
            }

        }
    }

    public void render(SpriteBatch sb) {
        renderItemReward(sb);
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, tip, (float) Settings.WIDTH / 2.0F, tipY, Color.LIGHT_GRAY);

        for (AbstractGameEffect effect : effects) {
            effect.render(sb);
        }
    }

    @Override
    public void openingSettings() {
        // Save old screen as previous screen, and put ours in there.
        AbstractDungeon.dynamicBanner.hide();
        previousScreen = AbstractDungeon.previousScreen;
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void openingDeck() {
        AbstractDungeon.dynamicBanner.hide();
        // Save old screen as previous screen, and put ours in there.
        previousScreen = AbstractDungeon.previousScreen;
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void openingMap() {
        AbstractDungeon.dynamicBanner.hide();
        // Save old screen as previous screen, and put ours in there.
        previousScreen = AbstractDungeon.previousScreen;
        AbstractDungeon.previousScreen = curScreen();
    }

    public boolean allowOpenDeck() {
        return true;
    }

    public boolean allowOpenMap() {
        return true;
    }

    private void renderItemReward(SpriteBatch sb) {
        sb.setColor(uiColor);
        sb.draw(ImageMaster.REWARD_SCREEN_SHEET, (float) Settings.WIDTH / 2.0F - 306.0F, (float) Settings.HEIGHT / 2.0F - 46.0F * Settings.scale - 358.0F, 306.0F, 358.0F, 612.0F, 716.0F, Settings.xScale, Settings.scale, 0.0F, 0, 0, 612, 716, false, false);
        if (camera == null) {// 88
            try {
                Field f = CardCrawlGame.class.getDeclaredField("camera");// 90
                f.setAccessible(true);// 91
                camera = (OrthographicCamera) f.get(Gdx.app.getApplicationListener());// 92
            } catch (IllegalAccessException | NoSuchFieldException var4) {// 93
                var4.printStackTrace();// 94
                return;// 95
            }
        }

        sb.flush();// 99
        Rectangle scissors = new Rectangle();// 100
        Rectangle clipBounds = new Rectangle((float) Settings.WIDTH / 2.0F - 300.0F * Settings.scale, (float) Settings.HEIGHT / 2.0F - 350.0F * Settings.scale, 600.0F * Settings.scale, 600.0F * Settings.scale);// 101
        ScissorStack.calculateScissors(camera, sb.getTransformMatrix(), clipBounds, scissors);// 103
        ScissorStack.pushScissors(scissors);
        for (RewardItem reward : rewards) {
            reward.render(sb);
        }
        if (camera != null) {// 109
            sb.flush();// 110
            ScissorStack.popScissors();// 111
        }

        if (rewards.size() > 5) {// 114
            scrollBar.render(sb);// 115
        }
    }

    private class ScrollListener implements ScrollBarListener {
        private ScrollListener() {
        }// 32

        public void scrolledUsingBar(float v) {
            scrollPosition = MathHelper.valueFromPercentBetween(scrollLowerBound, scrollUpperBound, v);// 37
            scrollTarget = scrollPosition;// 38
            positionRewards();// 39
            updateBarPosition();// 40
        }// 41
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString(ArchipelagoMW.getModID() + ":RewardMenu");
        TEXT = uiStrings.TEXT;
    }
}
