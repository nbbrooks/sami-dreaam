package dreaam.developer;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import sami.event.Event;
import sami.event.ReflectionHelper;
import sami.markup.Markup;
import sami.mission.MissionPlanSpecification;
import sami.uilanguage.MarkupComponent;

/**
 * Used to receive parameters from operator needed by an Output event
 *
 * @author pscerri
 */
public class ReflectedEventD extends javax.swing.JDialog {

    private enum EventType {

        INPUT, OUTPUT
    };
    private final static Logger LOGGER = Logger.getLogger(ReflectedEventD.class.getName());
    private final static int BUTTON_HEIGHT = 50;
    private final static int BORDER = 5;
    private int maxComponentWidth = 100;
    private ScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton okButton;
    // Class held by event spec
    private Class eventClass = null;
    private EventType eventType;

    private HashMap<String, Object> fieldNameToValue;
    private HashMap<String, String> fieldNameToReadVariable;
    private HashMap<String, String> fieldNameToWriteVariable;
    private HashMap<String, Boolean> fieldNameToEditable;

    private final HashMap<Field, MarkupComponent> fieldToValueComponent;
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
    private final Mediator mediator = new Mediator();

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
        fieldToValueComponent = new HashMap<Field, MarkupComponent>();
        fieldToVariableCB = new HashMap<Field, JComboBox>();
        variableCBToField = new HashMap<JComboBox, Field>();
        fieldToVariableTF = new HashMap<Field, JTextField>();
        fieldToEditableB = new HashMap<Field, JButton>();

