package dreaam.developer;

import sami.event.ReflectedEventSpecification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import sami.markup.Markup;
import sami.markup.MarkupOption;
import sami.markup.ReflectedMarkupOptionSpecification;
import sami.markup.ReflectedMarkupSpecification;

/**
 * Used to receive parameters from operator needed by an Output event
 *
 * @author pscerri
 */
public class ReflectedMarkupD extends javax.swing.JDialog {

    private final static Logger LOGGER = Logger.getLogger(ReflectedMarkupD.class.getName());
    final static int BUTTON_HEIGHT = 50;
    final static int BORDER = 5;
    int maxComponentWidth = 100;
    private ScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton okButton;
    HashMap<String, JPanel> enumFieldNameToPanel;
    HashMap<String, JComboBox> enumFieldNameToCombo;
    HashMap<String, HashMap<String, JPanel>> enumToValueToPanel;
    HashMap<String, HashMap<String, Class>> enumToValueToClass;
    HashMap<String, HashMap<String, HashMap<Field, JComponent>>> enumToValueToFieldToComp;
    HashMap<String, HashMap<String, HashMap<Field, JComboBox>>> enumToValueToFieldToCombo;
    HashMap<String, HashMap<String, HashMap<Field, Object>>> enumToValueToFieldToDefinition;
    private final ReflectedMarkupSpecification markupSpec;
    public static final int RECURSION_DEPTH = 2;
    // Serializable variable definition storage using HashMaps and Strings to represent object
    HashMap<String, Object> enumFieldNameToDefinition = new HashMap<String, Object>();
    HashMap<String, HashMap<String, Object>> optionFieldNameToDefinition = new HashMap<String, HashMap<String, Object>>();
    Class markupClass = null;
    Markup markupInstance = null;
    public HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();

    /**
     * Creates new form ReflectedEventD
     */
    public ReflectedMarkupD(ReflectedMarkupSpecification markupSpec, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.markupSpec = markupSpec;

        enumFieldNameToPanel = new HashMap<String, JPanel>();
        enumFieldNameToCombo = new HashMap<String, JComboBox>();
        enumToValueToPanel = new HashMap<String, HashMap<String, JPanel>>();
        enumToValueToClass = new HashMap<String, HashMap<String, Class>>();
        enumToValueToFieldToComp = new HashMap<String, HashMap<String, HashMap<Field, JComponent>>>();
        enumToValueToFieldToCombo = new HashMap<String, HashMap<String, HashMap<Field, JComboBox>>>();
        enumToValueToFieldToDefinition = new HashMap<String, HashMap<String, HashMap<Field, Object>>>();

        try {
            markupClass = Class.forName(markupSpec.getClassName());
            markupInstance = (Markup) markupClass.newInstance();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            Logger.getLogger(ReflectedMarkupSpecification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedMarkupSpecification.class.getName()).log(Level.SEVERE, null, ex);
        }

        initComponents();
        setTitle("ReflectedMarkupD");
    }

