import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * FasalAIApp -- desktop version of the FasalAI crop recommendation tool.
 *
 * Enter a field's soil nutrients and local weather; CropModel (the trained
 * Random Forest, ported from Python) predicts the best-fit crop live, in
 * this JVM, with no network call and no server. The "Why this crop?" panel
 * plots each input against that crop's own typical range in the training
 * data, the same explainability idea used on the website.
 *
 * Group 8 -- AI-Based Crop Recommendation for Farmers.
 */
public class FasalAIApp extends JFrame {

    // ---- palette, matched to the deck / website ----
    private static final Color BG = new Color(0xFB, 0xF9, 0xF4);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3, 0xDE, 0xCF);
    private static final Color TEXT = new Color(0x1F, 0x2A, 0x24);
    private static final Color TEXT_MUTED = new Color(0x5F, 0x72, 0x65);
    private static final Color BRAND = new Color(0x1B, 0x43, 0x32);
    private static final Color ACCENT = new Color(0x2E, 0x8F, 0x5B);
    private static final Color WARN = new Color(0xC2, 0x5B, 0x2E);

    private static final String[] FIELD_LABELS = {
        "Nitrogen", "Phosphorus", "Potassium", "Soil pH",
        "Temperature", "Humidity", "Rainfall"
    };
    private static final String[] FIELD_UNITS = {
        "kg/ha", "kg/ha", "kg/ha", "", "°C", "%", "mm"
    };
    // Rice's own median reading -- the app opens already showing a live, correct prediction.
    private static final double[] DEFAULTS = { 80, 47, 40, 6.4, 23.7, 82.2, 233.1 };

    // Form fields are N,P,K,pH,temperature,humidity,rainfall.
    // CropModel.FEATURES (and every array in CropInfo) are N,P,K,temperature,humidity,ph,rainfall.
    // FORM_TO_MODEL[formIndex] = modelIndex -- the single source of truth for that
    // reordering, used everywhere it's needed so the two orderings can't drift apart.
    private static final int[] FORM_TO_MODEL = { 0, 1, 2, 5, 3, 4, 6 };

    private static double[] formToModel(double[] form) {
        double[] model = new double[7];
        for (int i = 0; i < 7; i++) model[FORM_TO_MODEL[i]] = form[i];
        return model;
    }

    private static double[] modelToForm(double[] model) {
        double[] form = new double[7];
        for (int i = 0; i < 7; i++) form[i] = model[FORM_TO_MODEL[i]];
        return form;
    }

    private final JTextField[] fields = new JTextField[7];
    private final JComboBox<String> exampleBox;
    private final JLabel statusLabel;

    private final JLabel resultBadge = new JLabel();
    private final JLabel resultGroup = new JLabel();
    private final JLabel resultName = new JLabel();
    private final JLabel resultFact = new JLabel();
    private final JLabel resultConf = new JLabel();
    private final UIKit.GradientBar resultConfBar = new UIKit.GradientBar(ACCENT);
    private final JPanel runnerUpsPanel = new JPanel();

    private final UIKit.PillButton whyToggle = new UIKit.PillButton("Why this crop?  ▾", BRAND, Color.WHITE);
    private final JPanel whyContent = new JPanel();
    private boolean whyOpen = false;

    private static final DecimalFormat PCT = new DecimalFormat("0.0");
    private static final DecimalFormat NUM = new DecimalFormat("0.##");

    public FasalAIApp() {
        super("FasalAI -- AI-Based Crop Recommendation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(UIKit.createAppIcon(64));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        // ---------------- header ----------------
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(BG);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(20, 24, 14, 24));

        JLabel logo = new JLabel(new ImageIcon(UIKit.createAppIcon(48)));
        logo.setBorder(new EmptyBorder(2, 0, 0, 16));
        header.add(logo);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel eyebrow = new JLabel("AI-BASED CROP RECOMMENDATION FOR FARMERS");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setForeground(ACCENT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("FasalAI");
        title.setFont(new Font("Serif", Font.BOLD, 30));
        title.setForeground(BRAND);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
            "<html><body style='width:640px'>Random Forest model (40 trees), trained on 2,200 real soil "
            + "and climate records across 22 crops &mdash; 99.5% accuracy on data it never saw during "
            + "training. This desktop app runs the same trained model as the web version.</body></html>");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(eyebrow);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock);
        header.add(Box.createHorizontalGlue());

        JPanel divider = new JPanel();
        divider.setBackground(BORDER);
        divider.setPreferredSize(new Dimension(10, 1));

        headerWrap.add(header, BorderLayout.CENTER);
        headerWrap.add(divider, BorderLayout.SOUTH);
        add(headerWrap, BorderLayout.NORTH);

        // ---------------- body (GridBagLayout: fixed-height columns, no stretch) ----------------
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(16, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ---- left column: field conditions ----
        JPanel inputCard = card(true);
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));

        inputCard.add(cardTitle("Field conditions"));
        inputCard.add(Box.createVerticalStrut(12));

        String[] exampleNames = new String[CropInfo.ALL.length];
        for (int i = 0; i < CropInfo.ALL.length; i++) exampleNames[i] = CropInfo.ALL[i].label;
        java.util.Arrays.sort(exampleNames);
        exampleBox = new JComboBox<>(exampleNames);
        exampleBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        exampleBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        exampleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        exampleBox.setSelectedItem("Rice");
        exampleBox.addActionListener(e -> loadExample());
        inputCard.add(labeled("Load an example", exampleBox));
        inputCard.add(Box.createVerticalStrut(14));

        for (int i = 0; i < FIELD_LABELS.length; i++) {
            fields[i] = new JTextField(String.valueOf(DEFAULTS[i]));
            UIKit.FieldBox box = new UIKit.FieldBox(fields[i], BORDER, ACCENT);
            String unit = FIELD_UNITS[i].isEmpty() ? "" : " (" + FIELD_UNITS[i] + ")";
            inputCard.add(labeled(FIELD_LABELS[i] + unit, box));
            inputCard.add(Box.createVerticalStrut(8));
        }

        UIKit.PillButton recommendBtn = new UIKit.PillButton("Get Recommendation", ACCENT, Color.WHITE);
        recommendBtn.addActionListener(e -> compute());
        inputCard.add(Box.createVerticalStrut(8));
        inputCard.add(recommendBtn);

        statusLabel = new JLabel("Model ready · " + CropModel.N_TREES + " trees · "
            + PCT.format(CropModel.TEST_ACCURACY * 100) + "% test accuracy");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        inputCard.add(statusLabel);

        // ---- right column: result + why-this-crop + footer ----
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        JPanel resultCard = card(true);
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));

        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultBadge.setOpaque(true);
        resultBadge.setPreferredSize(new Dimension(56, 56));
        resultBadge.setMaximumSize(new Dimension(56, 56));
        resultBadge.setHorizontalAlignment(SwingConstants.CENTER);
        resultBadge.setFont(new Font("Serif", Font.BOLD, 18));
        resultBadge.setForeground(Color.WHITE);

        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);
        nameCol.setBorder(new EmptyBorder(0, 14, 0, 0));
        nameCol.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));
        resultGroup.setFont(new Font("SansSerif", Font.BOLD, 11));
        resultGroup.setForeground(TEXT_MUTED);
        resultName.setFont(new Font("Serif", Font.BOLD, 22));
        resultName.setForeground(TEXT);
        resultFact.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultFact.setForeground(TEXT_MUTED);
        nameCol.add(resultGroup);
        nameCol.add(resultName);
        nameCol.add(Box.createVerticalStrut(4));
        nameCol.add(resultFact);

        JPanel confCol = new JPanel();
        confCol.setLayout(new BoxLayout(confCol, BoxLayout.Y_AXIS));
        confCol.setOpaque(false);
        resultConf.setFont(new Font("Monospaced", Font.BOLD, 26));
        resultConf.setForeground(ACCENT);
        resultConf.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel confLabel = new JLabel("CONFIDENCE");
        confLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        confLabel.setForeground(TEXT_MUTED);
        confLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        confCol.add(resultConf);
        confCol.add(confLabel);

        topRow.add(resultBadge);
        topRow.add(nameCol);
        topRow.add(Box.createHorizontalGlue());
        topRow.add(confCol);
        resultCard.add(topRow);

        resultCard.add(Box.createVerticalStrut(10));
        resultConfBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultConfBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        resultConfBar.setPreferredSize(new Dimension(10, 10));
        resultCard.add(resultConfBar);

        resultCard.add(Box.createVerticalStrut(18));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        resultCard.add(sep);
        resultCard.add(Box.createVerticalStrut(12));

        JLabel ruTitle = new JLabel("OTHER STRONG MATCHES");
        ruTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        ruTitle.setForeground(TEXT_MUTED);
        ruTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.add(ruTitle);
        resultCard.add(Box.createVerticalStrut(8));

        runnerUpsPanel.setLayout(new BoxLayout(runnerUpsPanel, BoxLayout.Y_AXIS));
        runnerUpsPanel.setOpaque(false);
        runnerUpsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.add(runnerUpsPanel);

        rightCol.add(resultCard);
        rightCol.add(Box.createVerticalStrut(14));

        // ---- Why this crop? collapsible explainability panel ----
        whyToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        whyToggle.addActionListener(e -> {
            whyOpen = !whyOpen;
            whyToggle.setText(whyOpen ? "Why this crop?  ▴" : "Why this crop?  ▾");
            whyContent.setVisible(whyOpen);
            rightCol.revalidate();
            rightCol.repaint();
        });
        rightCol.add(whyToggle);
        rightCol.add(Box.createVerticalStrut(10));

        whyContent.setLayout(new BoxLayout(whyContent, BoxLayout.Y_AXIS));
        whyContent.setVisible(false);
        whyContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCol.add(whyContent);
        rightCol.add(Box.createVerticalStrut(14));

        JPanel footerCard = card(false);
        footerCard.setLayout(new BoxLayout(footerCard, BoxLayout.Y_AXIS));
        JLabel footer = new JLabel(
            "<html><body style='width:320px'>Group 8 &mdash; AI-Based Crop Recommendation for Farmers. "
            + "Inputs: N, P, K in kg/ha; temperature in °C; humidity in %; soil pH; rainfall in mm. "
            + "The decision trees behind this prediction are exported directly from the trained "
            + "scikit-learn model, not hand-written rules.</body></html>");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(TEXT_MUTED);
        footerCard.add(footer);
        rightCol.add(footerCard);

        gbc.gridx = 0;
        gbc.weightx = 0.52;
        gbc.insets = new Insets(0, 0, 0, 20);
        body.add(inputCard, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.48;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(rightCol, gbc);

        // absorb any leftover width/height so the two columns stay pinned top-left
        GridBagConstraints fill = new GridBagConstraints();
        fill.gridx = 0;
        fill.gridy = 1;
        fill.gridwidth = 2;
        fill.weightx = 1;
        fill.weighty = 1;
        fill.fill = GridBagConstraints.BOTH;
        JPanel glue = new JPanel();
        glue.setOpaque(false);
        body.add(glue, fill);

        JScrollPane scroll = new JScrollPane(body,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        setSize(1060, 780);
        // Below this, the two-column layout has no room left to give up --
        // let it scroll horizontally (above) rather than silently clip content.
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        compute();
    }

    /** A card painted as a rounded rectangle on a soft shadow. */
    private JPanel card(boolean shadow) {
        UIKit.RoundedPanel p = new UIKit.RoundedPanel(14, SURFACE, BORDER, 1, shadow);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        return p;
    }

    private JLabel cardTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Serif", Font.BOLD, 17));
        l.setForeground(BRAND);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void loadExample() {
        String chosen = (String) exampleBox.getSelectedItem();
        for (CropInfo.Crop c : CropInfo.ALL) {
            if (c.label.equals(chosen)) {
                // c.example[] is in model order -- convert to form order through the
                // single shared mapping instead of a one-off inline reorder.
                double[] formOrder = modelToForm(c.example);
                for (int i = 0; i < 7; i++) fields[i].setText(NUM.format(formOrder[i]));
                compute();
                return;
            }
        }
    }

    private void compute() {
        double[] x = new double[7];
        try {
            for (int i = 0; i < 7; i++) x[i] = Double.parseDouble(fields[i].getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Enter a number in every field.");
            return;
        }
        double[] modelInput = formToModel(x);

        List<CropModel.Prediction> ranked = CropModel.predict(modelInput);
        CropModel.Prediction top = ranked.get(0);
        CropInfo.Crop meta = CropInfo.get(top.crop);

        resultBadge.setText(meta.label.length() >= 2 ? meta.label.substring(0, 2).toUpperCase() : meta.label.toUpperCase());
        resultBadge.setBackground(new Color(meta.color));
        resultGroup.setText(meta.group.toUpperCase());
        resultName.setText(meta.label);
        resultFact.setText("<html><body style='width:230px'>" + meta.fact + "</body></html>");
        resultConf.setText(PCT.format(top.confidence * 100) + "%");
        resultConfBar.setValue((int) Math.round(top.confidence * 100));

        runnerUpsPanel.removeAll();
        for (int i = 1; i <= 3 && i < ranked.size(); i++) {
            CropModel.Prediction r = ranked.get(i);
            CropInfo.Crop rMeta = CropInfo.get(r.crop);
            runnerUpsPanel.add(runnerUpRow(rMeta, r.confidence));
            runnerUpsPanel.add(Box.createVerticalStrut(8));
        }
        runnerUpsPanel.revalidate();
        runnerUpsPanel.repaint();

        rebuildWhyPanel(meta, x);

        statusLabel.setText("Model ready · " + CropModel.N_TREES + " trees · "
            + PCT.format(CropModel.TEST_ACCURACY * 100) + "% test accuracy");
    }

    /** Rebuilds the "Why this crop?" rows: one per input feature, each plotting
     *  the value the farmer entered against this crop's own typical 10th-90th
     *  percentile band from the training data. */
    private void rebuildWhyPanel(CropInfo.Crop meta, double[] formValues) {
        whyContent.removeAll();

        JLabel hint = new JLabel(
            "<html><body style='width:320px'>How each value you entered compares to " + meta.label
            + "'s typical range in the training data. The dot is your value; "
            + "the shaded band is where most " + meta.label.toLowerCase() + " rows fall.</body></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        whyContent.add(hint);
        whyContent.add(Box.createVerticalStrut(12));

        for (int formIdx = 0; formIdx < 7; formIdx++) {
            int modelIdx = FORM_TO_MODEL[formIdx];
            double lo = meta.rangeLo[modelIdx];
            double hi = meta.rangeHi[modelIdx];
            double value = formValues[formIdx];
            double pad = Math.max((hi - lo) * 0.6, 0.0001);
            double min = Math.min(lo, value) - pad;
            double max = Math.max(hi, value) + pad;

            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            JPanel labelRow = new JPanel(new BorderLayout());
            labelRow.setOpaque(false);
            labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel name = new JLabel(FIELD_LABELS[formIdx]);
            name.setFont(new Font("SansSerif", Font.BOLD, 12));
            name.setForeground(TEXT);
            String unit = FIELD_UNITS[formIdx].isEmpty() ? "" : " " + FIELD_UNITS[formIdx];
            JLabel val = new JLabel(NUM.format(value) + unit + "   (typical " + NUM.format(lo) + "–" + NUM.format(hi) + unit + ")");
            val.setFont(new Font("Monospaced", Font.PLAIN, 11));
            val.setForeground((value >= lo && value <= hi) ? ACCENT : WARN);
            labelRow.add(name, BorderLayout.WEST);
            labelRow.add(val, BorderLayout.EAST);

            UIKit.RangeBar bar = new UIKit.RangeBar();
            bar.set(min, max, lo, hi, value, new Color(meta.color));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            row.add(labelRow);
            row.add(Box.createVerticalStrut(4));
            row.add(bar);

            whyContent.add(row);
            whyContent.add(Box.createVerticalStrut(10));
        }

        whyContent.revalidate();
        whyContent.repaint();
    }

    private JPanel runnerUpRow(CropInfo.Crop meta, double confidence) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(meta.color));
        dot.setPreferredSize(new Dimension(16, 16));

        JLabel name = new JLabel(meta.label);
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setPreferredSize(new Dimension(110, 16));

        UIKit.GradientBar bar = new UIKit.GradientBar(new Color(meta.color));
        bar.setValue((int) Math.round(confidence * 100));
        bar.setPreferredSize(new Dimension(140, 8));

        JLabel pct = new JLabel(PCT.format(confidence * 100) + "%");
        pct.setFont(new Font("Monospaced", Font.PLAIN, 11));
        pct.setForeground(TEXT_MUTED);
        pct.setPreferredSize(new Dimension(44, 16));
        pct.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(dot);
        row.add(name);
        row.add(bar);
        row.add(Box.createHorizontalStrut(8));
        row.add(pct);
        return row;
    }

    public static void main(String[] args) {
        UIKit.enableAntialiasing();
        SwingUtilities.invokeLater(() -> new FasalAIApp().setVisible(true));
    }
}
