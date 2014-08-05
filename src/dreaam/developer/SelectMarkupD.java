package dreaam.developer;

import sami.markup.Markup;
import sami.config.DomainConfigManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import sami.config.DomainConfig.LeafNode;
import sami.markup.ReflectedMarkupSpecification;
import sami.mission.MissionPlanSpecification;

/**
 *
 * @author pscerri
 */
public class SelectMarkupD extends javax.swing.JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(SelectMarkupD.class.getName());
    DefaultMutableTreeNode treeRoot, existingMarkupsNode;
    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
    // This insanity is required because CheckBox node is replacing the event spec with a string.
    private Hashtable<ToolTipTreeNode, ReflectedMarkupSpecification> nodeMapping = new Hashtable<ToolTipTreeNode, ReflectedMarkupSpecification>();
    // This contains the events that are selected for caller class to pull out
    ArrayList<ReflectedMarkupSpecification> existingMarkupSpecs;
    MissionPlanSpecification mSpec;
    
    public SelectMarkupD(java.awt.Frame parent, boolean modal, ArrayList<ReflectedMarkupSpecification> existingMarkupSpecs, MissionPlanSpecification mSpec) {
        super(parent, modal);
        initComponents();
        setTitle("SelectMarkupD");
        this.existingMarkupSpecs = existingMarkupSpecs;
        this.mSpec = mSpec;
        
        eventT.setCellRenderer(renderer);
        eventT.setCellEditor(new CheckBoxNodeEditor(eventT));
        treeRoot = new javax.swing.tree.DefaultMutableTreeNode("JTree");

        // Work out markups from file
        if (this.existingMarkupSpecs != null && this.existingMarkupSpecs.size() > 0) {
            existingMarkupsNode = new DefaultMutableTreeNode("Existing events");
            treeRoot.add(existingMarkupsNode);
            for (ReflectedMarkupSpecification eventSpec : this.existingMarkupSpecs) {
                addExistingMarkup(eventSpec, "", existingMarkupsNode);
            }
        }
        addDomainMarkups(treeRoot);

        // Expand the tree for easier viewing
        eventT.setModel(new javax.swing.tree.DefaultTreeModel(treeRoot));
        for (int i = 0; i < eventT.getRowCount(); i++) {
            eventT.expandRow(i);
        }
        
        ToolTipManager.sharedInstance().registerComponent(eventT);
    }
    
    public void addExistingMarkup(ReflectedMarkupSpecification markupSpec, String toolText, DefaultMutableTreeNode parentNode) {
        // Store spec in a ToolTipTreeNode, which is stored in the tree and a lookup table
        ToolTipTreeNode node = new ToolTipTreeNode(markupSpec, toolText);
        // Make node checked
        nodeMapping.put(node, markupSpec);
        parentNode.add(node);
    }
    
    public void addDomainMarkups(DefaultMutableTreeNode treeRoot) {
        DefaultMutableTreeNode eventTree = (DefaultMutableTreeNode) DomainConfigManager.getInstance().domainConfiguration.markupTree;
        for (int i = 0; i < eventTree.getChildCount(); i++) {
            addNode(eventTree.getChildAt(i), treeRoot);
        }
    }
    
    public void addNode(TreeNode aliasNode, DefaultMutableTreeNode parent) {
        if (aliasNode instanceof LeafNode) {
            // At a event, add as CheckBox and return
            LeafNode leafNode = (LeafNode) aliasNode;
            addEmptyMarkup(leafNode.className, leafNode.displayName, leafNode.detailedDescription, parent);
        } else if (aliasNode instanceof DefaultMutableTreeNode) {
            // At a category, add and recurse
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(aliasNode.toString());
            parent.add(categoryNode);
            for (int i = 0; i < aliasNode.getChildCount(); i++) {
                addNode(aliasNode.getChildAt(i), categoryNode);
            }
            if (categoryNode.getChildCount() == 0) {
                categoryNode.removeFromParent();
            }
        } else {
            LOGGER.severe("Could not handle TreeNode: " + aliasNode + ": of class: " + aliasNode.getClass());
        }
    }
    
    private void addEmptyMarkup(String className, String displayName, String toolText, DefaultMutableTreeNode parentNode) {
        try {
            if (!Markup.class.isAssignableFrom(Class.forName(className))) {
                // Don't add this to the menu
                return;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unable to work out class for " + className);
            ex.printStackTrace();
        }
        
        ReflectedMarkupSpecification markupSpec = new ReflectedMarkupSpecification(className);

        // Store spec in a ToolTipTreeNode, which is stored in the tree and a lookup table
        ToolTipTreeNode node = new ToolTipTreeNode(markupSpec, toolText);
        nodeMapping.put(node, markupSpec);
        parentNode.add(node);
    }
    
    public Markup instanceExists(String className, ArrayList<Markup> selectedMarkups) {
        for (Markup markup : selectedMarkups) {
            if (markup.getClass().getName().equals(className)) {
                return markup;
            }
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        eventT = new javax.swing.JTree();
        okB = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("JTree");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Service");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("SAMI");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Proxy");
        treeNode1.add(treeNode2);
        eventT.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        eventT.setEditable(true);
        eventT.setRootVisible(false);
        jScrollPane1.setViewportView(eventT);

        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, okB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 98, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 523, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(okB)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBActionPerformed
        ArrayList<ReflectedMarkupSpecification> newSelectedMarkups = new ArrayList<ReflectedMarkupSpecification>();
        
        Enumeration e = treeRoot.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            if (n.isLeaf()) {
                
                Object userObject = n.getUserObject();
                // Turns into a CheckBoxNode if touched
                // Get values for markup for each event that was passed into SelectMarkupD
                if (userObject instanceof CheckBoxNode) {
                    CheckBoxNode node = (CheckBoxNode) userObject;
                    boolean isSelected = node.isSelected();
                    if (isSelected) {
                        // Add the spec to our new list
                        newSelectedMarkups.add(nodeMapping.get(n));
                        ReflectedMarkupSpecification eventSpec = nodeMapping.get(n);
                        getDefinitionsForMarkup(eventSpec);
                    }
                    // Event was previously selected, need 
                } else if (userObject instanceof ReflectedMarkupSpecification) {
                    // Check if the spec in the tree (added by simpleAdd) is in the spec list
                    ReflectedMarkupSpecification markupSpec = (ReflectedMarkupSpecification) userObject;
                    if (existingMarkupSpecs != null && existingMarkupSpecs.contains(markupSpec)) {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Was previously selected, readding to events list!");
                        newSelectedMarkups.add(markupSpec);
                        getDefinitionsForMarkup(markupSpec);
                    }
                }
            }
        }
        existingMarkupSpecs = newSelectedMarkups;
        setVisible(false);
    }
    
    public ArrayList<ReflectedMarkupSpecification> getSelectedMarkupSpecs() {
        return existingMarkupSpecs;
    }
    
    public void getDefinitionsForMarkup(ReflectedMarkupSpecification markupSpec) {
        ReflectedMarkupD diag = new ReflectedMarkupD(null, true, markupSpec, mSpec);
        diag.setVisible(true);
    }//GEN-LAST:event_okBActionPerformed

    // Adapted from http://www.java2s.com/Code/Java/Swing-JFC/CheckBoxNodeTreeSample.htm
    class CheckBoxNodeRenderer implements TreeCellRenderer {
        
        private JCheckBox leafRenderer = new JCheckBox();
        private DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
        Color selectionBorderColor, selectionForeground, selectionBackground,
                textForeground, textBackground;
        
        protected JCheckBox getLeafRenderer() {
            return leafRenderer;
        }
        
        public CheckBoxNodeRenderer() {
            
            Font fontValue;
            fontValue = UIManager.getFont("Tree.font");
            if (fontValue != null) {
                leafRenderer.setFont(fontValue);
            }
            Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
            leafRenderer.setFocusPainted((booleanValue != null) && (booleanValue.booleanValue()));
            
            selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
            selectionForeground = UIManager.getColor("Tree.selectionForeground");
            selectionBackground = UIManager.getColor("Tree.selectionBackground");
            textForeground = UIManager.getColor("Tree.textForeground");
            textBackground = UIManager.getColor("Tree.textBackground");
            
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            
            Component returnValue;
            if (leaf) {
                // Add an markup class checkbox
                // This is the text that will appear before the "checkbox" is manipulated which causes an actual CheckBoxNode to be created from the ToolTipTreeNode...I think
                leafRenderer.setText(value.toString());
                if (nodeMapping.containsKey(value)) {
                    ReflectedMarkupSpecification eventSpec = nodeMapping.get(value);
                    if (existingMarkupSpecs != null && existingMarkupSpecs.contains(eventSpec)) {
                        leafRenderer.setSelected(true);
                    } else {
                        leafRenderer.setSelected(false);
                    }
                }
                leafRenderer.setEnabled(tree.isEnabled());
                
                if ((value != null) && (value instanceof ToolTipTreeNode)) {
                    ToolTipTreeNode tttn = (ToolTipTreeNode) value;
                    leafRenderer.setToolTipText(tttn.getToolTipText());
                    leafRenderer.setBorder(new LineBorder(Color.BLACK));
                    Object userObject = tttn.getUserObject();
                    if (userObject instanceof CheckBoxNode) {
                        // The object will be a CheckBoxNode if the "checkbox" has been manipulated previously...as opposed to it being a ReflectedEventSpecification as expected
                        CheckBoxNode node = (CheckBoxNode) userObject;
                        leafRenderer.setText(node.getText());
                        leafRenderer.setSelected(node.isSelected());
                        // System.out.println("This was called, with " + node.isSelected());
                    }
                }
                
                if (selected) {
                    leafRenderer.setForeground(selectionForeground);
                    leafRenderer.setBackground(selectionBackground);
                } else {
                    leafRenderer.setForeground(textForeground);
                    leafRenderer.setBackground(textBackground);
                }
                
                returnValue = leafRenderer;
            } else {
                returnValue = nonLeafRenderer.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
            }
            return returnValue;
        }
    }
    
    class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
        
        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
        ChangeEvent changeEvent = null;
        JTree tree;
        
        public CheckBoxNodeEditor(JTree tree) {
            this.tree = tree;
        }
        
        public Object getCellEditorValue() {
            JCheckBox checkbox = renderer.getLeafRenderer();
            CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(), checkbox.isSelected());
            return checkBoxNode;
        }
        
        public boolean isCellEditable(EventObject event) {
            boolean returnValue = false;
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                TreePath path = tree.getPathForLocation(mouseEvent.getX(),
                        mouseEvent.getY());
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if ((node != null) && (node instanceof ToolTipTreeNode)) {
                        ToolTipTreeNode treeNode = (ToolTipTreeNode) node;
                        returnValue = treeNode.isLeaf();
                    }
                }
            }
            return returnValue;
        }
        
        public Component getTreeCellEditorComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row) {
            
            Component editor = renderer.getTreeCellRendererComponent(tree, value,
                    true, expanded, leaf, row, true);

            // editor always selected / focused
            ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (stopCellEditing()) {
                        fireEditingStopped();
                    }
                }
            };
            if (editor instanceof JCheckBox) {
                ((JCheckBox) editor).addItemListener(itemListener);
            }
            
            return editor;
        }
    }
    
    class CheckBoxNode {
        
        String text;
        boolean selected;
        
        public CheckBoxNode(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public void setSelected(boolean newValue) {
            selected = newValue;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String newValue) {
            text = newValue;
        }
        
        public String toString() {
            return getClass().getName() + "[" + text + "/" + selected + "]";
        }
    }
    
    class ToolTipTreeNode extends DefaultMutableTreeNode {
        
        private String toolTipText;
        
        public ToolTipTreeNode(Object obj, String toolTipText) {
            super(obj);
            this.toolTipText = toolTipText;
            
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Got object of type: " + obj.getClass(), this);
        }
        
        public String getToolTipText() {
            return toolTipText;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                SelectMarkupD dialog = new SelectMarkupD(new javax.swing.JFrame(), true, null, new MissionPlanSpecification("Anon"));
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree eventT;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okB;
    // End of variables declaration//GEN-END:variables
}
