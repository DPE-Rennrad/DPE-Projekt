//Niklas Putz

package edu.thi.demo.services;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Random;

@Path("/spruch")
public class Spruch {

    private final Random random = new Random();

    private static final String[] SUBJEKTE = {"Du", "Dein Traum", "Deine Kraft", "Dein Wille", "Deine Leidenschaft"};
    private static final String[] VERBEN = {"erreicht", "erobert", "verwirklicht", "schafft", "erschafft", "meistert"};
    private static final String[] ADJEKTIVE = {"großartige", "unglaubliche", "wunderbare", "fantastische", "beeindruckende", "außergewöhnliche"};
    private static final String[] OBJEKTE = {"Ziele", "Träume", "Erfolge", "Möglichkeiten", "Chancen", "Herausforderungen"};
    private static final String[] ANFÄNGE = {"Glaube an dich", "Vertraue dir selbst", "Sei mutig", "Bleib stark", "Niemals aufgeben"};
    private static final String[] ENDEN = {"und alles wird möglich", "denn du bist stärker als du denkst", "und der Erfolg wird folgen", "denn du verdienst es", "und du wirst siegen"};

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSpruch() {
        return generiereSpruch();
    }

    public String generiereSpruch() {
        int spruchTyp = random.nextInt(3);
        switch (spruchTyp) {
            case 0: return generiereTyp1();
            case 1: return generiereTyp2();
            case 2: return generiereTyp3();
            default: return generiereTyp1();
        }
    }

    private String generiereTyp1() {
        String subjekt = SUBJEKTE[random.nextInt(SUBJEKTE.length)];
        String verb = VERBEN[random.nextInt(VERBEN.length)];
        String adjektiv = ADJEKTIVE[random.nextInt(ADJEKTIVE.length)];
        String objekt = OBJEKTE[random.nextInt(OBJEKTE.length)];
        return subjekt + " " + verb + " " + adjektiv + " " + objekt + "!";
    }

    private String generiereTyp2() {
        String anfang = ANFÄNGE[random.nextInt(ANFÄNGE.length)];
        String ende = ENDEN[random.nextInt(ENDEN.length)];
        return anfang + ", " + ende + "!";
    }

    private String generiereTyp3() {
        String adjektiv = ADJEKTIVE[random.nextInt(ADJEKTIVE.length)];
        String objekt = OBJEKTE[random.nextInt(OBJEKTE.length)];
        String verb = VERBEN[random.nextInt(VERBEN.length)];
        String subjekt = SUBJEKTE[random.nextInt(SUBJEKTE.length)].toLowerCase();
        return adjektiv.substring(0, 1).toUpperCase() + adjektiv.substring(1) + " " + objekt + " " + verb + " " + subjekt + "!";
    }
}