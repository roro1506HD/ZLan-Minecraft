package fr.roro.zlan.manager.config;

import java.util.Map;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
class ConfigEntry {

    private String               googleDocumentId;
    private String               googleDocumentTeamsRange;
    private Map<Integer, String> googleDocumentTeamsStatsRange;
    private double               borderSize;
    private int                  borderTime;

    String getGoogleDocumentId() {
        return this.googleDocumentId;
    }

    String getGoogleDocumentTeamsRange() {
        return this.googleDocumentTeamsRange;
    }

    String getGoogleDocumentTeamsStatsRange(int round) {
        return this.googleDocumentTeamsStatsRange.get(round);
    }

    double getBorderSize() {
        return this.borderSize;
    }

    int getBorderTime() {
        return this.borderTime;
    }
}
