package ArchipelagoMW;

import com.megacrit.cardcrawl.rewards.RewardItem;
import gg.archipelago.APClient.parts.NetworkItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class LocationTracker {
    public static final Logger logger = LogManager.getLogger(LocationTracker.class.getName());

    private static ArrayList<Long> cardDrawLocations;

    private static ArrayList<Long> relicLocations;

    private static ArrayList<Long> rareCardLocations;

    private static ArrayList<Long> bossRelicLocations;
    private static int cardDrawLocationCounter = 0;
    private static int cardDrawLocationCheckPointAct2 = 5;
    private static int cardDrawLocationCheckPointAct3 = 10;
    private static int relicLocationCounter = 0;
    private static int relicLocationCheckPointAct2 = 3;
    private static int relicLocationCheckPointAct3 = 6;
    public static Set<Long> checkedLocations;

    public static boolean cardDraw;

    public static HashMap<Long, NetworkItem> scoutedLocations = new HashMap<>();

    public static portalType chosenPortalType = portalType.portalsWithCheckpoints;
    public enum portalType{
        noPortals, portalsWithCheckpoints
    }

    public static void reset() {
        cardDrawLocations = new ArrayList<Long>() {{
            add(19001L);
            add(19002L);
            add(19003L);
            add(19004L);
            add(19005L);
            add(19006L);
            add(19007L);
            add(19008L);
            add(19009L);
            add(19010L);
            add(19011L);
            add(19012L);
            add(19013L);
            add(19014L);
            add(19015L);
        }};

        relicLocations = new ArrayList<Long>() {{
            add(20001L);
            add(20002L);
            add(20003L);
            add(20004L);
            add(20005L);
            add(20006L);
            add(20007L);
            add(20008L);
            add(20009L);
            add(20010L);
        }};

        rareCardLocations = new ArrayList<Long>() {{
            add(21001L);
            add(21002L);
            add(21003L);
        }};

        bossRelicLocations = new ArrayList<Long>() {{
            add(22001L);
            add(22002L);
            add(22003L);
        }};
        cardDrawLocationCounter = 0;
        relicLocationCounter = 0;
        cardDraw = true;

    }
    public static void SetLocationCounters(int incomingAct){
        cardDrawLocationCounter = 0;
        int cardDrawLocationCheckpoint = 0;
        int relicLocationCheckpoint = 0;
        switch(incomingAct){
            case 2:
                cardDrawLocationCheckpoint = cardDrawLocationCheckPointAct2;
                relicLocationCheckpoint = relicLocationCheckPointAct2;
                break;
            case 3:
                cardDrawLocationCheckpoint = cardDrawLocationCheckPointAct3;
                relicLocationCheckpoint = relicLocationCheckPointAct3;
                break;
            default:
                break;
        }
        for (Long location: cardDrawLocations) {
            if(checkedLocations.contains(location) && cardDrawLocationCounter < cardDrawLocationCheckpoint){
                cardDrawLocationCounter += 1;
            }
            else{
                break;
            }
        }
        for (Long location: relicLocations) {
            if(checkedLocations.contains(location) && relicLocationCounter < relicLocationCheckpoint){
                relicLocationCounter += 1;
            }
            else{
                break;
            }
        }

    }

    /**
     * @return true if this card draw was sent to AP,
     * false if you should keep this card draw locally.
     */
    public static String sendCardDraw(RewardItem reward, int act) {
        try {
            Field isBoss = RewardItem.class.getDeclaredField("isBoss");
            isBoss.setAccessible(true);
            if ((boolean) isBoss.get(reward)) {
                return (sendRareCardDraw(reward, act));
            }

        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        cardDraw = !cardDraw;
        if (cardDraw) {
            long locationID;
            if(cardDrawLocationCounter>=cardDrawLocations.size()) {return "";}
            try {
                locationID = cardDrawLocations.get(cardDrawLocationCounter);
            }
            catch(IndexOutOfBoundsException e) {
                logger.info("Index out of bounds! Tried to access cardLocation position " + cardDrawLocationCounter);
                logger.info("while the length is " + cardDrawLocations.size());
                return "";
            }
            APClient.apClient.checkLocation(locationID);
            NetworkItem item = scoutedLocations.get(locationID);
            cardDrawLocationCounter += 1;
            logger.info("Sending card draw location " + cardDrawLocationCounter);
            if (item == null)
                return "Card Draw " + cardDrawLocationCounter;
            return item.itemName + " [] NL " + item.playerName + " [] NL Card Draw " + cardDrawLocationCounter;
        }
        return "";
    }
    public static String sendRareCardDraw(RewardItem reward, int act) {
        long locationID;
        try {
            locationID = rareCardLocations.get(act - 1);
        }
        catch(IndexOutOfBoundsException e) {
            logger.info("Index out of bounds! Tried to access rareCardLocation position " + (act - 1));
            logger.info("while the length is " + rareCardLocations.size());
            return "";
        }
        APClient.apClient.checkLocation(locationID);
        NetworkItem item = scoutedLocations.get(locationID);
        logger.info("Sending rare card draw location " + (act - 1));
        if (item == null)
            return "Rare Card Draw " + act;
        return item.itemName + " [] NL " + item.playerName + " [] NL Rare Card Draw " + (act - 1);
    }

    /**
     * sends the next relic location to AP
     */
    public static String sendRelic(int act) {
        long locationID;
        if(relicLocationCounter>=relicLocations.size()) {return "";}
        try {
            locationID = relicLocations.get(relicLocationCounter);
        }
        catch(IndexOutOfBoundsException e) {
            logger.info("Index out of bounds! Tried to access relicLocation position " + relicLocationCounter);
            logger.info("while the length is " + relicLocations.size());
            return "";
        }
        APClient.apClient.checkLocation(locationID);
        NetworkItem item = scoutedLocations.get(locationID);
        relicLocationCounter += 1;
        logger.info("Sending relic location " + relicLocationCounter);
        if (item == null)
            return "Relic " + relicLocationCounter;
        return item.itemName + " [] NL " + item.playerName + " [] NL Relic " + relicLocationCounter;
    }

    /**
     * sends the next boss relic location to AP
     */
    public static String sendBossRelic(int act) {
        logger.info("Going to send relic from act " + act);
        long locationID;
        try {
            locationID = bossRelicLocations.get(act - 1);
        } catch (IndexOutOfBoundsException e) {
            logger.info("Index out of bounds! Tried to access bossRelicLocation position " + (act - 1));
            logger.info("while the length is " + bossRelicLocations.size());
            return "";
        }
        APClient.apClient.checkLocation(locationID);
        NetworkItem item = scoutedLocations.get(locationID);
        logger.info("Sending Boss Relic location " + (act - 1));
        if (item == null)
            return "Boss Relic " + act;
        return item.itemName + " [] NL " + item.playerName + " [] NL Boss Relic " + (act - 1);
    }

    public static void forfeit() {
        APClient ap = APClient.apClient;
        for (long location : cardDrawLocations) {
            ap.checkLocation(location);
        }
        for (long location : rareCardLocations) {
            ap.checkLocation(location);
        }
        for (long location : relicLocations) {
            ap.checkLocation(location);
        }
        for (long location : bossRelicLocations) {
            ap.checkLocation(location);
        }
    }

    public static void scoutAllLocations() {
        ArrayList<Long> locations = new ArrayList<Long>() {{
            addAll(cardDrawLocations);
            addAll(relicLocations);
            addAll(rareCardLocations);
            addAll(bossRelicLocations);
        }};
        APClient.apClient.scoutLocations(locations);
    }

    public static void addToScoutedLocations(ArrayList<NetworkItem> networkItems) {
        for (NetworkItem item : networkItems) {
            scoutedLocations.put(item.locationID, item);
        }

    }
}