    /**
     * Create JComponents to hold the values of parameters
     *
     * @param obj
     * @param existingValues
     */
    protected void addEnumComponents() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        for (String enumFieldName : markupInstance.enumFieldNames) {
            try {
                final Field enumField = markupClass.getField(enumFieldName);
                if (enumField.getType().isEnum()) {
                    // Initialize hashmaps
                    enumToValueToPanel.put(enumFieldName, new HashMap<String, JPanel>());
                    enumToValueToClass.put(enumFieldName, new HashMap<String, Class>());
                    HashMap<String, HashMap<Field, JComponent>> valueToFieldToComp = new HashMap<String, HashMap<Field, JComponent>>();
                    HashMap<String, HashMap<Field, JComboBox>> valueToFieldToCombo = new HashMap<String, HashMap<Field, JComboBox>>();
                    HashMap<String, HashMap<Field, Object>> valueToFieldToDefinition = new HashMap<String, HashMap<Field, Object>>();
                    for (Object enumValue : enumField.getType().getEnumConstants()) {
                        valueToFieldToComp.put(enumValue.toString(), new HashMap<Field, JComponent>());
                        valueToFieldToCombo.put(enumValue.toString(), new HashMap<Field, JComboBox>());
                        valueToFieldToDefinition.put(enumValue.toString(), new HashMap<Field, Object>());
                    }
                    enumToValueToFieldToComp.put(enumFieldName, valueToFieldToComp);
                    enumToValueToFieldToCombo.put(enumFieldName, valueToFieldToCombo);
                    enumToValueToFieldToDefinition.put(enumFieldName, valueToFieldToDefinition);

                    // Add description for this enum
                    JLabel description = new JLabel(markupInstance.enumNameToDescription.get(enumField.getName()), SwingConstants.LEFT);
                    description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                    maxComponentWidth = Math.max(maxComponentWidth, description.getPreferredSize().width);
                    paramsPanel.add(description, constraints);
                    constraints.gridy = constraints.gridy + 1;

                    // Add combo box of the enum's possible values
                    JComboBox comboBox = new JComboBox(enumField.getType().getEnumConstants());
                    maxComponentWidth = Math.max(maxComponentWidth, comboBox.getPreferredSize().width);
                    paramsPanel.add(comboBox, constraints);
                    constraints.gridy = constraints.gridy + 1;
                    enumFieldNameToCombo.put(enumFieldName, comboBox);

                    // Add panel for handling interaction for the selected enum value
                    for (Object enumValue : enumField.getType().getEnumConstants()) {
                        JPanel markupValuePanel = constructEnumValuePanel(enumField, enumValue);
                        maxComponentWidth = Math.max(maxComponentWidth, markupValuePanel.getPreferredSize().width);
                        paramsPanel.add(markupValuePanel, constraints);
                        constraints.gridy = constraints.gridy + 1;
                    }

                    // Add space between each enum's interaction area
                    paramsPanel.add(Box.createRigidArea(new Dimension(0, 25)), constraints);
                    constraints.gridy = constraints.gridy + 1;

                    // Indicate that there is no markup value panel currently visible 
                    enumFieldNameToPanel.put(enumFieldName, null);

                    // Add listener to enum combo box
                    comboBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            enumValueSelected(enumField);
                        }
                    });

