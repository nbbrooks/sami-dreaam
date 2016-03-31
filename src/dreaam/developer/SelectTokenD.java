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

                System.out.println("### selTokenReq " + selTokenReq.getClass().getSimpleName() + " \t" + selTokenReq.toString());

                // Don't add in item listener yet or we'll get a bunch of extra empty boxes 
                ReqSelPanel reqPanel = addReqPanel(selTokenReq);
//                matchCriteriaListener.refreshCBOptions(reqPanel);
            }
        }

        // Add a new combo box for selecting the next Proxy (if necessary)
        addReqPanel();
    }

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public ReqSelPanel addReqPanel() {
        return addReqPanel(null, panel.getComponentCount() - 1);
    }

    public ReqSelPanel addReqPanel(int index) {
        return addReqPanel(null, index);
    }

    public ReqSelPanel addReqPanel(TokenRequirement value) {
        return addReqPanel(value, panel.getComponentCount() - 1);
    }

    public ReqSelPanel addReqPanel(TokenRequirement value, int index) {
        ReqSelPanel reqPanel = new ReqSelPanel(edgeType, value);
        reqPanels.add(reqPanel);
        layout.setRows(layout.getRows() + 1);
        panel.add(reqPanel, index);
        return reqPanel;
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

            if (reqPanel.criteriaCB.getSelectedItem() != null 
                    && reqPanel.criteriaCB.getSelectedItem() != "") {
                matchCriteria = (MatchCriteria) reqPanel.criteriaCB.getSelectedItem();
                if (matchCriteria == MatchCriteria.SpecificTask) {
                    if (reqPanel.specificTaskCB.getSelectedItem() != null && reqPanel.specificTaskCB.getSelectedItem() != "") {
                        specificTaskName = reqPanel.specificTaskCB.getSelectedItem().toString();
                    } else {
                        LOGGER.warning("No task name specified for MatchCriteria SpecificTask");
                        continue;
                    }
                }
            } else {
                LOGGER.warning("No criteria specified for MatchCriteria");
                continue;
            }
            if ((edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery)
                    && matchCriteria != MatchCriteria.None
                    && reqPanel.actionCB.getSelectedItem() != null
                    && reqPanel.actionCB.getSelectedItem() != "") {
                matchAction = (MatchAction) reqPanel.actionCB.getSelectedItem();
            } else if ((edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery)
                    && matchCriteria != MatchCriteria.None) {
                LOGGER.warning("No action specified for MatchAction");
                continue;
            }
            if (reqPanel.quantityCB.getSelectedItem() != "") {
                matchQuantity = (MatchQuantity) reqPanel.quantityCB.getSelectedItem();
                if (matchQuantity == MatchQuantity.Number
                        || matchQuantity == MatchQuantity.GreaterThanEqualTo
                        || matchQuantity == MatchQuantity.LessThan) {
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
            if (matchQuantity != MatchQuantity.Number
                    && matchQuantity != MatchQuantity.GreaterThanEqualTo
                    && matchQuantity != MatchQuantity.LessThan) {
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
                    if (quantityCombo.getSelectedItem() == MatchQuantity.Number
                            || quantityCombo.getSelectedItem() == MatchQuantity.GreaterThanEqualTo
                            || quantityCombo.getSelectedItem() == MatchQuantity.LessThan) {
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
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    refreshCBOptions(reqP);
                }
            } else {

            }
        }

        public void refreshCBOptions(ReqSelPanel reqP) {
            // actionCB visibility
            if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.None
                    || reqP.criteriaCB.getSelectedItem() == "") {
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

            // specificTaskCB visibility
            if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.SpecificTask) {
                // Show specificTaskCB
                reqP.specificTaskCB.setVisible(true);
            } else {
                // Hide specificTaskCB
                reqP.specificTaskCB.setVisible(false);
            }

            // criteriaCombo options
            if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.SubMissionToken) {
                // Only use "Add" action
            }

            // quantityCB options
            if (edgeType == EdgeType.IncomingNominal || edgeType == EdgeType.IncomingRecovery) {
                if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.RelevantToken) {
                    // In edge and RT selected
                    reqP.quantityCB.removeAllItems();
                    reqP.quantityCB.addItem("");
                    reqP.quantityCB.addItem(MatchQuantity.None);
                    reqP.quantityCB.addItem(MatchQuantity.All);
                    reqP.quantityCB.addItem(MatchQuantity.LessThan);
                    reqP.quantityCB.addItem(MatchQuantity.GreaterThanEqualTo);
                } else {
                    // In edge
                    reqP.quantityCB.removeAllItems();
                    reqP.quantityCB.addItem("");
                    reqP.quantityCB.addItem(MatchQuantity.None);
                    reqP.quantityCB.addItem(MatchQuantity.LessThan);
                    reqP.quantityCB.addItem(MatchQuantity.GreaterThanEqualTo);
                }
            } else if (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery) {
                if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.SubMissionToken) {
                    // Out edge and SMT selected
                    reqP.quantityCB.removeAllItems();
                    reqP.quantityCB.addItem("");
                    reqP.quantityCB.addItem(MatchQuantity.All);
                } else {
                    // Out edge
                    reqP.quantityCB.removeAllItems();
                    reqP.quantityCB.addItem("");
                    reqP.quantityCB.addItem(MatchQuantity.All);
                    reqP.quantityCB.addItem(MatchQuantity.Number);
                }
            } else {
                LOGGER.severe("Case not handled in setting quantityCB options");
            }

            // actionCB options
            if (reqP.actionCB != null
                    && (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery)) {
                if (reqP.criteriaCB.getSelectedItem() == MatchCriteria.SubMissionToken) {
                    reqP.actionCB.removeAllItems();
                    reqP.actionCB.addItem("");
                    reqP.actionCB.addItem(MatchAction.Add);
                } else {
                    reqP.actionCB.removeAllItems();
                    reqP.actionCB.addItem("");
                    for (MatchAction criteria : MatchAction.values()) {
                        reqP.actionCB.addItem(criteria);
                    }
                }
            }

            panel.revalidate();
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
            criteriaCB.insertItemAt("", 0);
            criteriaCB.addItem(MatchCriteria.AnyProxy);
            criteriaCB.addItem(MatchCriteria.AnyToken);
            criteriaCB.addItem(MatchCriteria.AnyTask);
            criteriaCB.addItem(MatchCriteria.Generic);
            criteriaCB.addItem(MatchCriteria.None);
            criteriaCB.addItem(MatchCriteria.RelevantToken);
            criteriaCB.addItem(MatchCriteria.SpecificTask);
            if (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery) {
                criteriaCB.addItem(MatchCriteria.SubMissionToken);
            }
            add(criteriaCB);
            // specificTaskCB
            specificTaskCB = new JComboBox();
            specificTaskCB.insertItemAt("", 0);
            specificTaskCB.insertItemAt("New task token", 1);
            for (TaskSpecification taskTokenSpec : taskTokenSpecs) {
                specificTaskCB.addItem(taskTokenSpec.getName());
            }
            add(specificTaskCB);
            specificTaskCB.setVisible(false);
            // Quantity
            // quantityCB
            quantityCB = new JComboBox();
            quantityCB.insertItemAt("", 0);
            if (edgeType == EdgeType.IncomingNominal || edgeType == EdgeType.IncomingRecovery) {
                quantityCB.addItem(MatchQuantity.None);
                quantityCB.addItem(MatchQuantity.LessThan);
                quantityCB.addItem(MatchQuantity.GreaterThanEqualTo);
            } else if (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery) {
                quantityCB.addItem(MatchQuantity.All);
                quantityCB.addItem(MatchQuantity.Number);
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
            if (edgeType == EdgeType.OutgoingNominal || edgeType == EdgeType.OutgoingRecovery) {
                actionCB = new JComboBox();
                actionCB.insertItemAt("", 0);
                for (MatchAction criteria : MatchAction.values()) {
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

            // Set the previously selected values
            setSelectedItems(value);
            // Update what options the CB should have based on selected values
            matchCriteriaListener.refreshCBOptions(this);
            // Re-select the previously selected values (refresh resets)
            setSelectedItems(value);
            addListeners();
        }

        public void setSelectedItems(TokenRequirement value) {
            if (value instanceof OutTokenRequirement
                    && ((OutTokenRequirement) value).getMatchAction() != null) {
                actionCB.setSelectedItem(((OutTokenRequirement) value).getMatchAction());
                actionCB.setVisible(true);
            }
            if (value.getMatchCriteria() != null) {
                criteriaCB.setSelectedItem(value.getMatchCriteria());
                criteriaCB.setVisible(true);
                if ((edgeType == EdgeType.IncomingNominal || edgeType == EdgeType.IncomingRecovery)
                        && criteriaCB.getSelectedItem() == MatchCriteria.RelevantToken) {
                    quantityCB.addItem(MatchQuantity.All);
                }
            }
            if (value.getMatchQuantity() != null) {
                quantityCB.setSelectedItem(value.getMatchQuantity());
                quantityCB.setVisible(true);
                if (value.getMatchQuantity() == MatchQuantity.Number
                        || value.getMatchQuantity() == MatchQuantity.GreaterThanEqualTo
                        || value.getMatchQuantity() == MatchQuantity.LessThan) {
                    quantityTF.setText(value.getQuantity() + "");
                    quantityTF.setVisible(true);
                }
            }
            if (value.getTaskName() != null) {
                specificTaskCB.setSelectedItem(value.getTaskName());
                specificTaskCB.setVisible(true);
            }
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
            DefaultMutableTreeNode eventTree = (DefaultMutableTreeNode) DomainConfigManager.getInstance().getDomainConfiguration().taskTree;
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
