package dreaam.developer;

import sami.config.DomainConfigManager;
import sami.config.DomainConfig;
import sami.mission.TaskSpecification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import sami.mission.InTokenRequirement;
import sami.mission.OutTokenRequirement;
import sami.mission.TokenRequirement;
import sami.mission.TokenRequirement.MatchAction;
import sami.mission.TokenRequirement.MatchCriteria;
import sami.mission.TokenRequirement.MatchQuantity;

/**
 *
 * @author nbb
 */
public class SelectTokenD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectTokenD.class.getName());

    public enum EdgeType {

        IncomingNominal, IncomingRecovery, OutgoingNominal, OutgoingRecovery
    };
    JPanel panel;
    SelectTokenD.LastItemListener lastItemListener = new SelectTokenD.LastItemListener();
    SelectTokenD.NewTaskListener newTaskListener = new SelectTokenD.NewTaskListener();
    SelectTokenD.MatchQuantityListener matchQuantityListener = new SelectTokenD.MatchQuantityListener();
    SelectTokenD.MatchCriteriaListener matchCriteriaListener = new SelectTokenD.MatchCriteriaListener();
    private ArrayList<ReqSelPanel> reqPanels = new ArrayList<ReqSelPanel>();
    // Task token specs the developer created
    private ArrayList<TaskSpecification> taskTokenSpecs;    
    private EdgeType edgeType;
    private GridLayout layout;
    private ArrayList<TokenRequirement.MatchCriteria> criteriaOptions;
    private ArrayList<TokenRequirement.MatchQuantity> quantityOptions;
    private ArrayList<TokenRequirement.MatchAction> actionOptions;

    static private ArrayList<TokenRequirement.MatchCriteria> inCriteriaOptions, outCriteriaOptions, outRecovCriteriaOptions;
    static private ArrayList<TokenRequirement.MatchQuantity> inQuantityOptions, outQuantityOptions, outRecovQuantityOptions;
    static private ArrayList<TokenRequirement.MatchAction> outActionOptions, outRecovActionOptions;

    static {
        createTokenReqLists();
    }

    public SelectTokenD(java.awt.Frame parent, boolean modal, EdgeType edgeType, ArrayList<? extends TokenRequirement> selectedTokenReqs, ArrayList<TaskSpecification> taskTokenSpecs) {
        super(parent, modal);
        setTitle("SelectTokenD");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(450, 500);
        panel = new JPanel();
        layout = new GridLayout(1, 1);
        panel.setLayout(layout);
        add(panel);

        this.edgeType = edgeType;
        this.taskTokenSpecs = taskTokenSpecs;

        switch (edgeType) {
            case IncomingNominal:
                criteriaOptions = inCriteriaOptions;
                quantityOptions = inQuantityOptions;
                actionOptions = null;
                break;
            case IncomingRecovery:
                criteriaOptions = inCriteriaOptions;
                quantityOptions = inQuantityOptions;
                actionOptions = null;
                break;
            case OutgoingNominal:
                criteriaOptions = outCriteriaOptions;
                quantityOptions = outQuantityOptions;
                actionOptions = outActionOptions;
                break;
            case OutgoingRecovery:
                criteriaOptions = outRecovCriteriaOptions;
                quantityOptions = outRecovQuantityOptions;
                actionOptions = outRecovActionOptions;
                break;
        }

        // Add the done button
        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });
        panel.add(doneButton);

        // Add box for each previously selected Proxy
        if (selectedTokenReqs != null) {
            for (TokenRequirement selTokenReq : selectedTokenReqs) {
                // Don't add in item listener yet or we'll get a bunch of extra empty boxes 
                addReqPanel(selTokenReq);
            }
        }

        // Add a new combo box for selecting the next Proxy (if necessary)
        addReqPanel();
    }

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public void addReqPanel() {
        addReqPanel(null, panel.getComponentCount() - 1);
    }

    public void addReqPanel(int index) {
        addReqPanel(null, index);
    }

    public void addReqPanel(TokenRequirement value) {
        addReqPanel(value, panel.getComponentCount() - 1);
    }

    public void addReqPanel(TokenRequirement value, int index) {
        ReqSelPanel reqPanel = new ReqSelPanel(edgeType, value);
        reqPanels.add(reqPanel);
        layout.setRows(layout.getRows() + 1);
        panel.add(reqPanel, index);
    }

    public ArrayList<TokenRequirement> getSelectedTokenReqs() {
        ArrayList<TokenRequirement> selectedTokenReqs = new ArrayList<TokenRequirement>();

        for (int i = 0; i < reqPanels.size() - 1; i++) {
            ReqSelPanel reqPanel = reqPanels.get(i);
            MatchCriteria matchCriteria = null;
            MatchQuantity matchQuantity = null;
            MatchAction matchAction = null;
            int quantity = -1;
            String specificTaskName = null;

            if (reqPanel.criteriaCB.getSelectedItem() != "") {
                matchCriteria = (MatchCriteria) reqPanel.criteriaCB.getSelectedItem();
                if (matchCriteria == MatchCriteria.SpecificTask) {
                    if (reqPanel.specificTaskCB.getSelectedItem() != null) {
                        specificTaskName = reqPanel.specificTaskCB.getSelectedItem().toString();
                    } else {
                        LOGGER.warning("No task name specified for MatchCriteria SpecificTask");
                        continue;
                    }
                }
            }
            if ((edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery)
                    && matchCriteria != MatchCriteria.None
                    && reqPanel.actionCB.getSelectedItem() != null) {
                matchAction = (MatchAction) reqPanel.actionCB.getSelectedItem();
            } else if ((edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery)
                    && matchCriteria != MatchCriteria.None) {
                LOGGER.warning("No action specified for MatchAction");
                continue;
            }
            if (reqPanel.quantityCB.getSelectedItem() != "") {
                matchQuantity = (MatchQuantity) reqPanel.quantityCB.getSelectedItem();
                if (matchQuantity == matchQuantity.Number) {
                    if (!reqPanel.quantityTF.getText().equals("")) {
                        try {
                            quantity = Integer.parseInt(reqPanel.quantityTF.getText());
                        } catch (NumberFormatException ex) {
                            LOGGER.warning("Could not parse quantity specified for MatchQuantity Number: " + reqPanel.quantityTF.getText());
                            continue;
                        }
                    } else {
                        LOGGER.warning("No quantity specified for MatchQuantity Number");
                        continue;
                    }
                }
            }

            // Blank out values as necessary (may have been entered when they were valid, but then a value was changed making them obsolete
            //  This is just to make the toString() like nice
            if (matchQuantity != MatchQuantity.Number) {
                quantity = -1;
            }
            if (matchCriteria != MatchCriteria.SpecificTask) {
                specificTaskName = null;
            }
            if (matchCriteria == MatchCriteria.None) {
                matchAction = null;
                matchQuantity = null;
                quantity = -1;
                specificTaskName = null;
            }

            if (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery) {
                TokenRequirement req = new OutTokenRequirement(matchCriteria, matchQuantity, matchAction, quantity, specificTaskName);
                selectedTokenReqs.add(req);
            } else if (edgeType == EdgeType.IncomingNominal || edgeType == EdgeType.IncomingRecovery) {
                TokenRequirement req = new InTokenRequirement(matchCriteria, matchQuantity, quantity, specificTaskName);
                selectedTokenReqs.add(req);
            } else {
                LOGGER.severe("Could not construct TokenRequirement");
            }
        }
        return selectedTokenReqs;
    }

    private static void createTokenReqLists() {
        inCriteriaOptions = new ArrayList<TokenRequirement.MatchCriteria>();
        outCriteriaOptions = new ArrayList<TokenRequirement.MatchCriteria>();
        outRecovCriteriaOptions = new ArrayList<TokenRequirement.MatchCriteria>();

        inQuantityOptions = new ArrayList<TokenRequirement.MatchQuantity>();
        outQuantityOptions = new ArrayList<TokenRequirement.MatchQuantity>();
        outRecovQuantityOptions = new ArrayList<TokenRequirement.MatchQuantity>();

        outActionOptions = new ArrayList<TokenRequirement.MatchAction>();
        outRecovActionOptions = new ArrayList<TokenRequirement.MatchAction>();

        // Incoming
        //  Criteria
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyProxy);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyTask);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyToken);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.Generic);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.None);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.RelevantToken);
        inCriteriaOptions.add(TokenRequirement.MatchCriteria.SpecificTask);
        //  Quantities
        inQuantityOptions.add(TokenRequirement.MatchQuantity.None);
        inQuantityOptions.add(TokenRequirement.MatchQuantity.Number);

        // Outgoing
        //  Criteria
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyProxy);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyTask);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyToken);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.Generic);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.None);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.RelevantToken);
        outCriteriaOptions.add(TokenRequirement.MatchCriteria.SpecificTask);
        //  Quantity
        outQuantityOptions.add(TokenRequirement.MatchQuantity.All);
        outQuantityOptions.add(TokenRequirement.MatchQuantity.Number);
        //  Action
        outActionOptions.add(TokenRequirement.MatchAction.Add);
        outActionOptions.add(TokenRequirement.MatchAction.Consume);
        outActionOptions.add(TokenRequirement.MatchAction.Take);

        // Outgoing for recovery mode edges
        //  Criteria
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyProxy);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyTask);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.AnyToken);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.Generic);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.None);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.RelevantToken);
        outRecovCriteriaOptions.add(TokenRequirement.MatchCriteria.SpecificTask);
        //  Quantity
        outRecovQuantityOptions.add(TokenRequirement.MatchQuantity.All);
        outRecovQuantityOptions.add(TokenRequirement.MatchQuantity.Number);
        //  Action
        outRecovActionOptions.add(TokenRequirement.MatchAction.Add);
        outRecovActionOptions.add(TokenRequirement.MatchAction.Consume);
        outRecovActionOptions.add(TokenRequirement.MatchAction.Take);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SelectTokenD(null, true, EdgeType.OutgoingNominal, new ArrayList<TokenRequirement>(), new ArrayList<TaskSpecification>()).setVisible(true);
            }
        });
    }

    private class LastItemListener implements ItemListener {

        public LastItemListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            if (evt.getSource() instanceof JComboBox
                    && ((JComboBox) evt.getSource()).getParent() instanceof ReqSelPanel) {
                ReqSelPanel reqP = (ReqSelPanel) ((JComponent) evt.getSource()).getParent();
                if (reqP == reqPanels.get(reqPanels.size() - 1)) {
                    addReqPanel();
                    panel.revalidate();
                }
            } else {

            }
        }
    }

    private class MatchQuantityListener implements ItemListener {

        public MatchQuantityListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            if (evt.getSource() instanceof JComboBox
                    && ((JComboBox) evt.getSource()).getParent() instanceof ReqSelPanel) {
                ReqSelPanel reqP = (ReqSelPanel) ((JComponent) evt.getSource()).getParent();
                JComboBox quantityCombo = (JComboBox) evt.getSource();
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    // Item was just selected
                    if (quantityCombo.getSelectedItem() == MatchQuantity.Number) {
                        // Show quantityTF
                        reqP.quantityTF.setVisible(true);
                    } else {
                        // Hide quantityTF
                        reqP.quantityTF.setVisible(false);
                    }
                    panel.revalidate();
                }
            } else {

            }
        }
    }

    private class MatchCriteriaListener implements ItemListener {

        public MatchCriteriaListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            if (evt.getSource() instanceof JComboBox
                    && ((JComboBox) evt.getSource()).getParent() instanceof ReqSelPanel) {
                ReqSelPanel reqP = (ReqSelPanel) ((JComponent) evt.getSource()).getParent();
                JComboBox criteriaCombo = (JComboBox) evt.getSource();
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    // Item was just selected
                    if (criteriaCombo.getSelectedItem() == MatchCriteria.None
                            || criteriaCombo.getSelectedItem() == "") {
                        reqP.quantityCB.setVisible(false);
                        if (reqP.actionCB != null) {
                            reqP.actionCB.setVisible(false);
                        }
                    } else {
                        reqP.quantityCB.setVisible(true);
                        if (reqP.actionCB != null) {
                            reqP.actionCB.setVisible(true);
                        }
                    }
                    if (criteriaCombo.getSelectedItem() == MatchCriteria.SpecificTask) {
                        // Show specificTaskCB
                        reqP.specificTaskCB.setVisible(true);
                    } else {
                        // Hide specificTaskCB
                        reqP.specificTaskCB.setVisible(false);
                    }
                    panel.revalidate();
                }
            } else {

            }
        }
    }

    private class NewTaskListener implements ItemListener {

        public NewTaskListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            if (evt.getSource() instanceof JComboBox
                    && ((JComboBox) evt.getSource()).getParent() instanceof ReqSelPanel) {
                JComboBox specificTaskCombo = (JComboBox) evt.getSource();
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    // Item was just selected
                    if (specificTaskCombo.getSelectedIndex() == 1) {
                        NewTaskTokenD diag = new NewTaskTokenD(null, true, taskTokenSpecs);
                        diag.setVisible(true);

                        // Create a new task spec with this id and add it to the master list
                        TaskSpecification newTaskSpec = new TaskSpecification(diag.getTokenName(), diag.getTaskClass());
                        taskTokenSpecs.add(newTaskSpec);
                        // Add this Proxy to all the combo boxes
                        for (ReqSelPanel reqP : reqPanels) {
                            reqP.specificTaskCB.addItem(newTaskSpec);
                        }
                        specificTaskCombo.setSelectedIndex(specificTaskCombo.getItemCount() - 1);
                    }
                    panel.revalidate();
                }
            } else {

            }
        }
    }

    class ReqSelPanel extends JPanel {

        JComboBox criteriaCB, specificTaskCB, quantityCB, actionCB;
        JTextField quantityTF;

        public ReqSelPanel(EdgeType edgeType, boolean addListeners) {
            // Criteria
            // criteriaCB
            criteriaCB = new JComboBox();
            criteriaCB.insertItemAt(" ", 0);
            for (MatchCriteria criteria : criteriaOptions) {
                criteriaCB.addItem(criteria);
            }
            add(criteriaCB);
            // specificTaskCB
            specificTaskCB = new JComboBox();
            specificTaskCB.insertItemAt(" ", 0);
            specificTaskCB.insertItemAt("New task token", 1);
            for (TaskSpecification taskTokenSpec : taskTokenSpecs) {
                specificTaskCB.addItem(taskTokenSpec.getName());
            }
            add(specificTaskCB);
            specificTaskCB.setVisible(false);
            // Quantity
            // quantityCB
            quantityCB = new JComboBox();
            quantityCB.insertItemAt(" ", 0);
            for (MatchQuantity criteria : quantityOptions) {
                quantityCB.addItem(criteria);
            }
            add(quantityCB);
            quantityCB.setVisible(false);
            // quantityTF
            quantityTF = new JTextField("1");
            quantityTF.setPreferredSize(new Dimension(32, quantityTF.getPreferredSize().height));
            add(quantityTF);
            quantityTF.setVisible(false);
            // Action
            // actionCB
            if (actionOptions != null) {
                actionCB = new JComboBox();
                actionCB.insertItemAt(" ", 0);
                for (MatchAction criteria : actionOptions) {
                    actionCB.addItem(criteria);
                }
                add(actionCB);
                actionCB.setVisible(false);
            }
            // Separator
            setBorder(BorderFactory.createLineBorder(Color.BLACK));

            if (addListeners) {
                addListeners();
            }
        }

        public ReqSelPanel(EdgeType edgeType) {
            this(edgeType, true);
        }

        public ReqSelPanel(EdgeType edgeType, TokenRequirement value) {
            this(edgeType, false);
            if (value == null) {
                addListeners();
                return;
            }

            if (actionOptions != null
                    && value instanceof OutTokenRequirement
                    && ((OutTokenRequirement) value).getMatchAction() != null) {
                actionCB.setSelectedItem(((OutTokenRequirement) value).getMatchAction());
                actionCB.setVisible(true);
            }
            if (value.getMatchCriteria() != null) {
                criteriaCB.setSelectedItem(value.getMatchCriteria());
                criteriaCB.setVisible(true);
            }
            if (value.getMatchQuantity() != null) {
                quantityCB.setSelectedItem(value.getMatchQuantity());
                quantityCB.setVisible(true);
                if (value.getMatchQuantity() == MatchQuantity.Number) {
                    quantityTF.setText(value.getQuantity() + "");
                    quantityTF.setVisible(true);
                }
            }
            if (value.getTaskName() != null) {
                specificTaskCB.setSelectedItem(value.getTaskName());
                specificTaskCB.setVisible(true);
            }

            addListeners();
        }

        private void addListeners() {
            criteriaCB.addItemListener(lastItemListener);
            criteriaCB.addItemListener(matchCriteriaListener);
            specificTaskCB.addItemListener(newTaskListener);
            quantityCB.addItemListener(matchQuantityListener);
        }
    }

    class NewTaskTokenD extends javax.swing.JDialog {

        private JPanel panel;
        private GridLayout layout;
        private JTextField nameField;
        private JComboBox taskCombo;
        private HashMap<String, String> taskChoices = new HashMap<String, String>();

        public NewTaskTokenD(java.awt.Frame parent, boolean modal, ArrayList<TaskSpecification> existingTasks) {
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

            JButton doneButton = new JButton("Create Token");
            doneButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    createActionPerformed(evt);
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

        private void createActionPerformed(java.awt.event.ActionEvent evt) {
            String name = getTokenName();
            if (name.length() == 0) {
                JOptionPane.showMessageDialog(null, "Specify a task name");
                return;
            }
            for (TaskSpecification taskSpec : taskTokenSpecs) {
                if (taskSpec.getName().equalsIgnoreCase(name)) {
                    // Already have a task of this name
                    JOptionPane.showMessageDialog(null, "Task with specified name already exists");
                    return;
                }
            }
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
