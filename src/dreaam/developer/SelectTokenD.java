package dreaam.developer;

import sami.config.DomainConfigManager;
import sami.config.DomainConfig;
import sami.mission.TokenSpecification;
import sami.mission.TokenSpecification.TokenType;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author nbb
 */
public class SelectTokenD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectTokenD.class.getName());
    JPanel panel;
    SelectTokenD.MyItemListener actionListener = new SelectTokenD.MyItemListener();
    private ArrayList<JComboBox> comboBoxes = new ArrayList<JComboBox>();
    private ArrayList<TokenSpecification> usableSpecs;    // Token specs that are legal for the passed in edge
    private ArrayList<TokenSpecification> taskTokenSpecs;    // Task token specs the developer created
    private HashMap<String, String> taskChoices = new HashMap<String, String>();
    private GridLayout layout;

    public SelectTokenD(java.awt.Frame parent, boolean modal, ArrayList<TokenSpecification> selectedTokenSpecs, ArrayList<TokenSpecification> allTokenSpecs, ArrayList<TokenSpecification> taskTokenSpecs) {
        super(parent, modal);
        setTitle("SelectTokenD");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(220, 500);
        panel = new JPanel();
        layout = new GridLayout(1, 1);
        panel.setLayout(layout);
        add(panel);

        this.usableSpecs = allTokenSpecs;
        this.taskTokenSpecs = taskTokenSpecs;
        // Add box for each previously selected Proxy
        if (selectedTokenSpecs != null) {
            for (TokenSpecification selTokenSpec : selectedTokenSpecs) {
                // Don't add in item listener yet or we'll get a bunch of extra empty boxes 
                addNewComboBox(null, panel.getComponentCount());
                comboBoxes.get(comboBoxes.size() - 1).setSelectedItem(selTokenSpec);
            }
            for (JComboBox comboBox : comboBoxes) {
                comboBox.addItemListener(actionListener);
            }
        }
        // Add a new combo box for selecting the next Proxy (if necessary)
        addNewComboBox(actionListener, panel.getComponentCount());

        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });
        panel.add(doneButton);
    }

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public void addNewComboBox(SelectTokenD.MyItemListener actionListener, int index) {
        JComboBox newComboBox = new JComboBox();
        newComboBox.insertItemAt(" ", 0);
        newComboBox.insertItemAt("New task token", 1);
        for (TokenSpecification tokenSpec : usableSpecs) {
            newComboBox.addItem(tokenSpec);
        }
        for (TokenSpecification tokenSpec : taskTokenSpecs) {
            newComboBox.addItem(tokenSpec);
        }
        comboBoxes.add(newComboBox);
        newComboBox.addItemListener(actionListener);
        layout.setRows(layout.getRows() + 1);
        panel.add(newComboBox, index);
    }

    public ArrayList<TokenSpecification> getSelectedTokenSpecs() {
        ArrayList<TokenSpecification> selectedTokenSpecs = new ArrayList<TokenSpecification>();
        for (JComboBox comboBox : comboBoxes) {
            if (comboBox.getSelectedIndex() >= 2 && comboBox.getSelectedItem() instanceof TokenSpecification) {
                selectedTokenSpecs.add((TokenSpecification) comboBox.getSelectedItem());
            }
        }
        return selectedTokenSpecs;
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                TokenSpecification ts0 = new TokenSpecification("Generic token", TokenType.MatchGeneric, null);
                ArrayList<TokenSpecification> selectedTokens = new ArrayList<TokenSpecification>();
                selectedTokens.add(ts0);
                ArrayList<TokenSpecification> allTokens = new ArrayList<TokenSpecification>();
                allTokens.add(ts0);
                new SelectTokenD(null, true, selectedTokens, allTokens, new ArrayList<TokenSpecification>()).setVisible(true);
            }
        });
    }

    private class MyItemListener implements ItemListener {

        public MyItemListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            JComboBox activeCombo = (JComboBox) evt.getSource();
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                // Item was just selected
                if (activeCombo.getSelectedIndex() == 1) {
                    //@todo type check
                    // Get the task this token will represent
                    NewTokenD diag = new NewTokenD(null, true);
                    diag.setVisible(true);

                    boolean alreadyExists = false;
                    for (TokenSpecification token : taskTokenSpecs) {
                        if (token.getName().equalsIgnoreCase(diag.getTokenName())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        // Create a new Proxy with this id and add it to the master list
                        TokenSpecification newTokenSpec = new TokenSpecification(diag.getTokenName(), TokenType.Task, diag.getTaskClass());
                        taskTokenSpecs.add(newTokenSpec);
                        // Add this Proxy to all the combo boxes
                        for (JComboBox combo : comboBoxes) {
                            combo.addItem(newTokenSpec);
                        }
                        activeCombo.setSelectedIndex(activeCombo.getItemCount() - 1);
                    } else {
                        // Proxy already exists, ignore and blank out selection
                        activeCombo.setSelectedItem(null);
                    }
                }
                if (activeCombo == comboBoxes.get(comboBoxes.size() - 1)) {
                    // Add a new combo box above the Done button for selecting the next Proxy
                    addNewComboBox(actionListener, Math.max(0, panel.getComponentCount() - 1));
                    panel.revalidate();
                    validate();
                }
            }
        }
    }

    class NewTokenD extends javax.swing.JDialog {

        private JPanel panel;
        private GridLayout layout;
        private JTextField nameField;
        private JComboBox taskCombo;
        private HashMap<String, String> taskChoices = new HashMap<String, String>();

        public NewTokenD(java.awt.Frame parent, boolean modal) {
            super(parent, modal);
            setTitle("NewTokenD");
            setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());
            setSize(220, 150);
            panel = new JPanel();
            layout = new GridLayout(3, 1);
            panel.setLayout(layout);
            add(panel);

            nameField = new JTextField("Enter new token id here");
            panel.add(nameField);

            // Add tasks in domain configuration to drop down box
            addDomainEvents();
            taskCombo = new JComboBox();
            for (String taskDescription : taskChoices.keySet()) {
                taskCombo.addItem(taskDescription);
            }
            panel.add(taskCombo);

            JButton doneButton = new JButton("Done");
            doneButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    doneActionPerformed(evt);
                }
            });
            panel.add(doneButton);

            panel.revalidate();
            validate();
        }

        public void addDomainEvents() {
            DefaultMutableTreeNode eventTree = (DefaultMutableTreeNode) DomainConfigManager.getInstance().domainConfiguration.taskTree;
            for (int i = 0; i < eventTree.getChildCount(); i++) {
                addNode(eventTree.getChildAt(i));
            }
        }

        public void addNode(TreeNode aliasNode) {
            if (aliasNode instanceof DomainConfig.LeafNode) {
                // At a event, add as CheckBox and return
                DomainConfig.LeafNode leafNode = (DomainConfig.LeafNode) aliasNode;
                taskChoices.put(leafNode.displayName, leafNode.className);
            } else if (aliasNode instanceof DefaultMutableTreeNode) {
                // At a category, add and recurse
                for (int i = 0; i < aliasNode.getChildCount(); i++) {
                    addNode(aliasNode.getChildAt(i));
                }
            } else {
                LOGGER.severe("Could not handle TreeNode: " + aliasNode + ": of class: " + aliasNode.getClass());
            }
        }

        private void doneActionPerformed(java.awt.event.ActionEvent evt) {
            setVisible(false);
        }

        public String getTokenName() {
            return nameField.getText();
        }

        public String getTaskClass() {
            return taskChoices.get(taskCombo.getSelectedItem().toString());
        }
    }
}