                    if (fieldNameToDefinition.containsKey(enumField.getName())) {
                        System.out.println("Have previous enum value");
                        comboBox.setSelectedItem(fieldNameToDefinition.get(enumField.getName()));
                    } else {
                        comboBox.setSelectedIndex(0);
                    }

                } else {
                    LOGGER.severe("Markup class field " + enumField.getName() + " is not an enum");
                }
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            } catch (SecurityException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected JPanel constructEnumValuePanel(Field enumField, Object enumValue) {
        JPanel enumValuePanel = new JPanel();
        enumValuePanel.setLayout(new GridBagLayout());

        HashMap<String, JPanel> valueToPanel = enumToValueToPanel.get(enumField.getName());
        HashMap<String, Class> valueToClass = enumToValueToClass.get(enumField.getName());
        HashMap<String, HashMap<Field, JComponent>> valueToFieldToComp = enumToValueToFieldToComp.get(enumField.getName());
        HashMap<String, HashMap<Field, JComboBox>> valueToFieldToCombo = enumToValueToFieldToCombo.get(enumField.getName());
        HashMap<String, HashMap<Field, Object>> valueToFieldToDefinition = enumToValueToFieldToDefinition.get(enumField.getName());

        // Store the class?
        String enumValueFieldName = markupInstance.enumValueToFieldName.get((Enum) enumValue);
        // Make the value to panel hash
        valueToPanel.put(enumValue.toString(), enumValuePanel);
        enumValuePanel.setVisible(false);
        if (enumValueFieldName != null) {
            try {
                Field enumValueField = markupClass.getField(enumValueFieldName);
                valueToClass.put(enumValue.toString(), enumValueField.getType());
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Make the field to comp hash
            valueToFieldToComp.put(enumValue.toString(), new HashMap<Field, JComponent>());
            // Make the field to combo hash
            valueToFieldToCombo.put(enumValue.toString(), new HashMap<Field, JComboBox>());
            // Make the field to definition
            valueToFieldToDefinition.put(enumValue.toString(), new HashMap<Field, Object>());

            // Construct the panel for this enum value
            addMarkupOptionComponents(enumField, enumValue, enumValuePanel);
        } else {
            LOGGER.severe("No field name linked to enum \"" + enumField.getName() + "\" value \"" + enumValue + "\"");
        }
        return enumValuePanel;
    }

    protected void enumValueSelected(Field enumField) {
        // Retrieve the panel for this enum value
        Object enumValue = enumFieldNameToCombo.get(enumField.getName()).getSelectedItem();
        if (enumFieldNameToPanel.get(enumField.getName()) != null) {
            JPanel oldEnumValuePanel = enumFieldNameToPanel.get(enumField.getName());
            oldEnumValuePanel.setVisible(false);
            oldEnumValuePanel.setBorder(null);
        }
        JPanel enumValuePanel = enumToValueToPanel.get(enumField.getName()).get(enumValue.toString());
        enumValuePanel.setVisible(true);
        if (enumValuePanel.getComponentCount() > 0) {
            enumValuePanel.setBorder(BorderFactory.createLineBorder(Color.black));
        }
        enumFieldNameToPanel.put(enumField.getName(), enumValuePanel);
        paramsPanel.revalidate();
    }

    protected void addMarkupOptionComponents(Field enumField, Object enumValue, JPanel enumValuePanel2asdfasdf) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        // Get JPanel, class, and values for this value of the enum
        JPanel enumValuePanel = enumToValueToPanel.get(enumField.getName()).get(enumValue.toString());
        Class enumValueClass = enumToValueToClass.get(enumField.getName()).get(enumValue.toString());
        HashMap<Field, Object> fieldToDefinition = enumToValueToFieldToDefinition.get(enumField.getName()).get(enumValue.toString());

        // Get stored reflected option spec for the MarkupOption field associated with this value of the enum
        String enumValueFieldName = markupInstance.enumValueToFieldName.get((Enum) enumValue);
        Object temp = fieldNameToDefinition.get(enumValueFieldName);
        ReflectedMarkupOptionSpecification optionSpec = null;
        if (temp != null && temp instanceof ReflectedMarkupOptionSpecification) {
            optionSpec = (ReflectedMarkupOptionSpecification) temp;
        }

        try {
            if (MarkupOption.class.isAssignableFrom(enumValueClass)) {
                // Get instance of markup option
                MarkupOption markupOptionInstance = (MarkupOption) (enumValueClass.newInstance());

                for (String optionFieldName : markupOptionInstance.fieldNames) {
                    // Get corresponding field
                    Field optionField = markupOptionInstance.getClass().getField(optionFieldName);
                    // Add description for this field of the markup option
                    JLabel description = new JLabel(markupOptionInstance.fieldNameToDescription.get(optionFieldName), SwingConstants.LEFT);
                    description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                    enumValuePanel.add(description, constraints);
                    constraints.gridy = constraints.gridy + 1;
                    // Add combo box for selecting variable name
                    addVariableComboBox(enumField.getName(), enumValue.toString(), optionField, fieldToDefinition, enumValuePanel, constraints, optionSpec);
                    constraints.gridy = constraints.gridy + 1;
                    // Add component for defining value
                    addValueComponent(enumField.getName(), enumValue.toString(), optionField, fieldToDefinition, enumValuePanel, constraints, optionSpec);
                    constraints.gridy = constraints.gridy + 1;
                }
            }
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    protected void addVariableComboBox(String enumName, String enumValueName, Field optionField, HashMap<Field, Object> fieldToDefinition, JPanel panel, GridBagConstraints constraints, ReflectedMarkupOptionSpecification optionSpec) {
        ArrayList<String> existingVariables = null;
        try {
            existingVariables = (ArrayList<String>) (new Mediator()).getAllVariables().clone();
        } catch (NullPointerException e) {
            existingVariables = new ArrayList<String>();
        }
        existingVariables.add(0, ReflectedEventSpecification.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        if (optionSpec != null) {
            Object optionFieldValue = optionSpec.getFieldDefinitions().get(optionField.getName());
            if (optionFieldValue != null && optionFieldValue instanceof String && ((String) optionFieldValue).startsWith("@")) {
                // This field is already defined with a variable name
                if (existingVariables.contains((String) optionFieldValue)) {
                    comboBox.setSelectedItem((String) optionFieldValue);
                } else {
                    LOGGER.severe("Nonexistent variable name referenced: " + (String) optionFieldValue);
                }
            }
        }
        maxComponentWidth = Math.max(maxComponentWidth, comboBox.getPreferredSize().width);
        panel.add(comboBox, constraints);
        enumToValueToFieldToCombo.get(enumName).get(enumValueName).put(optionField, comboBox);
    }

    protected void addValueComponent(String enumName, String enumValueName, Field optionField, HashMap<Field, Object> fieldToDefinition, JPanel panel, GridBagConstraints constraints, ReflectedMarkupOptionSpecification optionSpec) {
        JComponent component = UiComponentGenerator.getInstance().getCreationComponent(optionField.getType());
        Object definition = null;
        if (optionSpec != null) {
            definition = optionSpec.getFieldDefinitions().get(optionField.getName());
        }
        if (component != null && definition != null) {
            // This field already has been defined with a value
            // Earlier we had to replace primitive fields with their wrapper object, take that into account here
            if (definition.getClass().equals(optionField.getType())
                    || (definition.getClass().equals(Double.class) && optionField.getType().equals(double.class))
                    || (definition.getClass().equals(Float.class) && optionField.getType().equals(float.class))
                    || (definition.getClass().equals(Integer.class) && optionField.getType().equals(int.class))
                    || (definition.getClass().equals(Long.class) && optionField.getType().equals(long.class))) {
                UiComponentGenerator.getInstance().setComponentValue(definition, component);
            }
        }
        if (component == null) {
            // There is no component that can be used to define a value for this field
            // The field can only be set to a variable name
            // Show a message for now, may remove this later...
            component = new JLabel("No component", SwingConstants.LEFT);
        }
        maxComponentWidth = Math.max(maxComponentWidth, component.getPreferredSize().width);
        panel.add(component, constraints);
        enumToValueToFieldToComp.get(enumName).get(enumValueName).put(optionField, component);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        fieldNameToDefinition = markupSpec.getFieldDefinitions();
        if (fieldNameToDefinition == null) {
            fieldNameToDefinition = new HashMap<String, Object>();
        }
        System.out.println("fieldNameToDefinition:\n" + fieldNameToDefinition.toString());
        paramsPanel = new javax.swing.JPanel();
        BoxLayout paramsLayout = new BoxLayout(paramsPanel, BoxLayout.Y_AXIS);
        paramsPanel.setLayout(paramsLayout);
        paramsPanel.setLayout(new GridBagLayout());
        addEnumComponents();
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
        markupSpec.setFieldDefinitions(getDefinitionsFromComponents());
    }

    private HashMap<String, Object> getDefinitionsFromComponents() {
        HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();

        // Store enum values
        for (String enumName : enumFieldNameToCombo.keySet()) {
            fieldNameToDefinition.put(enumName, enumFieldNameToCombo.get(enumName).getSelectedItem());
        }

        // Store enum value field values
        for (String enumName : enumFieldNameToCombo.keySet()) {
            // Get the markup option class associated with the enum value
            Object enumValue = enumFieldNameToCombo.get(enumName).getSelectedItem();
            Class enumValueClass = enumToValueToClass.get(enumName).get(enumValue.toString());

            if (enumValueClass != null) {
                ReflectedMarkupOptionSpecification optionSpec = new ReflectedMarkupOptionSpecification(enumValueClass.getName());
                // Save values in reflected markup option spec
                HashMap<Field, JComponent> valueComponentLookup = enumToValueToFieldToComp.get(enumName).get(enumValue.toString());
                HashMap<Field, JComboBox> variableComboBoxLookup = enumToValueToFieldToCombo.get(enumName).get(enumValue.toString());
                for (Field optionField : variableComboBoxLookup.keySet()) {
                    Object definition = null;
                    JComboBox variableComboBox = variableComboBoxLookup.get(optionField);
                    if (!variableComboBox.getSelectedItem().toString().equalsIgnoreCase(ReflectedEventSpecification.NONE)) {
                        // User selected a variable other than NONE to define the field - store the variable name
                        definition = variableComboBox.getSelectedItem().toString();
                    } else {
                        // User selected NONE for variable, now see if they created a value definition
                        JComponent valueComponent = valueComponentLookup.get(optionField);
                        if (valueComponent != null) {
                            // Store the value from the component
                            definition = UiComponentGenerator.getInstance().getComponentValue(valueComponent, optionField);
                        }
                    }
                    optionSpec.addFieldDefinition(optionField.getName(), definition);
                }
                // Store refl markup option spec
                String markupValueFieldName = markupInstance.enumValueToFieldName.get((Enum) enumValue);
                fieldNameToDefinition.put(markupValueFieldName, optionSpec);
            } else {
                LOGGER.severe("No class associated with enum \"" + enumName + "\" value \"" + enumValue + "\"");
            }
        }

        return fieldNameToDefinition;
    }

    public static void main(String argv[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                ReflectedMarkupD dialog = new ReflectedMarkupD(new ReflectedMarkupSpecification("sami.markup.Attention"), null, true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
}
