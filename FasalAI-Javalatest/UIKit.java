import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * UIKit -- small, self-contained custom-painted Swing components used by
 * FasalAIApp: rounded cards, a pill button, gradient progress bars, a
 * "typical range" bar for the Why-this-crop panel, bordered text fields
 * with a focus highlight, and a programmatically-drawn app icon.
 *
 * None of this touches CropModel or CropInfo -- it is purely visual.
 * Zero external dependencies: AWT/Swing only, so `javac *.java` keeps working
 * with nothing but a plain JDK.
 */
public class UIKit {

    /** Call once, before any Swing component is created, for smoother text and shapes. */
    public static void enableAntialiasing() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ---------------------------------------------------------------- RoundedPanel

    /** A JPanel painted as a rounded rectangle, with an optional 1-2px border and
     *  an optional soft drop shadow. It also caps getMaximumSize() to its preferred
     *  height so a BoxLayout/GridBagLayout parent never stretches it vertically. */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        private final Color borderColor;
        private final int borderWidth;
        private final boolean shadow;

        public RoundedPanel(int radius, Color bg, Color borderColor, int borderWidth, boolean shadow) {
            this.radius = radius;
            this.bg = bg;
            this.borderColor = borderColor;
            this.borderWidth = borderWidth;
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            int pad = shadow ? 4 : 1;
            if (shadow) {
                g2.setColor(new Color(0x1B, 0x43, 0x32, 20));
                g2.fillRoundRect(3, 5, w - 6, h - 7, radius, radius);
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - pad, h - pad - 1, radius, radius);
            if (borderWidth > 0 && borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderWidth));
                g2.drawRoundRect(0, 0, w - pad - borderWidth, h - pad - borderWidth - 1, radius, radius);
            }
            g2.dispose();
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, pref.height);
        }
    }

    // ---------------------------------------------------------------- PillButton

    /** A flat, rounded "pill" button with a hover/press color shift, no default
     *  Swing chrome at all. */
    public static class PillButton extends JButton {
        private final Color base, hover, pressedColor;

        public PillButton(String text, Color base, Color fg) {
            super(text);
            this.base = base;
            this.hover = mix(base, Color.WHITE, 0.14);
            this.pressedColor = mix(base, Color.BLACK, 0.12);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(fg);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        private static Color mix(Color a, Color b, double t) {
            int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            ButtonModel m = getModel();
            Color c = m.isPressed() ? pressedColor : (m.isRollover() ? hover : base);
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------------------------------------------------------------- GradientBar

    /** A rounded, gradient-filled horizontal bar for confidence percentages. */
    public static class GradientBar extends JComponent {
        private int value; // 0-100
        private final Color color;

        public GradientBar(Color color) {
            this.color = color;
            setOpaque(false);
        }

        public void setValue(int v) {
            this.value = Math.max(0, Math.min(100, v));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0xF0, 0xEC, 0xE0));
            g2.fillRoundRect(0, 0, w, h, h, h);
            int fw = (int) Math.round(w * (value / 100.0));
            if (fw > 0) {
                fw = Math.max(fw, h);
                GradientPaint gp = new GradientPaint(0, 0, mix(color, Color.WHITE, 0.25), 0, h, color);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, Math.min(fw, w), h, h, h);
            }
            g2.dispose();
        }

        private static Color mix(Color a, Color b, double t) {
            int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(140, 10);
        }
    }

    // ---------------------------------------------------------------- RangeBar

    /** Plots one input value against a crop's typical 10th-90th percentile band,
     *  on a [min,max] scale -- the "Why this crop?" explainability visual. The
     *  marker is drawn green when the value sits inside the typical band, and
     *  amber when it does not -- straight from the data, not decorative. */
    public static class RangeBar extends JComponent {
        private double min = 0, max = 1, lo = 0, hi = 1, value = 0;
        private Color bandColor = new Color(0x2E, 0x8F, 0x5B);

        public void set(double min, double max, double lo, double hi, double value, Color bandColor) {
            this.min = min;
            this.max = max;
            this.lo = lo;
            this.hi = hi;
            this.value = value;
            this.bandColor = bandColor;
            repaint();
        }

        private int xFor(double v, int w) {
            double span = (max - min);
            double t = span == 0 ? 0.5 : (v - min) / span;
            t = Math.max(0, Math.min(1, t));
            return (int) Math.round(t * (w - 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            int trackY = h / 2 - 2;
            g2.setColor(new Color(0xE3, 0xDE, 0xCF));
            g2.fillRoundRect(0, trackY, w, 4, 4, 4);

            int xLo = xFor(lo, w), xHi = xFor(hi, w);
            g2.setColor(new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), 90));
            g2.fillRoundRect(xLo, trackY - 3, Math.max(xHi - xLo, 6), 10, 6, 6);

            int xv = xFor(value, w);
            boolean inRange = value >= lo && value <= hi;
            Color dot = inRange ? new Color(0x2E, 0x8F, 0x5B) : new Color(0xC2, 0x5B, 0x2E);
            int r = 6;
            g2.setColor(dot);
            g2.fillOval(xv - r, h / 2 - r, r * 2, r * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(xv - r, h / 2 - r, r * 2, r * 2);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 22);
        }
    }

    // ---------------------------------------------------------------- FieldBox

    /** Wraps a JTextField in a rounded, bordered box whose border brightens
     *  on focus -- replaces the default Swing text-field chrome. */
    public static class FieldBox extends JPanel {
        private boolean focused = false;
        private final Color normalBorder, focusBorder;

        public FieldBox(JTextField field, Color normalBorder, Color focusBorder) {
            super(new BorderLayout());
            this.normalBorder = normalBorder;
            this.focusBorder = focusBorder;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            field.setOpaque(false);
            field.setBorder(new EmptyBorder(6, 10, 6, 10));
            field.setFont(new Font("Monospaced", Font.PLAIN, 13));
            add(field, BorderLayout.CENTER);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            setPreferredSize(new Dimension(10, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2.setColor(focused ? focusBorder : normalBorder);
            g2.setStroke(new BasicStroke(focused ? 2f : 1f));
            g2.drawRoundRect(0, 0, w - 2, h - 2, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------------------------------------------------------------- App icon

    /** A small rounded-square leaf mark, drawn in code -- used as the window icon
     *  so the app doesn't need to ship or load an external image file. */
    public static Image createAppIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0x1B, 0x43, 0x32));
        g2.fillRoundRect(0, 0, size, size, size / 4, size / 4);

        double s = size;
        GeneralPath leaf = new GeneralPath();
        leaf.moveTo(s * 0.5, s * 0.16);
        leaf.curveTo(s * 0.86, s * 0.26, s * 0.86, s * 0.66, s * 0.5, s * 0.86);
        leaf.curveTo(s * 0.14, s * 0.66, s * 0.14, s * 0.26, s * 0.5, s * 0.16);
        leaf.closePath();
        g2.setColor(new Color(0x2E, 0x8F, 0x5B));
        g2.fill(leaf);

        g2.setColor(new Color(0x1B, 0x43, 0x32));
        g2.setStroke(new BasicStroke((float) Math.max(1.0, s * 0.035)));
        g2.draw(new Line2D.Double(s * 0.5, s * 0.28, s * 0.5, s * 0.8));
        g2.dispose();
        return img;
    }
}
