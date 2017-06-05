package ogame.pages;

import bot.Bot;
import ogame.objects.game.Buildable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by jarndt on 5/8/17.
 */
public class Research implements OgamePage{
    public static final String RESEARCH = "Research";

    public static final String ID = "id";

    public static final String
            ENERGY              = "Energy Technology",
            LASER               = "Laser Technology",
            ION                 = "Ion Technology",
            HYPERSPACE_TECH     = "Hyperspace Technology",
            PLASMA              = "Plasma Technology",
            COMBUSTION          = "Combustion Drive",
            IMPULSE             = "Impulse Drive",
            HYPERSPACE_DRIVE    = "Hyperspace Drive",
            ESPIONAGE           = "Espionage Technology",
            COMPUTER            = "Computer Technology",
            ASTROPHYSICS        = "Astrophysics",
            INTERGALACTIC       = "Intergalactic Research Network",
            GRAVITON            = "Graviton Technology",
            WEAPONS             = "Weapons Technology",
            SHIELDING           = "Shielding Technology",
            ARMOUR              = "Armour Technology";


    public static final String[] names = {
            ENERGY, LASER, ION, HYPERSPACE_TECH, PLASMA, COMBUSTION, IMPULSE,
            HYPERSPACE_DRIVE, ESPIONAGE, COMPUTER, ASTROPHYSICS, INTERGALACTIC,
            GRAVITON, WEAPONS, SHIELDING, ARMOUR
    };

    @Override
    public String getPageName() {
        return RESEARCH;
    }
    @Override
    public String getXPathSelector() {
        return "//*[@id='menuTable']/li[5]/a/span";
    }

    @Override
    public String getCssSelector() {
        return "#menuTable > li:nth-child(5) > a > span";
    }

    @Override
    public String uniqueXPath() {
        return "//*[@id='wrapBattle']/h2";
    }

    @Override
    public void parsePage(Bot b, Document document) {
        Elements v = document.select("#buttonz").select("div.buildingimg");
        for (Element e : v) {
            String name = e.select("span.textlabel").text().trim();
            Integer level = Integer.parseInt(e.select("span.level").get(0).ownText().trim());
            Buildable bb = Buildable.getBuildableByName(name).setCurrentLevel(level);
            bb.setCssSelector(e.cssSelector());
            b.addResearch(bb);
        }
        //TODO Currently researching research
    }

}
