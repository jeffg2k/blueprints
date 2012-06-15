package com.tinkerpop.blueprints.util.io.tlp;

/**
 * A collection of tokens used for TLP related data.
 * <p/>
 * TLP definition taken from
 * (http://tulip.labri.fr/TulipDrupal/?q=tlp-file-format)
 * <p/>
 *
 * @author Jeff Gentes
 */
public class TLPTokens {
    public static final String TLP = "tlp";
    public static final String ID = "id";
    public static final String DEFAULT = "(default";
    public static final String NODEPROPERTY = "(node";
    public static final String NODE = "(nodes";
    public static final String EDGE = "(edge";
    public static final String PROPERTY = "(property";
    public static final String COMMENT_CHAR = ";";
    public static final String LABEL = "viewLabel";
    public static final String COLOR = "viewColor";

    /**
     * Special token used to store Blueprint ids as they may not be integers
     */
    public static final String BLUEPRINTS_ID = "blueprintsId";
}
