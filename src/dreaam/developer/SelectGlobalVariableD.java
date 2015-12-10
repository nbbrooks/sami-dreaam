package dreaam.developer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Dialogue for adding, removing, and modifying global variables
 *
 * @author nbb
 */
public class SelectGlobalVariableD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectGlobalVariableD.class.getName());
    private HashMap<String, Object> existingVariables;
    private LinkedHashMap<String, Object> variables;

    private javax.swing.JButton newB, okB, cancelB;

    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Layout
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    private JScrollPane existingVariablesSP;
    private JPanel existingVariablesP;

    public SelectGlobalVariableD(java.awt.Frame parent, boolean modal, HashMap<String, Object> existingVariables) {
        super(parent, modal);
        this.existingVariables = existingVariables;
        if (existingVariables == null) {
            this.existingVariables = new HashMap<String, Object>();
            this.variables = new LinkedHashMap<String, Object>();
        } else {
            this.variables = new LinkedHashMap<String, Object>();
            ArrayList<String> variablesNames = new ArrayList<String>(existingVariables.keySet());
            Collections.sort(variablesNames,
                    new Comparator<String>() {
                        public int compare(String f1, String s2) {
                            return f1.compareTo(s2);
                        }
                    });
            for (String variableName : variablesNames) {
                variables.put(variableName, existingVariables.get(variableName));
            }
        }
        initComponents();
        setTitle("SelectGlobalVariableD");
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        existingVariablesP = new JPanel();
        existingVariablesP.setLayout(new BoxLayout(existingVariablesP, BoxLayout.Y_AXIS));

        for (String variable : variables.keySet()) {
            VariableP globalVariableP = new VariableP(variable);
            existingVariablesP.add(globalVariableP);
        }

        existingVariablesSP = new JScrollPane();
        existingVariablesSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        existingVariablesSP.setViewportView(existingVariablesP);

        JPanel buttonsP = new JPanel(new GridBagLayout());
        GridBagConstraints buttonsConstraints = new GridBagConstraints();
        buttonsConstraints.gridx = 0;
        buttonsConstraints.gridy = 0;
        buttonsConstraints.fill = GridBagConstraints.BOTH;
        buttonsConstraints.weightx = 1.0;
        buttonsConstraints.weighty = 1.0;

        newB = new javax.swing.JButton();
        newB.setText("Add New");
        newB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newBActionPerformed(evt);
            }
        });
        newB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        buttonsP.add(newB, buttonsConstraints);
        buttonsConstraints.gridy = buttonsConstraints.gridy + 1;

        okB = new javax.swing.JButton();
        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });
        okB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        buttonsP.add(okB, buttonsConstraints);
        buttonsConstraints.gridy = buttonsConstraints.gridy + 1;

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        cancelB.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        buttonsP.add(cancelB, buttonsConstraints);
        buttonsConstraints.gridy = buttonsConstraints.gridy + 1;

        getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;

        getContentPane().add(existingVariablesSP, constraints);
        constraints.gridy = constraints.gridy + 1;
        getContentPane().add(buttonsP, constraints);
        constraints.gridy = constraints.gridy + 1;

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenHeight = gd.getDisplayMode().getHeight();
        setPreferredSize(new Dimension(getPreferredSize().width, (int) (screenHeight * 0.9)));

        pack();
    }

    private void newBActionPerformed(java.awt.event.ActionEvent evt) {
        EditGlobalVariableD variableD = new EditGlobalVariableD(null, true);
        variableD.setVisible(true);

        if (variableD.confirmedExit()) {
            String variable = variableD.getVariableName();
            if (variable.length() > 0 && !variable.startsWith("@")) {
                variable = "@" + variable;
            }
            if (variable.length() > 1 && variable.startsWith("@")) {
                variables.put(variable, variableD.getVariableValue());
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
        existingVariablesP.removeAll();
        for (String variable : variables.keySet()) {
            VariableP variableP = new VariableP(variable);
            existingVariablesP.add(variableP);
        }
        validate();
        repaint();
    }

    class VariableP extends JPanel {

        String variableName;
        JButton modifyB, deleteB;

        public VariableP(String variable) {
            this.variableName = variable;
            initComponents();
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            JPanel buttonsP = new JPanel(new BorderLayout());
            modifyB = new JButton("Edit");
            modifyB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    String variableOld = variableName;
                    EditGlobalVariableD variableD = new EditGlobalVariableD(null, true, variableName, variables.get(variableName));
                    variableD.setVisible(true);

                    if (variableD.confirmedExit()) {
                        // Remove old entry
                        variables.remove(variableOld);
                        variables.put(variableD.getVariableName(), variableD.getVariableValue());
                        refreshVariableP();
                    }
                }
            });

            deleteB = new JButton("Delete");
            deleteB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    variables.remove(variableName);
                    refreshVariableP();
                }
            });

            add(new JLabel(variableName), BorderLayout.WEST);
            buttonsP.add(modifyB, BorderLayout.WEST);
            buttonsP.add(deleteB, BorderLayout.EAST);
            add(buttonsP, BorderLayout.EAST);

            pack();
        }
    }
}
