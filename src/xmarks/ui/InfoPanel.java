package xmarks.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

/** Glass card with a heading, a block of text and a Back button. Base for How to Play and Credits. */
public class InfoPanel extends BackdropPanel {

    public InfoPanel(String title, String body, Runnable onBack) {
        super(true);
        setLayout(new GridBagLayout());

        GlassPanel card = new GlassPanel(36, 120);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(26, 36, 26, 36));
        card.setPreferredSize(new Dimension(660, 440));

        JLabel heading = new JLabel(title.toUpperCase(), SwingConstants.CENTER);
        heading.setFont(Theme.title(30f));
        heading.setForeground(Theme.LOGO);

        JTextArea text = new JTextArea(body);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.body(16f, Font.PLAIN));
        text.setForeground(Theme.TEXT);

        GlassButton back = new GlassButton("BACK");
        back.setPreferredSize(new Dimension(160, 42));
        back.addActionListener(e -> onBack.run());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        south.setOpaque(false);
        south.add(back);

        card.add(heading, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        add(card);
    }
}