        initComponents();
        setTitle("ReflectedEventD");
    }

    protected void addParamComponents() {
        try {
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
            HashMap<String, String> fieldNameToDescription = (HashMap<String, String>) (eventClass.getField("fieldNameToDescription").get(null));
            LOGGER.info("ReflectedEventD adding fields for " + eventSpec + ", fields: " + fieldNames.toString());

            GridBagConstraints paramsConstraints = new GridBagConstraints();
            paramsConstraints.fill = GridBagConstraints.HORIZONTAL;
            paramsConstraints.gridy = 0;
            paramsConstraints.gridx = 0;
            paramsConstraints.weightx = 1.0;

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
                fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
                fieldConstraints.gridy = 0;
                fieldConstraints.gridx = 0;
                fieldConstraints.weightx = 1.0;

                // Add description for this field
                JLabel description = new JLabel(fieldNameToDescription.get(fieldName), SwingConstants.LEFT);
                description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                fieldPanel.add(description, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add combo box for selecting variable name
                addVariableComboBox(field, fieldNameToReadVariable, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add component for defining value
                addValueComponent(field, fieldNameToValue, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add toggle button for setting ability to edit field at run-time
                addEditableButton(field, fieldNameToEditable, fieldPanel, fieldConstraints);
                fieldConstraints.gridy = fieldConstraints.gridy + 1;

                // Add fieldPanel to paramsPanel
                maxComponentWidth = Math.max(maxComponentWidth, fieldPanel.getPreferredSize().width);
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
                    variableConstraints.fill = GridBagConstraints.HORIZONTAL;
                    variableConstraints.gridy = 0;
                    variableConstraints.gridx = 0;
                    variableConstraints.weightx = 1.0;

                    // Add description for this field
                    JLabel description = new JLabel(variableNameToDescription.get(variableFieldName), SwingConstants.LEFT);
                    description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                    maxComponentWidth = Math.max(maxComponentWidth, description.getPreferredSize().width);
                    variablePanel.add(description, variableConstraints);
                    variableConstraints.gridy = variableConstraints.gridy + 1;

                    // Add text field for setting variable
                    addVariableTextField(variableField, fieldNameToWriteVariable, variablePanel, variableConstraints);
                    variableConstraints.gridy = variableConstraints.gridy + 1;

                    // Add variablePanel to paramsPanel
                    maxComponentWidth = Math.max(maxComponentWidth, variablePanel.getPreferredSize().width);
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
        maxComponentWidth = Math.max(maxComponentWidth, textField.getPreferredSize().width);
        panel.add(textField, constraints);
    }

    protected void addVariableComboBox(Field field, HashMap<String, String> fieldNameToReadVariable, JPanel panel, GridBagConstraints constraints) {
        ArrayList<String> existingVariables = mediator.getProjectSpec().getVariables(field);
        existingVariables.add(0, Event.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        if (fieldNameToReadVariable.containsKey(field.getName())) {
            if (existingVariables.contains(fieldNameToReadVariable.get(field.getName()))) {
                comboBox.setSelectedItem(fieldNameToReadVariable.get(field.getName()));
            } else {
                LOGGER.severe("Read variable \"" + fieldNameToReadVariable.get(field.getName()) + "\" for field " + fieldNameToReadVariable.get(field.getName()) + " is not an existing variable");
            }
        }
        fieldToVariableCB.put(field, comboBox);
        variableCBToField.put(comboBox, field);
        maxComponentWidth = Math.max(maxComponentWidth, comboBox.getPreferredSize().width);
        panel.add(comboBox, constraints);
        comboBox.addItemListener(variableSelectedListener);
    }

    protected void addValueComponent(Field field, HashMap<String, Object> fieldNameToValue, JPanel panel, GridBagConstraints constraints) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent((java.lang.reflect.Type) field.getType(), new ArrayList<Markup>());
        JComponent visualization = null;
        if (markupComponent != null && markupComponent.getComponent() != null) {
            visualization = markupComponent.getComponent();
            Object definition = fieldNameToValue.get(field.getName());
            if (definition != null) {
                // This field already has been defined with a value
                // Earlier we had to replace primitive fields with their wrapper object, take that into account here
                if (definition.getClass().equals(field.getType())
                        || (definition.getClass().equals(Double.class) && field.getType().equals(double.class))
                        || (definition.getClass().equals(Float.class) && field.getType().equals(float.class))
                        || (definition.getClass().equals(Integer.class) && field.getType().equals(int.class))
                        || (definition.getClass().equals(Long.class) && field.getType().equals(long.class))
                        || (definition.getClass().equals(Boolean.class) && field.getType().equals(boolean.class))) {
                    UiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
                }
            }
            fieldToValueComponent.put(field, markupComponent);
        } else {
            // There is no component that can be used to define a value for this field
            // The field can only be set to a variable name
            // Show a message for now, may remove this later...
            visualization = new JLabel("No component", SwingConstants.LEFT);
        }
        maxComponentWidth = Math.max(maxComponentWidth, visualization.getPreferredSize().width);
        panel.add(visualization, constraints);
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
        } else {
            if (variableCombo.getSelectedIndex() != 0) {
                enableEditB.setText("Locked");
                enableEditB.setSelected(false);
                enableEditB.setEnabled(false);
            }
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
        maxComponentWidth = Math.max(maxComponentWidth, enableEditB.getPreferredSize().width);
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

        scrollPane = new ScrollPane();
        scrollPane.add(paramsPanel);
        scrollPane.setPreferredSize(paramsPanel.getPreferredSize());

        okButton = new javax.swing.JButton();
        okButton.setText("OK");
        okButton.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        BorderLayout okLayout = new BorderLayout(BORDER, BORDER);
        JPanel okPanel = new JPanel(okLayout);
        okPanel.setPreferredSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okPanel.setMaximumSize(new Dimension(maxComponentWidth, BUTTON_HEIGHT));
        okPanel.add(okButton);

        BoxLayout paneLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(paneLayout);
        getContentPane().setPreferredSize(new Dimension(maxComponentWidth + 8 * BORDER, 600));
        getContentPane().add(scrollPane);
        getContentPane().add(okPanel);
        pack();
    }

    /**
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
        HashMap<String, Object> fieldNameToValue = getValuesFromComponents();
        eventSpec.setFieldValues(fieldNameToValue);
        HashMap<String, String> fieldNameToReadVariable = getReadVariablesFromComponents();
        eventSpec.setReadVariables(fieldNameToReadVariable);
        HashMap<String, String> fieldNameToWriteVariable = getWriteVariablesFromComponents();
        eventSpec.setWriteVariables(fieldNameToWriteVariable);
        HashMap<String, Boolean> fieldNameToEditable = getEditableFromComponents(fieldNameToValue, fieldNameToReadVariable);
        eventSpec.setEditableFields(fieldNameToEditable);
    }

    /**
     * For each field with a creation component, store the defined value if it
     * is not null
     *
     * @return HashMap of field names to defined, non-null values
     */
    private HashMap<String, Object> getValuesFromComponents() {
        HashMap<String, Object> fieldNameToObject = new HashMap<String, Object>();
        for (Field field : fieldToValueComponent.keySet()) {
            JComboBox variableComboBox = fieldToVariableCB.get(field);
            if (variableComboBox == null || variableComboBox.getSelectedItem().toString().equalsIgnoreCase(Event.NONE)) {
                // User selected NONE for variable, now see if they created a value definition
                MarkupComponent markupComponent = fieldToValueComponent.get(field);
                if (markupComponent != null) {
                    // Store the value from the component
                    fieldNameToObject.put(field.getName(), UiComponentGenerator.getInstance().getComponentValue(markupComponent, field.getType()));
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
