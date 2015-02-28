package dreaam.developer;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Dialog window that lets you select InputEvents and OutputEvents for SAMI
 * transitions
 *
 * @author pscerri
 */
public class SelectGlobalVariableD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectGlobalVariableD.class.getName());
    private HashMap<String, Object> variables, existingVariables;

    private javax.swing.JButton newB, okB, cancelB;

    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Layout
    private GroupLayout layout;
    private GroupLayout.SequentialGroup rowSeqGroup;
    private GroupLayout.ParallelGroup rowParGroup1;
    private GroupLayout.SequentialGroup colSeqGroup;
    private GroupLayout.ParallelGroup[] colParGroupArr;
    private int row;
    private int maxColWidth;
    private int cumulComponentHeight;
    private final static int BUTTON_WIDTH = 100;
    private final static int BUTTON_HEIGHT = 50;

    private JScrollPane mSpecSP;
    private JPanel existingVariableP;

    public SelectGlobalVariableD(java.awt.Frame parent, boolean modal, HashMap<String, Object> existingVariables) {
        super(parent, modal);
        this.existingVariables = existingVariables;
        if (existingVariables == null) {
            existingVariables = new HashMap<String, Object>();
            this.variables = new HashMap<String, Object>();
        } else {
            this.variables = (HashMap<String, Object>) existingVariables.clone();
        }
        initComponents();
        setTitle("SelectGlobalVariableD");
    }

    private void addComponent(JComponent component) {
        rowParGroup1.addComponent(component);
        colParGroupArr[row] = layout.createParallelGroup();
        colParGroupArr[row].addComponent(component);
        maxColWidth = Math.max(maxColWidth, (int) component.getPreferredSize().getWidth() + BUTTON_WIDTH);
        cumulComponentHeight += Math.max((int) component.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        row++;
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        int numRows = 4;
        rowSeqGroup = layout.createSequentialGroup();
        rowParGroup1 = layout.createParallelGroup();
        colSeqGroup = layout.createSequentialGroup();
        colParGroupArr = new GroupLayout.ParallelGroup[numRows];
        row = 0;
        maxColWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        existingVariableP = new JPanel();
        existingVariableP.setLayout(new BoxLayout(existingVariableP, BoxLayout.Y_AXIS));
        for (String variable : variables.keySet()) {
            VariableP mSpecP = new VariableP(variable);
            existingVariableP.add(mSpecP);
        }

        mSpecSP = new JScrollPane();
        mSpecSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mSpecSP.setViewportView(existingVariableP);
        addComponent(mSpecSP);

        newB = new javax.swing.JButton();
        newB.setText("Add New");
        newB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newBActionPerformed(evt);
            }
        });
        addComponent(newB);

        okB = new javax.swing.JButton();
        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });
        addComponent(okB);

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        addComponent(cancelB);

        // Finish layout setup
        layout.setHorizontalGroup(rowSeqGroup
                //                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1, Short.MAX_VALUE) // Spring to right-align
                .addGroup(rowParGroup1));
        for (int i = 0; i < colParGroupArr.length; i++) {
            GroupLayout.ParallelGroup parGroup = colParGroupArr[i];
            colSeqGroup.addGroup(parGroup);
            if (i < colParGroupArr.length - 1) {
                colSeqGroup.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE);
                cumulComponentHeight += 6;
            }
        }
        layout.setVerticalGroup(colSeqGroup);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        maxColWidth = Math.min(maxColWidth, screenWidth);
        cumulComponentHeight = Math.min(cumulComponentHeight, screenHeight);
        setSize(new Dimension(maxColWidth, cumulComponentHeight));
        setPreferredSize(new Dimension(maxColWidth, cumulComponentHeight));

//        setPreferredSize(new Dimension(300, 300));
        validate();
    }

    private void newBActionPerformed(java.awt.event.ActionEvent evt) {
        EditGlobalVariableD variableD = new EditGlobalVariableD(null, true);
        variableD.setVisible(true);

        if (variableD.confirmedExit()) {
            String variable = variableD.getName();
            if (variable.length() > 0 && !variable.startsWith("@")) {
                variable = "@" + variable;
            }
            if (variable.length() > 1 && variable.startsWith("@")) {
                variables.put(variable, variableD.getValue());
            }
            refreshVariableP();
        }
    }

    private void okBActionPerformed(java.awt.event.ActionEvent evt) {
        okExit = true;
        setVisible(false);
    }

    public boolean confirmedExit() {
        return okExit;
    }

    private void cancelBActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    public HashMap<String, Object> getVariables() {
        return variables;
    }

    public HashMap<String, Object> getCreatedVariables() {
        HashMap<String, Object> createdVariables = new HashMap<String, Object>();
        for (String variable : variables.keySet()) {
            if (!existingVariables.containsKey(variable)) {
                createdVariables.put(variable, variables.get(variable));
            }
        }
        return createdVariables;
    }

    public ArrayList<String> getDeletedVariables() {
        ArrayList<String> deletedVariables = new ArrayList<String>();
        for (String variable : existingVariables.keySet()) {
            if (!variables.containsKey(variable)) {
                deletedVariables.add(variable);
            }
        }
        return deletedVariables;
    }

    private void refreshVariableP() {
        existingVariableP.removeAll();
//        existingMSpecP.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (String variable : variables.keySet()) {
            VariableP variableP = new VariableP(variable);
            existingVariableP.add(variableP);
        }
        validate();
        repaint();
    }

    class VariableP extends JPanel {

        String variable;
        JButton modifyB, deleteB;

        public VariableP(String variable) {
            this.variable = variable;
            initComponents();
        }

        private void initComponents() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            modifyB = new JButton("Edit");
            modifyB.setPreferredSize(new Dimension(10, 10));
            modifyB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    String variableOld = variable;
                    EditGlobalVariableD variableD = new EditGlobalVariableD(null, true, variable, variables.get(variable));
                    variableD.setVisible(true);

                    if (variableD.confirmedExit()) {
                        // Remove old entry
                        variables.remove(variableOld);
                        variables.put(variableD.getName(), variableD.getValue());
                        refreshVariableP();
                    }
                }
            });

            deleteB = new JButton("Delete");
            deleteB.setPreferredSize(new Dimension(10, 10));
            deleteB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    variables.remove(variable);
                    refreshVariableP();
                }
            });

            add(new JLabel(variable));
            add(modifyB);
            add(deleteB);

            pack();
        }
    }

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                SelectGlobalVariableD dialog = new SelectGlobalVariableD(new javax.swing.JFrame(), true, null, new ArrayList<String>(), true, true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//            }
//        });
//    }
}
