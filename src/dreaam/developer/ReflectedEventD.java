package dreaam.developer;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import sami.engine.Mediator;
import sami.event.Event;
import sami.event.ReflectionHelper;
import sami.mission.MissionPlanSpecification;

/**
 * Dialog for specifying the parameters for an input (fields and variables) or
 * output (fields) event.
 *
 * For classes with no MarkupComponent supporting its creation, we allow
 * recursion into the class's fields to get components supporting those fields
 * (up to a certain depth)
 *
 * @author nbb
 */
public class ReflectedEventD extends javax.swing.JDialog {

    private enum EventType {

        INPUT, OUTPUT
    };

    private final static Logger LOGGER = Logger.getLogger(ReflectedEventD.class.getName());
    private final static int BUTTON_WIDTH = 250;
    private final static int BUTTON_HEIGHT = 50;
    private JScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton saveB;
    // Class held by event spec
    private Class eventClass = null;
    private EventType eventType;

    private HashMap<String, Object> fieldNameToValue;
    private HashMap<String, String> fieldNameToReadVariable;
    private HashMap<String, String> fieldNameToWriteVariable;
    private HashMap<String, Boolean> fieldNameToEditable;

    private final HashMap<Field, ComplexMarkupCreationComponentP> fieldToComplexValueComponent;
    // Combo box for selecting a variable to read at run-time to get the field's object definition
    private final HashMap<Field, JComboBox> fieldToVariableCB;
    // Reverse lookup for disabling/enabling editable button based on variable selection
    private final HashMap<JComboBox, Field> variableCBToField;
    // Button to choose whether or not the field definition can be modified at run-time
    private final HashMap<Field, JButton> fieldToEditableB;
    // Text field to provide the variable name to write the run-time field's value to (input events only)
    private final HashMap<Field, JTextField> fieldToVariableTF;
    private final ReflectedEventSpecification eventSpec;
    private final MissionPlanSpecification mSpec;
    private final VariableSelectedListener variableSelectedListener = new VariableSelectedListener();
    private final Mediator mediator = Mediator.getInstance();

