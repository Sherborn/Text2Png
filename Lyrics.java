import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Convert textual song lyrics to transparent .png files for use with OBS.
 * 
 * 0) get text files for the songs with blank lines between stanzas
 * 1) install JDK 11 (ask 900913 for help)
 * 2) javac Lyrics.java
 * 3) java Lyrics /path/to/folder/with/dot/txt/files
 * 
 * The output will be lyrics slides organized by song under
 * /path/to/folder/with/dot/txt/files/images
 *
 * @Version 0.0.6
 * @Date 2021-08-29
 */

class Config {
    public static void usage() {
        System.out.println("usage:  java Lyrics [--width width] [--height height] [--position top|bottom] [--percent percentOfHeight] [--font \"droid sans\"] /path/to/directory/containing/txt/files");
        System.exit(13);
    }

    private int height = 1080;
    private int width = 1920;
    private Color backColorStart = new Color(45, 45, 45, 0);
    private Color backColor = new Color(45, 45, 45, 220);
    private Color foreColor = new Color(250, 250, 250, 255);
    private String imageDirectoryName = "images";
    private int xLyricsMargin = width / 20;
    private int yLyricsStart = 0;
    private int yLyricsSize = height / 2;
    private String fontFamily = "open sans"; // or "Utopia", "droid sans", "nimbus sans"
    private File lyricsDirectory;

    public void updateFromArgs(String[] args) {
        if (args.length == 0) {
            usage();
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].matches("-[a-zA-Z0-9]+")) {
                usage();
            }
        }
        for (int i = 0; i < args.length; i++) {
            if ("--height".equals(args[i])) {
                height = Integer.parseInt(args[++i]);
            }
            else if ("--width".equals(args[i])) {
                width = Integer.parseInt(args[++i]);
                xLyricsMargin = width / 20;
            }
            else if ("--font".equals(args[i])) {
                fontFamily = args[++i];
            }
        }
        for (int i = 0; i < args.length; i++) {
            if ("--percent".equals(args[i])) {
                int percent = Integer.parseInt(args[++i]);
                yLyricsSize = percent * height / 100;
                break;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if ("--position".equals(args[i])) {
                if (!"top".equalsIgnoreCase(args[++i])) {
                    yLyricsStart = height - yLyricsSize;
                }
                break;
            }
        }
        File f = new File(args[args.length - 1]);
        if (!f.isDirectory()) {
            usage();
        }
        lyricsDirectory = f;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Color getBackColorStart() {
        return backColorStart;
    }

    public Color getBackColor() {
        return backColor;
    }

    public Color getForeColor() {
        return foreColor;
    }

    public String getImageDirectoryName() {
        return imageDirectoryName;
    }

    public int getxLyricsMargin() {
        return xLyricsMargin;
    }

    public int getyLyricsStart() {
        return yLyricsStart;
    }

    public int getyLyricsSize() {
        return yLyricsSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public File getLyricsDirectory() {
        return lyricsDirectory;
    }
}

public class Lyrics {

    private static Config config = new Config();
    
    public static void main(String[] args) throws IOException {
        config.updateFromArgs(args);
        for (File f : config.getLyricsDirectory().listFiles((f2, name) -> name.endsWith(".txt"))) {
            processFile(f);
        }
    }

    /**
     * A stanza is a list of strings.
     * A song is a list of stanzas.
     */
    public static List<List<String>> parseStanzas(File file) throws IOException {
        List<List<String>> result = new ArrayList<>();
        List<String> stanza = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (stanza != null) {
                        result.add(stanza);
                        stanza = null;
                    }
                }
                else {
                    if (stanza == null) {
                        stanza = new ArrayList<>();
                    }
                    stanza.add(line);
                }
            }
            if (stanza != null) {
                result.add(stanza);
            }
        }
        return result;
    }

    public static String abbreviatedNameFor(String fileName) {
        final int MAX = 15;
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            fileName = fileName.substring(0, index);
        }
        fileName = fileName.replace(" ", "");
        fileName = fileName.toLowerCase();
        if (fileName.length() > MAX) {
            fileName = fileName.substring(0, MAX);
        }
        return fileName;
    }

    public static void processFile(File file) throws IOException {
        String stub = abbreviatedNameFor(file.getName());
        File dest = new File(file.getParentFile(), config.getImageDirectoryName() + "/" + stub + "/");
        dest.mkdirs();
        int count = 0;
        processStanza(Collections.emptyList(), dest, stub, count++); // initial empty slide
        for (List<String> stanza : parseStanzas(file)) {
            processStanza(stanza, dest, stub, count++);
        }
        processStanza(Collections.emptyList(), dest, stub, count++); // final empty slide
    }

    public static Font pickFont(Graphics2D g, List<String> stanza) {
        String maxLine = "a";
        if (stanza != null && !stanza.isEmpty()) {
            for (String s : stanza) {
                if (s.length() > maxLine.length()) {
                    maxLine = s;
                }
            }
        }
        int fontSize = stanza != null && stanza.size() > 6 ? 44 : 56;
        Font font;
        FontMetrics fm;
        do {
            fontSize -= 4;
            font = new Font(config.getFontFamily(), Font.BOLD, fontSize);
            fm = g.getFontMetrics(font);
        } while (fm.stringWidth(maxLine) > config.getWidth() - 2*config.getxLyricsMargin());
        return font;
    }

    public static void processStanza(List<String> stanza, File dir, String stubName, int count) throws IOException {
        BufferedImage bi = new BufferedImage(config.getWidth(), config.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        RenderingHints rh = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_RENDERING, 
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHints(rh);
        Font font = pickFont(g, stanza);
        g.setFont(font);
        System.out.println("FONT " + font);

        // experimental
        // BufferedImage band = ImageIO.read(new File("band.jpg"));
        // BufferedImage band2 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        // Graphics2D g2 = band2.createGraphics();
        // g2.drawImage(band, 0, 0, WIDTH, HEIGHT, null);
        // g2.dispose();
        // g.drawImage(band2, 0, 0, WIDTH, HEIGHT, null);
        // AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        // g.setComposite(alphaChannel);

        // GradientPaint gp = new GradientPaint(0, LYRICS_Y, BACKGROUND_START_COLOR, 0, LYRICS_Y + TRANSITION, BACKGROUND_COLOR);
        // g.setPaint(gp);
        // g.fillRect(0, LYRICS_Y, WIDTH, HEIGHT);

        g.setColor(config.getBackColor());
        g.fillRect(0, config.getyLyricsStart(), config.getWidth(), config.getyLyricsSize());
        g.setColor(config.getForeColor());
        int denominator = Math.max(stanza.size(), 4);
        int dy = config.getyLyricsSize() / denominator;
        int y = config.getyLyricsStart() + (3 * dy / 4);
        for (String line : stanza) {
            g.drawString(line, config.getxLyricsMargin(), y);
            y += dy;
        }
        
        String prefix = String.valueOf(count);
        prefix = prefix.length() < 2 ? "0" + prefix : prefix;

        ImageIO.write(bi, "png", new File(dir, prefix + stubName + ".png"));
        g.dispose();
    }

}