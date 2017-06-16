package ogame.pages;

import bot.Bot;
import bot.settings.SettingsManager;
import com.google.gson.Gson;
import ogame.objects.game.messages.CombatMessage;
import ogame.objects.game.messages.EspionageMessage;
import ogame.objects.game.messages.MessageObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import utilities.data.HttpsClient;
import utilities.database.Database;
import utilities.fileio.FileOptions;
import utilities.webdriver.DriverController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jarndt on 5/30/17.
 */
public class Messages implements OgamePage {
    public static final String MESSAGES = "Messages";

    @Override
    public String getPageName() {
        return MESSAGES;
    }

    @Override
    public String getXPathSelector() {
        return "//*[@id='message-wrapper']/a[1]";
    }

    @Override
    public String getCssSelector() {
        return "#message-wrapper > a.selected.comm_menu.messages.tooltip.js_hideTipOnMobile";
    }

    @Override
    public String uniqueCssSelector() {
        return "#ui-id-1 > div";
    }

    @Override
    public boolean isPageLoaded(DriverController driverController) {
        String style = Jsoup.parse(driverController.getDriver().getPageSource()).select("div.ajax_load_shadow").attr("style");
        return style.contains("display:") && style.contains("none");
    }

    @Override
    public boolean waitForPageToLoad(DriverController driverController, TimeUnit timeUnit, long l) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        boolean b = true;
        try {
            exec.submit(new Callable<Boolean>(){
                @Override public Boolean call() throws Exception {
                    return _waitForText(driverController);
                }
            }).get(l, timeUnit);
            exec.shutdown();
            exec.awaitTermination(l, timeUnit);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            b = false;
        }finally{
            exec.shutdownNow();
        }
        return b;
    }private boolean _waitForText(DriverController driverController){
        while(!isPageLoaded(driverController))
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) { /* DO NOTHING */ }
        return true;
    }

    @Override
    public void parsePage(Bot b, Document document) {
        //TODO message parsing
        Set<Integer> v = document.select("li").stream().filter(a -> {
            Elements n = a.select("span.new_msg_count");
            if (n.size() == 0)
                return false;
            if (n.get(0).text().isEmpty())
                return false;
            if (Integer.parseInt(n.get(0).text()) < 1)
                return false;
            return true;
        }).map(a->Integer.parseInt(a.attr("data-tabid"))).collect(Collectors.toSet());
        v.addAll(Arrays.asList(20,21));
        parseAllMessages(b,new ArrayList<>(v));
    }

    public void deleteMessage(String messageID){
        String messageAction = "103";

    }

    public void parseAllMessages(Bot b){
        parseAllMessages(b,Arrays.asList(2,1,3,5,4,6,20,21,22,23,24,25,10,14,12,11,13));
    }

    public void parseAllMessages(Bot b, List<Integer> tabIds){
        //2 fleets, 1 communication, 3 economy, 5 universe, 4 ogame, 6 favorites
        //20 espionage, 21 combat reports, 22 expeditions, 23 unions/transport, 24 other, 25 trash
        //10 messages, 14 information, 12 shared combat reports , 11 shared espoinage reports, 13 expeditions
        //
        String domain = b.getLogin().getServer().getDomain();
        String tabReplace = "[TAB]", pageReplace = "[PAGE]";
        String host = "https://"+domain+"/game/index.php?page=messages&tab="+tabReplace+"&pagination="+pageReplace+"&ajax=1";

        Set<Cookie> cookies = b.getDriverController().getDriver().manage().getCookies();
//        List<String> requiredCookies = new ArrayList<>(Arrays.asList("maximizeId","_ga","__auc","PHPSESSID","pc_idt","prsess_116678","login_116678"));
        StringBuilder builder = new StringBuilder("");
//        int i = 0;
//            String sep = i++<cookies.size()-1?"; ":"";
        for(Cookie c : cookies)
            builder.append(c.getName() + "=" + c.getValue() + "; ");
//      NO CONCURRENCY to avoid getting logged off
//        ExecutorService executor = FileOptions.runConcurrentProcessNonBlocking(tabIds.stream().map(a -> (Callable) () -> {
        for(int a : tabIds)
            try {
                Document d = Jsoup.parse(new HttpsClient().getMessages(b.getServerDomain(),a,b.getCookies()));
//                Document d = Jsoup.parse(new HttpsClient().getMessages(host.replace(tabReplace, a + "").replace(pageReplace, 1 + ""), builder.toString()));
                int totalPages = 1;
                try{
                    totalPages = Integer.parseInt(d.select("#defaultmessagespage > div > ul > ul:nth-child(1) > li.curPage").text().split("\\/")[1]);
                }catch (Exception e){/*DO NOTHING*/}
                if (totalPages > 1)
                    FileOptions.runConcurrentProcessNonBlocking(IntStream.range(0, totalPages).boxed().map(aa -> (Callable) () -> {
                        try {
                            Document dd = Jsoup.parse(
                                    new HttpsClient().getMessages(
                                            host.replace(tabReplace, a + "")
                                                    .replace(pageReplace, aa + "")
                                                    .replace(tabReplace, a + ""),
                                            builder.toString()
                                    )
                            );
                            dd.select("li.msg").stream().forEach(c -> b.getMessages().add(new MessageObject(c).setTabId(a).setPageNumber(aa)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).collect(Collectors.toList()));
                else
                    d.select("li.msg").stream().forEach(c -> b.getMessages().add(new MessageObject(c).setTabId(a).setPageNumber(1)));
            } catch (IOException e) {
                e.printStackTrace();
            }
//            return null;
//        }).collect(Collectors.toList()));
        FileOptions.runConcurrentProcessNonBlocking((Callable) ()->{
//            while (!executor.isTerminated() || !executor.isShutdown())
//                Thread.sleep(100);
            StringBuilder builder1 = new StringBuilder("");
            for(MessageObject m : b.getMessages())
                builder1.append("insert into messages(ogame_user_id,message_id,tab_id,message_status,message_title,message_date,message_from,message_content)" +
                        "   values("+b.getOgameUserId()+","+m.getMessageId()+","+m.getTabId()+",'"+m.getMessageStatus()+"','" +
                        m.getMessageTitle()+"','"+m.getMessageTimestamp()+"'::timestamp,'"+m.getFrom()+"','"+m.getMessageContent()+"') ON CONFLICT DO NOTHING; "
                );

            try {
                if(!builder1.toString().isEmpty())
                    Database.getExistingDatabaseConnection().executeQuery(builder1.toString());
            }catch (Exception e){}

            List<EspionageMessage> espionageMessages = b.getMessages().stream().map(a -> {
                try {
                    return a.getEspionageMessage(b.getServerDomain(), b.getCookies());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(a -> a != null).collect(Collectors.toList());

            String insert = "insert into espionage_messages(server_id,messages_id,loot,counter_esp_percent,small_cargo_needed,largo_cargo_needed," +
                    "loot_percent,message_date,planet_name,player_name,status,activity,api,coordinates,is_honorable,metal,crystal,deuterium," +
                    "solar,json_levels,json_active_repair) values(";
            builder1 = new StringBuilder("");
            for(EspionageMessage m : espionageMessages)
                builder1.append( b.getLogin().getServer().getServerID()+","+
                        insert+m.getMessageId()+","+m.getLoot()+","+m.getCounterEspionagePercent()+","+m.getSmallCargosNeeded()+","+
                        m.getLargeCargosNeeded()+","+m.getLootPercent()+",'"+ Timestamp.valueOf(m.getMessageDate())+"'::timestamp,'"+
                        m.getPlanetName()+"','"+m.getPlayerName()+"','"+m.getStatus()+"','"+m.getActivity()+"','"+m.getApi()+"','"+
                        m.getCoordinates().getStringValue()+"',"+(m.isHonorable()+"").toUpperCase()+","+m.getResources().getMetal()+","+
                        m.getResources().getCrystal()+","+m.getResources().getDeuterium()+","+m.getResources().getEnergy()+",'"+
                        new Gson().toJson(m.getLevels())+"','"+new Gson().toJson(m.getActiveRepair())+"') ON CONFLICT DO NOTHING;"
                );
            if(!builder1.toString().isEmpty())
                Database.getExistingDatabaseConnection().executeQuery(builder1.toString());

            //TODO Combat reports
            List<CombatMessage> combatMessages = b.getMessages().stream().map(a -> {
                try {
                    return a.getCombatMessage(b.getServerDomain(), b.getCookies());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(a -> a != null).collect(Collectors.toList());

            insert = "insert into COMBAT_MESSAGES(" +
                    "server_id,message_id,message_date," +
                    "attacker_gains,defender_gains,debris_size," +
                    "actually_repaired,attacker_honor,defender_honor," +
                    "recycler_count,moon_change_percent,attacker_weapons,attacker_shields," +
                    "attacker_armour,defender_weapons,defender_shields,defender_armour," +
                    "loot_metal,loot_crystal,loot_deueterium,debris_metal," +
                    "debris_cyrstal,attacker_name,defender_name,api,attacker_status," +
                    "defender_status,attacker_planet_coords,defender_planet_coords," +
                    "json_attacker_ships,json_attacker_ships_lost,json_defender_ships," +
                    "json_defender_ships_lost) values(";
            builder1 = new StringBuilder("");
            for(CombatMessage m : combatMessages)
                builder1.append(
                        b.getLogin().getServer().getServerID()+","+m.getMessageId()+",'"+Timestamp.valueOf(m.getMessageDate())+"'::timestamp,"+
                                m.getAttackerGainsOrLosses()+","+m.getDefenderGainsOrLosses()+","+m.getDebrisFieldSize()+","+
                                m.getActuallyRepaired()+","+m.getAttackerHonorPointsGainOrLoss()+","+m.getDefenderHonorPointsGainOrLoss()+","+
                                m.getRecyclerCount()+","+m.getMoonChancePercent()+","+m.getAttackerWeapons()+","+m.getAttackerShields()+","+
                                m.getAttackerArmour()+","+m.getDefenderWeapons()+","+m.getDefenderShields()+","+m.getDefenderArmour()+","+
                                m.getLoot().getMetal()+","+m.getLoot().getCrystal()+","+m.getLoot().getDeuterium()+","+m.getDebrisField().getMetal()+","+
                                m.getDebrisField().getCrystal()+",'"+m.getAttackerName()+"','"+m.getDefenderName()+"','"+m.getApi()+"','"+m.getAttackerStatus()+"','"+
                                m.getDefenderStatus()+"','"+m.getAttackerCoordinates().getStringValue()+"','"+m.getDefenderCoordinates().getStringValue()+"','"+
                                new Gson().toJson(m.getAttackerShips())+"','"+new Gson().toJson(m.getAttackerShipsLost())+"','" +
                                new Gson().toJson(m.getDefenderShipsDefence())+"','"+new Gson().toJson(m.getDefenderShipsLost())+"'" +
                                ") ON CONFLICT DO NOTHING; "
                );
            if(!builder1.toString().isEmpty())
                Database.getExistingDatabaseConnection().executeQuery(builder1.toString());

            if(b.getCurrentPlanet().getSetting(SettingsManager.DELETE_MESSAGES,b.getOgameUserId()).equalsIgnoreCase("true"))
                b.getMessages().forEach(a -> { //added to the database, may safely delete messages
                    try {
                        new HttpsClient().deleteMessage(b.getServerDomain(), a.getMessageId() + "", b.getCookies());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
            else {
//              the following is to mark the messages as read instead
                String newMessages = "";
                for (MessageObject m : b.getMessages())
                    if (m.isNewMessage())
                        newMessages += m.getMessageId() + ",";
                newMessages = newMessages.substring(0, newMessages.length() - ",".length());

                new HttpsClient().markMessagesAsRead(b.getLogin().getServer().getDomain(), newMessages, builder.toString());
            }
            return null;
        });
    }
}
