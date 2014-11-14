package dreaam.developer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import sami.mission.MockupInEdge;
import sami.mission.MockupOutEdge;
import sami.mission.MockupPlace;
import sami.mission.MockupPlace.MockupSubMissionType;
import sami.mission.MockupTransition;
import sami.mission.MockupTransition.MockupIeStatus;

/**
 *
 * @author nbb
 */
public class MockupDetailsD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(MockupDetailsD.class.getName());
    final static int BUTTON_WIDTH = 100;
    final static int BUTTON_HEIGHT = 50;
    private JScrollPane scrollPane;
    JPanel panel;
    JTextField nameTF;
    MockupOeDetailsP oeP;
    MockupTokenDetailsP tokenP;
    MockupSMDetailsP smP;
    MockupIeDetailsP ieP;
    MockupEdgeDetailsP edgeP;

    public MockupDetailsD(java.awt.Frame parent, boolean modal, MockupInEdge mockupEdge) {
        super(parent, modal);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        edgeP = new MockupEdgeDetailsP(mockupEdge.getMockupTokenRequirements());
        panel.add(edgeP);

        finishLayout();
    }

    public MockupDetailsD(java.awt.Frame parent, boolean modal, MockupOutEdge mockupEdge) {
        super(parent, modal);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        edgeP = new MockupEdgeDetailsP(mockupEdge.getMockupTokenRequirements());
        panel.add(edgeP);

        finishLayout();
    }

    public MockupDetailsD(java.awt.Frame parent, boolean modal, MockupPlace mockupPlace) {
        super(parent, modal);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add TF for name
        nameTF = new JTextField(mockupPlace.getName());
        nameTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameTF.getPreferredSize().height));

        JPanel nameP = new JPanel();
        nameP.setLayout(new BoxLayout(nameP, BoxLayout.Y_AXIS));
        Border border = BorderFactory.createLineBorder(Color.black, 1);
        Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        nameP.setBorder(new CompoundBorder(border, margin));
        nameP.add(new JLabel("Place name"));
        nameP.add(nameTF);
        panel.add(nameP);

        oeP = new MockupOeDetailsP(mockupPlace.getMockupOutputEventMarkups());
        panel.add(oeP);

        tokenP = new MockupTokenDetailsP(mockupPlace.getMockupTokens());
        panel.add(tokenP);

        smP = new MockupSMDetailsP(mockupPlace.getMockupSubMissionType());
        panel.add(smP);

        finishLayout();
    }

    public MockupDetailsD(java.awt.Frame parent, boolean modal, MockupTransition mockupTransition) {
        super(parent, modal);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add TF for name
        nameTF = new JTextField(mockupTransition.getName());
        nameTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameTF.getPreferredSize().height));

        JPanel nameP = new JPanel();
        nameP.setLayout(new BoxLayout(nameP, BoxLayout.Y_AXIS));
        Border border = BorderFactory.createLineBorder(Color.black, 1);
        Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        nameP.setBorder(new CompoundBorder(border, margin));
        nameP.add(new JLabel("Transition name"));
        nameP.add(nameTF);
        panel.add(nameP);

        ieP = new MockupIeDetailsP(mockupTransition.getMockupInputEventStatus(), mockupTransition.getMockupInputEventMarkups());
        panel.add(ieP);

        finishLayout();
    }

    public void finishLayout() {
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(panel.getPreferredSize());

        // Add the done button
        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });
        JPanel okPanel = new JPanel(new BorderLayout(10, 10));
        okPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
        okPanel.add(doneButton);

        BoxLayout paneLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(paneLayout);
        getContentPane().setPreferredSize(new Dimension(400, 600));
        getContentPane().add(scrollPane);
        getContentPane().add(okPanel);
        pack();
    }

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public String getName() {
        if (nameTF != null) {
            return nameTF.getText();
        }
        return null;
    }

    public Hashtable<String, ArrayList<String>> getMockupOutputEventMarkups() {
        if (oeP != null) {
            return oeP.getOutputEvents();
        }
        return null;
    }

    public Hashtable<String, MockupSubMissionType> getMockupSubMissionType() {
        if (smP != null) {
            return smP.getSubmissionStatus();
        }
        return null;
    }

    public ArrayList<String> getMockupTokens() {
        if (tokenP != null) {
            return tokenP.getTokens();
        }
        return null;
    }

    public Hashtable<String, ArrayList<String>> getMockupInputEventMarkups() {
        if (ieP != null) {
            return ieP.getInputEventMarkup();
        }
        return null;
    }

    public Hashtable<String, MockupIeStatus> getMockupInputEventStatus() {
        if (ieP != null) {
            return ieP.getInputEventStatus();
        }
        return null;
    }

    public ArrayList<String> getMockupTokenRequirements() {
        if (edgeP != null) {
            return edgeP.getTokenRequirements();
        }
        return null;
    }

    interface MockupDetails {

        public void addItem();
    }

    class MockupEdgeDetailsP extends JPanel implements MockupDetails {

        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        ArrayList<MockupMarkupDetailsP> markups = new ArrayList<MockupMarkupDetailsP>();
        JTextField lastTF;
        LastTfListener tfListener = new LastTfListener();

        public MockupEdgeDetailsP(ArrayList<String> mockupTokenRequirements) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));
            add(new JLabel("Token Requirements"));
            for (String ie : mockupTokenRequirements) {
                JTextField tf = new JTextField(ie);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
        }

        public ArrayList<String> getTokenRequirements() {
            ArrayList<String> mockupTokenRequirements = new ArrayList<String>();
            for (JTextField tf : tfs) {
                if (!tf.getText().isEmpty()) {
                    mockupTokenRequirements.add(tf.getText());
                }
            }
            return mockupTokenRequirements;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            revalidate();
        }
    }

    class MockupIeDetailsP extends JPanel implements MockupDetails {

        ArrayList<JComboBox> cbs = new ArrayList<JComboBox>();
        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        ArrayList<MockupMarkupDetailsP> markups = new ArrayList<MockupMarkupDetailsP>();
        JComboBox lastCB;
        JTextField lastTF;
        LastCbListener cbListener = new LastCbListener();
        LastTfListener tfListener = new LastTfListener();

        public MockupIeDetailsP(Hashtable<String, MockupTransition.MockupIeStatus> mockupInputEventStatus, Hashtable<String, ArrayList<String>> mockupInputEventMarkups) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));
            add(new JLabel("Input Events and Markup"));
            for (String ie : mockupInputEventStatus.keySet()) {
                JTextField tf = new JTextField(ie);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
                MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(mockupInputEventMarkups.get(ie));
                markups.add(markupDetailsP);
                add(markupDetailsP);
                JComboBox cb = new JComboBox(MockupIeStatus.values());
                cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
                cb.setSelectedItem(mockupInputEventStatus.get(ie));
                cbs.add(cb);
                add(cb);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(new ArrayList<String>());
            markups.add(markupDetailsP);
            add(markupDetailsP);
            JComboBox cb = new JComboBox(MockupIeStatus.values());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
            cbs.add(cb);
            add(cb);
            lastCB = cb;
            lastCB.addActionListener(cbListener);
        }

        public Hashtable<String, MockupIeStatus> getInputEventStatus() {
            Hashtable<String, MockupIeStatus> inputEventStatus = new Hashtable<String, MockupIeStatus>();
            int size = Math.min(tfs.size(), cbs.size());
            for (int i = 0; i < size; i++) {
                if (!tfs.get(i).getText().isEmpty()) {
                    inputEventStatus.put(tfs.get(i).getText(), (MockupIeStatus) cbs.get(i).getSelectedItem());
                }
            }
            return inputEventStatus;
        }

        public Hashtable<String, ArrayList<String>> getInputEventMarkup() {
            Hashtable<String, ArrayList<String>> inputEventMarkup = new Hashtable<String, ArrayList<String>>();
            int size = Math.min(tfs.size(), markups.size());
            for (int i = 0; i < size; i++) {
                if (!tfs.get(i).getText().isEmpty()) {
                    inputEventMarkup.put(tfs.get(i).getText(), markups.get(i).getMarkups());
                }
            }
            return inputEventMarkup;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(new ArrayList<String>());
            add(markupDetailsP);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            JComboBox cb = new JComboBox(MockupIeStatus.values());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
            cbs.add(cb);
            add(cb);
            lastCB.removeActionListener(cbListener);
            lastCB = cb;
            lastCB.addActionListener(cbListener);
            revalidate();
        }
    }

    class MockupOeDetailsP extends JPanel implements MockupDetails {

        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        ArrayList<MockupMarkupDetailsP> markups = new ArrayList<MockupMarkupDetailsP>();
        JTextField lastTF;
        LastTfListener tfListener = new LastTfListener();

        public MockupOeDetailsP(Hashtable<String, ArrayList<String>> mockupOutputEventMarkups) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));
            add(new JLabel("Output Events and Markup"));
            for (String oe : mockupOutputEventMarkups.keySet()) {
                JTextField tf = new JTextField(oe);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
                MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(mockupOutputEventMarkups.get(oe));
                markups.add(markupDetailsP);
                add(markupDetailsP);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(new ArrayList<String>());
            markups.add(markupDetailsP);
            add(markupDetailsP);
        }

        public Hashtable<String, ArrayList<String>> getOutputEvents() {
            Hashtable<String, ArrayList<String>> outputEvents = new Hashtable<String, ArrayList<String>>();
            int size = Math.min(tfs.size(), markups.size());
            for (int i = 0; i < size; i++) {
                if (!tfs.get(i).getText().isEmpty()) {
                    outputEvents.put(tfs.get(i).getText(), markups.get(i).getMarkups());
                }
            }
            return outputEvents;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            MockupMarkupDetailsP markupDetailsP = new MockupMarkupDetailsP(new ArrayList<String>());
            add(markupDetailsP);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            revalidate();
        }
    }

    class MockupMarkupDetailsP extends JPanel implements MockupDetails {

        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        JTextField lastTF;
        LastTfListener tfListener = new LastTfListener();

        public MockupMarkupDetailsP(ArrayList<String> mockupMarkups) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));

            for (String oe : mockupMarkups) {
                JTextField tf = new JTextField(oe);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
        }

        public ArrayList<String> getMarkups() {
            ArrayList<String> markups = new ArrayList<String>();
            for (JTextField tf : tfs) {
                if (!tf.getText().isEmpty()) {
                    markups.add(tf.getText());
                }
            }
            return markups;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            revalidate();
        }
    }

    class MockupTokenDetailsP extends JPanel implements MockupDetails {

        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        JTextField lastTF;
        LastTfListener tfListener = new LastTfListener();

        public MockupTokenDetailsP(ArrayList<String> mockupTokens) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));

            add(new JLabel("Tokens"));
            for (String token : mockupTokens) {
                JTextField tf = new JTextField(token);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
        }

        public ArrayList<String> getTokens() {
            ArrayList<String> tokens = new ArrayList<String>();
            for (JTextField tf : tfs) {
                if (!tf.getText().isEmpty()) {
                    tokens.add(tf.getText());
                }
            }
            return tokens;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            revalidate();
        }
    }

    class MockupSMDetailsP extends JPanel implements MockupDetails {

        ArrayList<JComboBox> cbs = new ArrayList<JComboBox>();
        ArrayList<JTextField> tfs = new ArrayList<JTextField>();
        JComboBox lastCB;
        JTextField lastTF;
        LastCbListener cbListener = new LastCbListener();
        LastTfListener tfListener = new LastTfListener();

        public MockupSMDetailsP(Hashtable<String, MockupPlace.MockupSubMissionType> mockupSubMissionStatus) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border border = BorderFactory.createLineBorder(Color.black, 1);
            Border margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            setBorder(new CompoundBorder(border, margin));

            add(new JLabel("Sub-missions"));

            for (String sm : mockupSubMissionStatus.keySet()) {
                JTextField tf = new JTextField(sm);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
                tfs.add(tf);
                add(tf);
                JComboBox cb = new JComboBox(MockupPlace.MockupSubMissionType.values());
                cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
                cb.setSelectedItem(mockupSubMissionStatus.get(sm));
                cbs.add(cb);
                add(cb);
            }
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            JComboBox cb = new JComboBox(MockupPlace.MockupSubMissionType.values());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
            cbs.add(cb);
            add(cb);
            lastCB = cb;
            lastCB.addActionListener(cbListener);
        }

        public Hashtable<String, MockupSubMissionType> getSubmissionStatus() {
            Hashtable<String, MockupSubMissionType> submissionStatus = new Hashtable<String, MockupSubMissionType>();
            int size = Math.min(tfs.size(), cbs.size());
            for (int i = 0; i < size; i++) {
                if (!tfs.get(i).getText().isEmpty()) {
                    submissionStatus.put(tfs.get(i).getText(), (MockupSubMissionType) cbs.get(i).getSelectedItem());
                }
            }
            return submissionStatus;
        }

        @Override
        public void addItem() {
            JTextField tf = new JTextField();
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, tf.getPreferredSize().height));
            tfs.add(tf);
            add(tf);
            lastTF.removeFocusListener(tfListener);
            lastTF.removeKeyListener(tfListener);
            lastTF.removeMouseListener(tfListener);
            lastTF = tf;
            lastTF.addFocusListener(tfListener);
            lastTF.addKeyListener(tfListener);
            lastTF.addMouseListener(tfListener);
            JComboBox cb = new JComboBox(MockupPlace.MockupSubMissionType.values());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, cb.getPreferredSize().height));
            cbs.add(cb);
            add(cb);
            lastCB.removeActionListener(cbListener);
            lastCB = cb;
            lastCB.addActionListener(cbListener);
            revalidate();
        }
    }

    private class LastCbListener implements ActionListener {

        public LastCbListener() {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (ae.getSource() instanceof JComponent
                    && ((JComponent) ae.getSource()).getParent() instanceof MockupDetails) {
                ((MockupDetails) ((JComponent) ae.getSource()).getParent()).addItem();
            }
        }
    }

    class LastTfListener implements KeyListener, FocusListener, MouseListener {

        @Override
        public void keyTyped(KeyEvent ke) {
            checkValidity(ke.getSource());
        }

        @Override
        public void keyPressed(KeyEvent ke) {
        }

        @Override
        public void keyReleased(KeyEvent ke) {
        }

        @Override
        public void focusGained(FocusEvent fe) {
            checkValidity(fe.getSource());
        }

        @Override
        public void focusLost(FocusEvent fe) {
            checkValidity(fe.getSource());
        }

        @Override
        public void mouseClicked(MouseEvent me) {
        }

        @Override
        public void mousePressed(MouseEvent me) {
        }

        @Override
        public void mouseReleased(MouseEvent me) {
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            checkValidity(me.getSource());
        }

        @Override
        public void mouseExited(MouseEvent me) {
            checkValidity(me.getSource());
        }

        private void checkValidity(Object source) {
            if (source instanceof JComponent
                    && ((JComponent) source).getParent() instanceof MockupDetails) {
                if (source instanceof JTextField
                        && ((JTextField) source).getText().isEmpty()) {
                    return;
                }
                ((MockupDetails) ((JComponent) source).getParent()).addItem();
            }
        }
    }

    public static void main(String[] args) {

        ArrayList<String> mockupOutputEvents = new ArrayList<String>();
        mockupOutputEvents.add("OE1");
        Hashtable<String, ArrayList<String>> mockupOutputEventMarkups = new Hashtable<String, ArrayList<String>>();
        ArrayList<String> markups = new ArrayList<String>();
        markups.add("M1");
        mockupOutputEventMarkups.put("OE1", markups);
        ArrayList<String> mockupTokens = new ArrayList<String>();
        mockupTokens.add("t1");
        ArrayList<String> mockupSubMissions = new ArrayList<String>();
        mockupSubMissions.add("SM1");
        Hashtable<String, MockupPlace.MockupSubMissionType> mockupSubMissionStatus = new Hashtable<String, MockupPlace.MockupSubMissionType>();
        mockupSubMissionStatus.put("SM1", MockupSubMissionType.INCOMPLETE);

        MockupPlace place = new MockupPlace("test");
//        place.setMockupIsActive(false);
        place.setMockupOutputEventMarkups(mockupOutputEventMarkups);
//        place.setMockupOutputEvents(mockupOutputEvents);
        place.setMockupSubMissionType(mockupSubMissionStatus);
//        place.setMockupSubMissions(mockupSubMissions);
        place.setMockupTokens(mockupTokens);

        MockupDetailsD d = new MockupDetailsD(null, true, place);
        d.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        d.setVisible(true);
    }
}
