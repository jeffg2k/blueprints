package com.tinkerpop.blueprints.pgm.util.io.net;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A reader for the Pajek Graph Format (net).
 * <p/>
 * NET definition taken from
 * (https://gephi.org/users/supported-graph-formats/pajek-net-format/)
 * <p/>
 *
 * @author Jeff Gentes
 */
public class NETTokens {
    public static final String NET = "net";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String LABEL = "label";
    public static final String WEIGHT = "weight";
    public static final String NEXT = "*";
    public static final String EDGES = "*edges";
    public static final String NODES = "*vertices";
    public static final String ARCS = "*arcs";
    public static final String SIZE = "size";
    public static final String X = "x_fact";
    public static final String Y = "y_fact";
    public static final String ICCOLOR = "ic";
    public static final String BCCOLOR = "bc";
    public static final String ECOLOR = "c";

    private static final HashMap<String, Color> colorTable = new HashMap<String, Color>();

    static {
        colorTable.put("Apricot", new Color(0xFDD9B5));
        colorTable.put("Aquamarine", new Color(0x78DBE2));
        colorTable.put("Bittersweet", new Color(0xFD7C6E));
        colorTable.put("Black", new Color(0x232323));
        colorTable.put("Blue", new Color(0x1F75FE));
        colorTable.put("BlueGreen", new Color(0x199EBD));
        colorTable.put("BlueViolet", new Color(0x7366BD));
        colorTable.put("BrickRed", new Color(0xCB4154));
        colorTable.put("Brown", new Color(0xB4674D));
        colorTable.put("BurntOrange", new Color(0xFF7F49));
        colorTable.put("CadetBlue", new Color(0xB0B7C6));
        colorTable.put("Canary", new Color(0xFFFF99));
        colorTable.put("CarnationPink", new Color(0xFFAACC));
        colorTable.put("Cerulean", new Color(0x1DACD6));
        colorTable.put("CornflowerBlue", new Color(0x9ACEEB));
        colorTable.put("Cyan", new Color(0x00FFFF));
        colorTable.put("Dandelion", new Color(0xFDDB6D));
        colorTable.put("DarkOrchid", new Color(0xFDDB7D));
        colorTable.put("Emerald", new Color(0x50C878));
        colorTable.put("ForestGreen", new Color(0x6DAE81));
        colorTable.put("Fuchsia", new Color(0xC364C5));
        colorTable.put("Goldenrod", new Color(0xFCD975));
        colorTable.put("Gray", new Color(0x95918C));
        colorTable.put("Gray05", new Color(0x0D0D0D));
        colorTable.put("Gray10", new Color(0x1A1A1A));
        colorTable.put("Gray15", new Color(0x262626));
        colorTable.put("Gray20", new Color(0x333333));
        colorTable.put("Gray25", new Color(0x404040));
        colorTable.put("Gray30", new Color(0x4D4D4D));
        colorTable.put("Gray35", new Color(0x595959));
        colorTable.put("Gray40", new Color(0x666666));
        colorTable.put("Gray45", new Color(0x737373));
        colorTable.put("Gray55", new Color(0x8C8C8C));
        colorTable.put("Gray60", new Color(0x999999));
        colorTable.put("Gray65", new Color(0xA6A6A6));
        colorTable.put("Gray70", new Color(0xB3B3B3));
        colorTable.put("Gray75", new Color(0xBFBFBF));
        colorTable.put("Gray80", new Color(0xCCCCCC));
        colorTable.put("Gray85", new Color(0xD9D9D9));
        colorTable.put("Gray90", new Color(0xE5E5E5));
        colorTable.put("Gray95", new Color(0xF2F2F2));
        colorTable.put("Green", new Color(0x1CAC78));
        colorTable.put("GreenYellow", new Color(0xF0E891));
        colorTable.put("JungleGreen", new Color(0x3BB08F));
        colorTable.put("Lavender", new Color(0xFCB4D5));
        colorTable.put("LFadedGreen", new Color(0x548B54));
        colorTable.put("LightCyan", new Color(0xE0FFFF));
        colorTable.put("LightGreen", new Color(0x90EE90));
        colorTable.put("LightMagenta", new Color(0xFF00FF));
        colorTable.put("LightOrange", new Color(0xFF6F1A));
        colorTable.put("LightPurple", new Color(0xE066FF));
        colorTable.put("LightYellow", new Color(0xFFFFE0));
        colorTable.put("LimeGreen", new Color(0x32CD32));
        colorTable.put("LSkyBlue", new Color(0x87CEFA));
        colorTable.put("Magenta", new Color(0xF664AF));
        colorTable.put("Mahogany", new Color(0xCD4A4A));
        colorTable.put("Maroon", new Color(0xC8385A));
        colorTable.put("Melon", new Color(0xFDBCB4));
        colorTable.put("MidnightBlue", new Color(0x1A4876));
        colorTable.put("Mulberry", new Color(0xAA709F));
        colorTable.put("NavyBlue", new Color(0x1974D2));
        colorTable.put("OliveGreen", new Color(0xBAB86C));
        colorTable.put("Orange", new Color(0xFF7538));
        colorTable.put("OrangeRed", new Color(0xFF5349));
        colorTable.put("Orchid", new Color(0xE6A8D7));
        colorTable.put("Peach", new Color(0xFFCFAB));
        colorTable.put("Periwinkle", new Color(0xC5D0E6));
        colorTable.put("PineGreen", new Color(0x158078));
        colorTable.put("Pink", new Color(0xFFC0CB));
        colorTable.put("Plum", new Color(0x8E4585));
        colorTable.put("ProcessBlue", new Color(0x4169E1));
        colorTable.put("Purple", new Color(0x926EAE));
        colorTable.put("RawSienna", new Color(0xD68A59));
        colorTable.put("Red", new Color(0xEE204D));
        colorTable.put("RedOrange", new Color(0xFF5349));
        colorTable.put("RedViolet", new Color(0xC0448F));
        colorTable.put("Rhodamine", new Color(0xE0119D));
        colorTable.put("RoyalBlue", new Color(0x4169E1));
        colorTable.put("RoyalPurple", new Color(0x7851A9));
        colorTable.put("RubineRed", new Color(0xCA005D));
        colorTable.put("Salmon", new Color(0xFF9BAA));
        colorTable.put("SeaGreen", new Color(0x9FE2BF));
        colorTable.put("Sepia", new Color(0xA5694F));
        colorTable.put("SkyBlue", new Color(0x80DAEB));
        colorTable.put("SpringGreen", new Color(0xECEABE));
        colorTable.put("Tan", new Color(0xFAA76C));
        colorTable.put("TealBlue", new Color(0x008080));
        colorTable.put("Thistle", new Color(0xD8BFD8));
        colorTable.put("Turquoise", new Color(0x77DDE7));
        colorTable.put("Violet", new Color(0x926EAE));
        colorTable.put("VioletRed", new Color(0xF75394));
        colorTable.put("White", new Color(0xEDEDED));
        colorTable.put("WildStrawberry", new Color(0xFF43A4));
        colorTable.put("Yellow", new Color(0xFCE883));
        colorTable.put("YellowGreen", new Color(0xC5E384));
        colorTable.put("YellowOrange", new Color(0xFFB653));
    }

    public static Integer getColorFromName(String colorName) {
        Color color = colorTable.get(colorName);

        if (color == null) {
            color = new Color(0x95918C); //Gray
        }
        return color.getRGB();
    }

    public static boolean containsColor(String colorName) {
        return colorTable.containsKey(colorName);
    }

    public static String getNameFromColor(int color) {
        //Attempt to get the closest color match
        String colorName = "Gray";
        int dist = Integer.MAX_VALUE;
        Set s = colorTable.entrySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            Color meColor = (Color)me.getValue();
            int colorDist= Math.abs(color - meColor.getRGB());
            if (colorDist < dist) {
                colorName = (String)me.getKey();
                dist = colorDist;
            }
        }
        return colorName;
    }

}