    /**
     * Creates new form ReflectedEventD
     */
    public ReflectedEventD(java.awt.Frame parent, boolean modal, ReflectedEventSpecification eventSpec, MissionPlanSpecification mSpec) {
        super(parent, modal);
        this.eventSpec = eventSpec;
        this.mSpec = mSpec;
        try {
            eventClass = Class.forName(eventSpec.getClassName());
            if (InputEvent.class.isAssignableFrom(eventClass)) {
                // Input event
                eventType = ReflectedEventD.EventType.INPUT;
            } else {
                // Output event
                eventType = ReflectedEventD.EventType.OUTPUT;
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        fieldToComplexValueComponent = new HashMap<Field, ComplexMarkupCreationComponentP>();
        fieldToVariableCB = new HashMap<Field, JComboBox>();
        variableCBToField = new HashMap<JComboBox, Field>();
        fieldToVariableTF = new HashMap<Field, JTextField>();
        fieldToEditableB = new HashMap<Field, JButton>();

        initComponents();
    }

    protected void addParamComponents() {
        try {
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
            HashMap<String, String> fieldNameToDescription = (HashMap<String, String>) (eventClass.getField("fieldNameToDescription").get(null));
            LOGGER.info("ReflectedEventD adding fields for " + eventSpec + ", fields: " + fieldNames.toString());

            GridBagConstraints paramsConstraints = new GridBagConstraints();
            paramsConstraints.gridx = 0;
            paramsConstraints.gridy = 0;
            paramsConstraints.fill = GridBagConstraints.BOTH;
            paramsConstraints.weightx = 1.0;
            paramsConstraints.weighty = 1.0;

            for (String fieldName : fieldNames) {
                final Field field = ReflectionHelper.getField(eventClass, fieldName);
                if (field == null) {
                    LOGGER.severe("Could not find field \"" + fieldName + "\" in class " + eventClass.getSimpleName() + " or any super class");
                    continue;
                }
                JPanel fieldPanel = new JPanel();
                fieldPanel.setLayout(new GridBagLayout());
                fieldPanel.setBorder(BorderFactory.createLineBorder(Color.black));

                GridBagConstraints fieldConstraints = new GridBagConstraints();
                fieldConstraints.gridx = 0;
                fieldConstraints.gridy = 0;
                fieldConstraints.fill = GridBagConstraints.BOTH;
                fieldConstraints.weightx = 1.0;
                fieldConstraints.weighty = 1.0;

                // Add description for this field
                JLabel description = new JLabel(" " + fieldNameToDescription.get(fieldName), SwingConstants.LEFT);
                description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                fieldPanel.add(description, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add combo box for selecting variable name
                JLabel cbL = new JLabel(" Read variable name");
                cbL.setToolTipText("Read in this field's value at run-time from a variable");
                fieldPanel.add(cbL, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;
                addVariableComboBox(field, fieldNameToReadVariable, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add component for defining value
                JLabel definitionL = new JLabel(" Value");
                definitionL.setToolTipText("Manually define the value for this field");
                fieldPanel.add(definitionL, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                ComplexMarkupCreationComponentP creationComponent = new ComplexMarkupCreationComponentP(field, fieldNameToValue.get(field.getName()));
//                ClassQuantity quantity = null;
//                Class fieldClass = field.getType();
//                if (fieldClass == ArrayList.class) {
//                    quantity = ClassQuantity.ARRAY_LIST;
//                } else {
//                    quantity = ClassQuantity.SINGLE;
//                }
//                ComplexMarkupCreationComponentP creationComponent = new ComplexMarkupCreationComponentP(quantity, fieldClass, fieldNameToValue.get(field.getName()));
                fieldToComplexValueComponent.put(field, creationComponent);
                fieldPanel.add(creationComponent, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add text field for saving defined value to variable
                JLabel writeL = new JLabel(" Write variable name (optional)");
                writeL.setToolTipText("Optionally store this field's value in a variable for other events to reference");
                fieldPanel.add(writeL, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;
                addVariableTextField(field, fieldNameToWriteVariable, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add toggle button for setting ability to edit field at run-time
                JLabel editableL = new JLabel(" Definition editable at run-time?");
                editableL.setToolTipText("If a value is manually specified here, should it be editable at run-time?");
                fieldPanel.add(editableL, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;
                addEditableButton(field, fieldNameToEditable, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add fieldPanel to paramsPanel
                paramsPanel.add(fieldPanel, paramsConstraints);
                paramsConstraints.gridy = paramsConstraints.gridy + 1;

                // Add space between each enum's interaction area
                paramsPanel.add(Box.createRigidArea(new Dimension(0, 25)), paramsConstraints);
                paramsConstraints.gridy = paramsConstraints.gridy + 1;
            }

            if (eventType == ReflectedEventD.EventType.INPUT) {
                ArrayList<String> variableNames = (ArrayList<String>) (eventClass.getField("variableNames").get(null));
                HashMap<String, String> variableNameToDescription = (HashMap<String, String>) (eventClass.getField("variableNameToDescription").get(null));
                LOGGER.info("ReflectedEventD adding variable fields for " + eventSpec + ", fields: " + variableNames.toString());
                for (String variableFieldName : variableNames) {
                    final Field variableField = ReflectionHelper.getField(eventClass, variableFieldName);
                    if (variableField == null) {
                        LOGGER.severe("Could not find variable field \"" + variableFieldName + "\" in class " + eventClass.getSimpleName() + " or any super class");
                        continue;
                    }
                    JPanel variablePanel = new JPanel();
                    variablePanel.setLayout(new GridBagLayout());
                    variablePanel.setBorder(BorderFactory.createLineBorder(Color.black));

                    GridBagConstraints variableConstraints = new GridBagConstraints();
                    variableConstraints.gridx = 0;
                    variableConstraints.gridy = 0;
                    variableConstraints.fill = GridBagConstraints.BOTH;
                    variableConstraints.weightx = 1.0;
                    variableConstraints.weighty = 1.0;

                    // Add description for this field
                    JLabel description = new JLabel(" " + variableNameToDescription.get(variableFieldName), SwingConstants.LEFT);
                    description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                    variablePanel.add(description, variableConstraints);
                    variableConstraints.gridy = variableConstraints.gridy + 1;

                    // Add text field for setting variable
                    addVariableTextField(variableField, fieldNameToWriteVariable, variablePanel, variableConstraints);
                    variableConstraints.gridy = variableConstraints.gridy + 1;

                    // Add variablePanel to paramsPanel
                    paramsPanel.add(variablePanel, paramsConstraints);
                    paramsConstraints.gridy = paramsConstraints.gridy + 1;

                    // Add space between each enum's interaction area
                    paramsPanel.add(Box.createRigidArea(new Dimension(0, 25)), paramsConstraints);
                    paramsConstraints.gridy = paramsConstraints.gridy + 1;
                }
            }
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    protected void addVariableTextField(Field field, HashMap<String, String> fieldNameToWriteVariable, JPanel panel, GridBagConstraints constraints) {
        JTextField textField = new JTextField();

        if (fieldNameToWriteVariable.containsKey(field.getName())) {
            // This field is already defined with a variable name
            textField.setText(fieldNameToWriteVariable.get(field.getName()));
        }
        fieldToVariableTF.put(field, textField);
        panel.add(textField, constraints);
    }

    protected void addVariableComboBox(Field field, HashMap<String, String> fieldNameToReadVariable, JPanel panel, GridBagConstraints constraints) {
        ArrayList<String> existingVariables = mediator.getProject().getVariablesInScope(field, mSpec);
        existingVariables.add(0, Event.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        if (fieldNameToReadVariable.containsKey(field.getName())) {
            if (existingVariables.contains(fieldNameToReadVariable.get(field.getName()))) {
                comboBox.setSelectedItem(fieldNameToReadVariable.get(field.getName()));
            } else {
                LOGGER.severe("Read variable \"" + fieldNameToReadVariable.get(field.getName()) + "\" for field " + fieldNameToReadVariable.get(field.getName()) + " is not an existing variable");
            }
        }

        comboBox.setLightWeightPopupEnabled(false);

        fieldToVariableCB.put(field, comboBox);
        variableCBToField.put(comboBox, field);
        panel.add(comboBox, constraints);
        comboBox.addItemListener(variableSelectedListener);

    }

    protected void addEditableButton(Field field, HashMap<String, Boolean> fieldNameToEditable, JPanel panel, GridBagConstraints constraints) {
        final JButton enableEditB = new JButton();
        if (fieldNameToEditable.containsKey(field.getName())
                && !fieldNameToEditable.get(field.getName())) {
            // Field was previously defined and locked
            enableEditB.setText("Locked");
            enableEditB.setSelected(false);
        } else {
            enableEditB.setText("Editable");
            enableEditB.setSelected(true);
        }
        // Check if variable combo box has a variable name selected (saved value will have already been loaded at this point)
        JComboBox variableCombo = fieldToVariableCB.get(field);
        if (variableCombo == null) {
            LOGGER.severe("Could not find variable combo for field: " + field + ", fieldToVariableComboBox: " + fieldToVariableCB.toString());
        } else if (variableCombo.getSelectedIndex() != 0) {
            enableEditB.setText("Locked");
            enableEditB.setSelected(false);
            enableEditB.setEnabled(false);
        }
        enableEditB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                //@todo only allow field to be locked if a definition exists
                if (enableEditB.isSelected()) {
                    enableEditB.setText("Locked");
                    enableEditB.setSelected(false);
                } else {
                    enableEditB.setText("Editable");
                    enableEditB.setSelected(true);
                }
            }
        });
        fieldToEditableB.put(field, enableEditB);
        panel.add(enableEditB, constraints);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        // Get previously set field definitions
        fieldNameToValue = eventSpec.getFieldValues();
        if (fieldNameToValue == null) {
            fieldNameToValue = new HashMap<String, Object>();
        }
        fieldNameToReadVariable = eventSpec.getReadVariables();
        if (fieldNameToReadVariable == null) {
            fieldNameToReadVariable = new HashMap<String, String>();
        }
        fieldNameToWriteVariable = eventSpec.getWriteVariables();
        if (fieldNameToWriteVariable == null) {
            fieldNameToWriteVariable = new HashMap<String, String>();
        }
        // Get previously set field editability
        fieldNameToEditable = eventSpec.getEditableFields();
        if (fieldNameToEditable == null) {
            fieldNameToEditable = new HashMap<String, Boolean>();
        }
        paramsPanel = new javax.swing.JPanel();
        paramsPanel.setLayout(new GridBagLayout());
        addParamComponents();
        paramsPanel.revalidate();

        scrollPane = new JScrollPane(paramsPanel);
        scrollPane.setPreferredSize(paramsPanel.getPreferredSize());

        saveB = new javax.swing.JButton();
        saveB.setText("Save");
        saveB.setPreferredSize(new Dimension(saveB.getPreferredSize().width, BUTTON_HEIGHT));
        saveB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        BoxLayout boxLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(boxLayout);
        getContentPane().add(scrollPane);
        getContentPane().add(saveB);

        // Adjust dialog size
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenHeight = gd.getDisplayMode().getHeight();
        setPreferredSize(new Dimension(getPreferredSize().width, (int) (screenHeight * 0.9)));

        pack();
    }

    /**
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        //@todo check if all fields have either a read variable or manual value

        HashMap<String, Object> fieldNameToValue = getValuesFromComponents();

        eventSpec.setFieldValues(fieldNameToValue);
        HashMap<String, String> fieldNameToReadVariable = getReadVariablesFromComponents();
        eventSpec.setReadVariables(fieldNameToReadVariable);
        HashMap<String, String> fieldNameToWriteVariable = getWriteVariablesFromComponents();
        eventSpec.setWriteVariables(fieldNameToWriteVariable);
        HashMap<String, Boolean> fieldNameToEditable = getEditableFromComponents(fieldNameToValue, fieldNameToReadVariable);
        eventSpec.setEditableFields(fieldNameToEditable);
        setVisible(false);
    }

    /**
     * For each field with a creation component, store the defined value if it
     * is not null
     *
     * @return HashMap of field names to defined, non-null values
     */
    private HashMap<String, Object> getValuesFromComponents() {
        HashMap<String, Object> fieldNameToObject = new HashMap<String, Object>();
        for (Field field : fieldToComplexValueComponent.keySet()) {
            JComboBox variableComboBox = fieldToVariableCB.get(field);
            if (variableComboBox == null || variableComboBox.getSelectedItem().toString().equalsIgnoreCase(Event.NONE)) {
                // User selected NONE for variable, now see if they created a value definition
                ComplexMarkupCreationComponentP componentP = fieldToComplexValueComponent.get(field);
                if (componentP != null) {
                    Object value = componentP.getValue();
                    if (value != null) {
                        fieldNameToObject.put(field.getName(), value);
                    }
                }
            }
        }
        return fieldNameToObject;
    }

    /**
     * For each field with a read variable combo box, store the selected
     * variable name if it is not @NONE
     *
     * @return HashMap of field names to variable names (not @NONE)
     */
    private HashMap<String, String> getReadVariablesFromComponents() {
        HashMap<String, String> fieldNameToReadVariable = new HashMap<String, String>();
        for (Field field : fieldToVariableCB.keySet()) {
            JComboBox variableComboBox = fieldToVariableCB.get(field);
            if (!variableComboBox.getSelectedItem().toString().equalsIgnoreCase(Event.NONE)) {
                // User selected a variable other than NONE to define the field
                fieldNameToReadVariable.put(field.getName(), variableComboBox.getSelectedItem().toString());
            }
        }
        return fieldNameToReadVariable;
    }

    /**
     * For each field with a write variable text field, store the defined
     * variable name if it is not @NONE
     *
     * @return HashMap of field names to variable names (not @NONE)
     */
    private HashMap<String, String> getWriteVariablesFromComponents() {
        HashMap<String, String> fieldNameToWriteVariable = new HashMap<String, String>();
        for (Field field : fieldToVariableTF.keySet()) {
            JTextField variableTextField = fieldToVariableTF.get(field);
            if (variableTextField != null) {
                String variable = variableTextField.getText().trim();
                if (variable.length() > 0 && !variable.startsWith("@")) {
                    variable = "@" + variable;
                }
                if (variable.length() > 1 && variable.startsWith("@")) {
                    fieldNameToWriteVariable.put(field.getName(), variable);
                }
            }
        }

        return fieldNameToWriteVariable;
    }

    private HashMap<String, Boolean> getEditableFromComponents(HashMap<String, Object> fieldNameToValue, HashMap<String, String> fieldNameToReadVariable) {
        HashMap<String, Boolean> fieldNameToEditable = new HashMap<String, Boolean>();
        for (Field field : fieldToEditableB.keySet()) {
            if ((fieldNameToValue.containsKey(field.getName()) || fieldNameToReadVariable.containsKey(field.getName()))
                    && !fieldToEditableB.get(field).isSelected()) {
                // Field is defined and locked
                fieldNameToEditable.put(field.getName(), false);
            } else {
                // Field is not defined and/or is not locked
                fieldNameToEditable.put(field.getName(), true);
            }
        }

        return fieldNameToEditable;
    }

    private class VariableSelectedListener implements ItemListener {

        public VariableSelectedListener() {
        }

        // This method is called only if a new item has been selected.
        public void itemStateChanged(ItemEvent evt) {
            if (evt.getSource() instanceof JComboBox) {
                JComboBox variableCombo = (JComboBox) evt.getSource();
                Field field = variableCBToField.get(variableCombo);
                if (field == null) {
                    LOGGER.severe("Could not find field for variable combo box: " + variableCombo + ", variableComboBoxToField: " + variableCBToField.toString());
                    return;
                }
                JButton editableB = fieldToEditableB.get(field);
                if (editableB == null) {
                    LOGGER.severe("Could not find editable button for field: " + field + ", fieldToEditable: " + fieldToEditableB.toString());
                    return;
                }
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                    if (variableCombo.getSelectedIndex() == 0) {
                        // @NONE selected
                        // Enable the "Editable" button if it was disabled
                        if (!editableB.isEnabled()) {
                            editableB.setEnabled(true);
                        }
                    } else {
                        // Variable other than @NONE selected
                        // Lock value and disable "Editable" button (we don't want to change the variable at run-time, just values)
                        editableB.setText("Locked");
                        editableB.setSelected(false);
                        editableB.setEnabled(false);
                    }
                    paramsPanel.revalidate();
                }
            } else {

            }
        }
    }
}
