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
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Dialogue for adding, removing, and modifying global variables
 *
 * @author nbb
 */
public class SelectGlobalVariableD extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(SelectGlobalVariableD.class.getName());
    private HashMap<String, Object> variables, existingVariables;

    private javax.swing.JButton newB, okB, cancelB;

    // OK button used to exit the dialog?
    private boolean okExit = false;

    // Layout
    private int maxComponentWidth;
    private int cumulComponentHeight;
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;

    private JScrollPane existingVariablesSP;
    private JPanel existingVariablesP;

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

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        maxComponentWidth = BUTTON_WIDTH;
        cumulComponentHeight = 0;

        existingVariablesP = new JPanel();
        existingVariablesP.setLayout(new BoxLayout(existingVariablesP, BoxLayout.Y_AXIS));
        for (String variable : variables.keySet()) {
            VariableP globalVariableP = new VariableP(variable);
            existingVariablesP.add(globalVariableP);
        }

        existingVariablesSP = new JScrollPane();
        existingVariablesSP.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        existingVariablesSP.setViewportView(existingVariablesP);
        cumulComponentHeight += Math.max(existingVariablesSP.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, existingVariablesSP.getPreferredSize().width);
        getContentPane().add(existingVariablesSP, BorderLayout.NORTH);

        JPanel buttonsP = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        newB = new javax.swing.JButton();
        newB.setText("Add New");
        newB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newBActionPerformed(evt);
            }
        });
        newB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(newB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, newB.getPreferredSize().width);
        buttonsP.add(newB, constraints);
        constraints.gridy = constraints.gridy + 1;

        okB = new javax.swing.JButton();
        okB.setText("OK");
        okB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBActionPerformed(evt);
            }
        });
        okB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(okB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, okB.getPreferredSize().width);
        buttonsP.add(okB, constraints);
        constraints.gridy = constraints.gridy + 1;

        cancelB = new javax.swing.JButton();
        cancelB.setText("Cancel");
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        cancelB.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        cumulComponentHeight += Math.max(cancelB.getPreferredSize().getHeight(), BUTTON_HEIGHT);
        maxComponentWidth = Math.max(maxComponentWidth, cancelB.getPreferredSize().width);
        buttonsP.add(cancelB, constraints);
        constraints.gridy = constraints.gridy + 1;

        getContentPane().add(buttonsP, BorderLayout.SOUTH);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        maxComponentWidth = Math.min(maxComponentWidth, screenWidth);
        cumulComponentHeight = Math.min(cumulComponentHeight, screenHeight);
        // Don't use cumulComponentHeight for now
        setSize(new Dimension(maxComponentWidth, (int) (screenHeight * 0.9)));
        setPreferredSize(new Dimension(maxComponentWidth, (int) (screenHeight * 0.9)));

        validate();
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
