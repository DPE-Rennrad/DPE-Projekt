package edu.thi.demo.service;

import java.util.Random;


public class MotivationGenerator {

    // Wortlisten für die Spruchgenerierung mit Unterstützung von KI erstellt
    private static final String[] SUBJEKTE = {
            "Du", "Dein Traum", "Deine Kraft", "Dein Wille", "Deine Leidenschaft"
    };

    private static final String[] VERBEN = {
            "erreicht", "erobert", "verwirklicht", "schafft", "erschafft", "meistert"
    };

    private static final String[] ADJEKTIVE = {
            "großartige", "unglaubliche", "wunderbare", "fantastische", "beeindruckende", "außergewöhnliche"
    };

    private static final String[] OBJEKTE = {
            "Ziele", "Träume", "Erfolge", "Möglichkeiten", "Chancen", "Herausforderungen"
    };

    private static final String[] ANFÄNGE = {
            "Glaube an dich", "Vertraue dir selbst", "Sei mutig", "Bleib stark", "Niemals aufgeben"
    };

    private static final String[] ENDEN = {
            "und alles wird möglich", "denn du bist stärker als du denkst",
            "und der Erfolg wird folgen", "denn du verdienst es",
            "und du wirst siegen"
    };


    private Random random;
    public MotivationGenerator() {
        this.random = new Random();
    }

    public String generiereSpruch() {
        int spruchTyp = random.nextInt(3);

        switch (spruchTyp) {
            case 0:
                return generiereTyp1();
            case 1:
                return generiereTyp2();
            case 2:
                return generiereTyp3();
            default:
                return generiereTyp1();
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

        return adjektiv.substring(0, 1).toUpperCase() + adjektiv.substring(1) +
                " " + objekt + " " + verb + " " + subjekt + "!";
    }


    public static void main(String[] args) {
        MotivationGenerator generator = new MotivationGenerator();
        System.out.println( generator.generiereSpruch());
    }
}