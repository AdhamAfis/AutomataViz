package com.dfavisualizer.ui;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

/** Checks that scrollbar thumbs remain distinguishable from their tracks in the active theme. */
public class ScrollbarAppearanceCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            for (int orientation : new int[] {JScrollBar.VERTICAL, JScrollBar.HORIZONTAL}) {
                for (int scale : new int[] {1, 2}) {
                    JScrollBar bar = new JScrollBar(orientation, 30, 40, 0, 100);
                    bar.setSize(orientation == JScrollBar.VERTICAL ? 16 : 240,
                            orientation == JScrollBar.VERTICAL ? 240 : 16);
                    bar.doLayout();
                    checkThumb(bar, scale, "normal");
                    bar.dispatchEvent(new MouseEvent(bar, MouseEvent.MOUSE_MOVED, 1, 0,
                            bar.getWidth() / 2, bar.getHeight() / 2, 0, false));
                    checkThumb(bar, scale, "hover");
                    bar.dispatchEvent(new MouseEvent(bar, MouseEvent.MOUSE_PRESSED, 2, 0,
                            bar.getWidth() / 2, bar.getHeight() / 2, 1, false, MouseEvent.BUTTON1));
                    checkThumb(bar, scale, "pressed");
                    bar.dispatchEvent(new MouseEvent(bar, MouseEvent.MOUSE_RELEASED, 3, 0,
                            bar.getWidth() / 2, bar.getHeight() / 2, 1, false, MouseEvent.BUTTON1));
                    assert !bar.getValueIsAdjusting() : "Scrollbar remains in drag mode";
                    assert bar.getValue() == 30 && bar.getVisibleAmount() == 40 : "Painting changed the scroll range";
                }
            }
        });
        System.out.println("Scrollbar appearance checks passed (both orientations, normal/hover/pressed, 1x/2x)");
    }

    private static void checkThumb(JScrollBar bar, int scale, String state) {
        BufferedImage image = new BufferedImage(bar.getWidth() * scale, bar.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(scale, scale);
        bar.paint(graphics);
        graphics.dispose();
        int thumb = image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
        int track = bar.getOrientation() == JScrollBar.VERTICAL
                ? image.getRGB(image.getWidth() / 2, image.getHeight() - 35 * scale)
                : image.getRGB(image.getWidth() - 35 * scale, image.getHeight() / 2);
        double thumbLight = luminance(thumb), trackLight = luminance(track);
        double contrast = (Math.max(thumbLight, trackLight) + 0.05) / (Math.min(thumbLight, trackLight) + 0.05);
        try {
            Files.createDirectories(Path.of("target/visual-checks"));
            ImageIO.write(image, "png", Path.of("target/visual-checks/scrollbar-" + bar.getOrientation()
                    + "-" + state + "-" + scale + ".png").toFile());
        } catch (Exception e) { throw new RuntimeException(e); }
        assert contrast >= 3 : "Scrollbar thumb blends into track (" + state + ", contrast " + contrast + ")";
    }

    private static double luminance(int rgb) {
        double result = 0;
        double[] weights = {0.2126, 0.7152, 0.0722};
        for (int channel = 0; channel < 3; channel++) {
            double value = ((rgb >> (16 - channel * 8)) & 255) / 255.0;
            result += weights[channel] * (value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4));
        }
        return result;
    }
}
