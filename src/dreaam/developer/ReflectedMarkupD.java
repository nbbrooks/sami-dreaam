package dreaam.developer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import sami.engine.Mediator;
import sami.event.Event;
import sami.event.ReflectionHelper;
import sami.markup.MarkupOption;
import sami.markup.ReflectedMarkupSpecification;
import sami.mission.MissionPlanSpecification;

/**
 * Dialog for specifying the parameters for a markup.
 *
 * @author nbb
 */
public class ReflectedMarkupD extends javax.swing.JDialog {

    private final static Logger LOGGER = Logger.getLogger(ReflectedMarkupD.class.getName());
    final static int BUTTON_WIDTH = 250;
    final static int BUTTON_HEIGHT = 50;
    private JScrollPane scrollPane;
    private JPanel paramsPanel;
    private JButton saveB;
    private HashMap<String, JPanel> enumFieldNameToPanel;
    private HashMap<String, JComboBox> enumFieldNameToCombo;
    private HashMap<String, HashMap<String, JPanel>> enumToConstantToPanel;
    private HashMap<String, HashMap<String, Class>> enumToConstantToClass;
    private HashMap<String, HashMap<String, HashMap<Field, ComplexMarkupCreationComponentP>>> enumToConstantToFieldToCreationP;
    private HashMap<String, HashMap<String, HashMap<Field, JComboBox>>> enumToConstantToFieldToCombo;
    private HashMap<String, HashMap<String, HashMap<Field, Object>>> enumToConstantToFieldToDefinition;
    private final ReflectedMarkupSpecification markupSpec;
    private final MissionPlanSpecification mSpec;
    public static final int RECURSION_DEPTH = 2;
    // Serializable variable definition storage using HashMaps and Strings to represent object
    private HashMap<String, Object> enumFieldNameToDefinition = new HashMap<String, Object>();
    private HashMap<String, HashMap<String, Object>> optionFieldNameToDefinition = new HashMap<String, HashMap<String, Object>>();
    private Class markupClass = null;
    private HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();
    private final Mediator mediator = Mediator.getInstance();

    /**
     * Creates new form ReflectedEventD
     */
    public ReflectedMarkupD(java.awt.Frame parent, boolean modal, ReflectedMarkupSpecification markupSpec, MissionPlanSpecification mSpec) {
        super(parent, modal);
        this.markupSpec = markupSpec;
        this.mSpec = mSpec;

        enumFieldNameToPanel = new HashMap<String, JPanel>();
        enumFieldNameToCombo = new HashMap<String, JComboBox>();
        enumToConstantToPanel = new HashMap<String, HashMap<String, JPanel>>();
        enumToConstantToClass = new HashMap<String, HashMap<String, Class>>();
        enumToConstantToFieldToCreationP = new HashMap<String, HashMap<String, HashMap<Field, ComplexMarkupCreationComponentP>>>();
        enumToConstantToFieldToCombo = new HashMap<String, HashMap<String, HashMap<Field, JComboBox>>>();
        enumToConstantToFieldToDefinition = new HashMap<String, HashMap<String, HashMap<Field, Object>>>();

        try {
            markupClass = Class.forName(markupSpec.getClassName());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        initComponents();
    }

    /**
     * Create JComponents for Enum values selection a
     *
     * @param obj
     * @param existingValues
     */
    protected void addComponents() {
        try {
            ArrayList<String> enumFieldNames = (ArrayList<String>) (markupClass.getField("enumFieldNames").get(null));
            HashMap<String, String> enumNameToDescription = (HashMap<String, String>) (markupClass.getField("enumNameToDescription").get(null));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;

            // Add component for each enum in the Markup
            for (String enumFieldName : enumFieldNames) {
                try {
                    final Field enumField = ReflectionHelper.getField(markupClass, enumFieldName);
                    if (enumFieldName == null) {
                        LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markupClass.getSimpleName() + " or any super class");
                        continue;
                    }
                    if (!enumField.getType().isEnum()) {
                        LOGGER.severe("Markup class field " + enumField.getName() + " is not an enum");
                        continue;
                    }
                    // Initialize hashmaps
                    enumToConstantToPanel.put(enumFieldName, new HashMap<String, JPanel>());
                    enumToConstantToClass.put(enumFieldName, new HashMap<String, Class>());
                    HashMap<String, HashMap<Field, ComplexMarkupCreationComponentP>> valueToFieldToCreationP = new HashMap<String, HashMap<Field, ComplexMarkupCreationComponentP>>();
                    HashMap<String, HashMap<Field, JComboBox>> valueToFieldToCombo = new HashMap<String, HashMap<Field, JComboBox>>();
                    HashMap<String, HashMap<Field, Object>> constantToFieldToDefinition = new HashMap<String, HashMap<Field, Object>>();
                    for (Object constant : enumField.getType().getEnumConstants()) {
                        valueToFieldToCreationP.put(constant.toString(), new HashMap<Field, ComplexMarkupCreationComponentP>());
                        valueToFieldToCombo.put(constant.toString(), new HashMap<Field, JComboBox>());
                        constantToFieldToDefinition.put(constant.toString(), new HashMap<Field, Object>());
                    }
                    enumToConstantToFieldToCreationP.put(enumFieldName, valueToFieldToCreationP);
                    enumToConstantToFieldToCombo.put(enumFieldName, valueToFieldToCombo);
                    enumToConstantToFieldToDefinition.put(enumFieldName, constantToFieldToDefinition);

                    // Add description for this enum to panel
                    JLabel description = new JLabel(" " + enumNameToDescription.get(enumField.getName()), SwingConstants.LEFT);
                    description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                    paramsPanel.add(description, constraints);
                    constraints.gridy = constraints.gridy + 1;

                    // Add combo box of the enum's possible values
                    JComboBox comboBox = new JComboBox(enumField.getType().getEnumConstants());
                    paramsPanel.add(comboBox, constraints);
                    constraints.gridy = constraints.gridy + 1;
                    enumFieldNameToCombo.put(enumFieldName, comboBox);

                    // Add panel for each enum constant to handle the associated MarkupOption (if there is one)
                    for (Object constant : enumField.getType().getEnumConstants()) {
                        JPanel constantPanel = constructConstantPanel(enumField, constant);
                        paramsPanel.add(constantPanel, constraints);
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
                            constantSelected(enumField);
                        }
                    });

                    if (fieldNameToDefinition.containsKey(enumField.getName())) {
                        comboBox.setSelectedItem(fieldNameToDefinition.get(enumField.getName()));
                    } else {
                        comboBox.setSelectedIndex(0);
                    }
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create panel to handle the MarkupOption associated with an enum constant
     * (if there is one)
     *
     * @param enumField
     * @param constant
     * @return
     */
    protected JPanel constructConstantPanel(Field enumField, Object constant) {
        JPanel constantPanel = new JPanel();
        constantPanel.setLayout(new GridBagLayout());
        try {
            HashMap<Enum, String> constantToFieldName = (HashMap<Enum, String>) (markupClass.getField("enumValueToFieldName").get(null));
            HashMap<String, JPanel> constantToPanel = enumToConstantToPanel.get(enumField.getName());
            HashMap<String, Class> constantToClass = enumToConstantToClass.get(enumField.getName());
            HashMap<String, HashMap<Field, ComplexMarkupCreationComponentP>> constantToFieldToCreationP = enumToConstantToFieldToCreationP.get(enumField.getName());
            HashMap<String, HashMap<Field, JComboBox>> constantToFieldToCombo = enumToConstantToFieldToCombo.get(enumField.getName());
            HashMap<String, HashMap<Field, Object>> constantToFieldToDefinition = enumToConstantToFieldToDefinition.get(enumField.getName());

            // Add entry to enum constant to panel hash
            constantToPanel.put(constant.toString(), constantPanel);
            constantPanel.setVisible(false);
            if (constantToFieldName.containsKey((Enum) constant)) {
                String fieldName = constantToFieldName.get((Enum) constant);
                if (fieldName != null) {
                    try {
                        // Field for MarkupOption associated with the enum constant
                        Field field = ReflectionHelper.getField(markupClass, fieldName);
                        if (field != null) {
                            constantToClass.put(constant.toString(), field.getType());

                            // Make the field to comp hash
                            constantToFieldToCreationP.put(constant.toString(), new HashMap<Field, ComplexMarkupCreationComponentP>());
                            // Make the field to combo hash
                            constantToFieldToCombo.put(constant.toString(), new HashMap<Field, JComboBox>());
                            // Make the field to definition
                            constantToFieldToDefinition.put(constant.toString(), new HashMap<Field, Object>());

                            // Construct the panel for this enum value
                            addConstantComponents(enumField, constant);
                        } else {
                            LOGGER.severe("Could not find field \"" + fieldName + "\" in class " + markupClass.getSimpleName() + " or any super class");
                        }
                    } catch (SecurityException ex) {
                        Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                LOGGER.severe("No field name linked to enum \"" + enumField.getName() + "\" value \"" + constant + "\"");
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return constantPanel;
    }

    protected void constantSelected(Field enumField) {
        // Retrieve the panel for this enum value
        Object enumValue = enumFieldNameToCombo.get(enumField.getName()).getSelectedItem();
        if (enumFieldNameToPanel.get(enumField.getName()) != null) {
            JPanel oldEnumValuePanel = enumFieldNameToPanel.get(enumField.getName());
            oldEnumValuePanel.setVisible(false);
            oldEnumValuePanel.setBorder(null);
        }
        JPanel enumValuePanel = enumToConstantToPanel.get(enumField.getName()).get(enumValue.toString());
        enumValuePanel.setVisible(true);
        if (enumValuePanel.getComponentCount() > 0) {
            enumValuePanel.setBorder(BorderFactory.createLineBorder(Color.black));
        }
        enumFieldNameToPanel.put(enumField.getName(), enumValuePanel);
        paramsPanel.revalidate();
    }

    /**
     * Fill out the panel for a particular enum value constant
     *
     * @param enumField
     * @param constant
     */
    protected void addConstantComponents(Field enumField, Object constant) {
        try {
            // Get JPanel, class, and values for this value of the enum
            // Get the JPanel that shows up when this constant is selected in the enum combo box
            JPanel constantPanel = enumToConstantToPanel.get(enumField.getName()).get(constant.toString());
            // Get the MarkupOption class linked to this enum constant value
            Class optionClass = enumToConstantToClass.get(enumField.getName()).get(constant.toString());

            if (optionClass == null) {
                return;
            }

            // Get the lookup for storing the MarkupOption's fields and definitions
            HashMap<Field, Object> optionFieldToDefinition = enumToConstantToFieldToDefinition.get(enumField.getName()).get(constant.toString());

            // Field retrieval 
            // Get the lookup from enum constant to associated MarkupOption
            HashMap<Enum, String> enumValueToFieldName = (HashMap<Enum, String>) (markupClass.getField("enumValueToFieldName").get(null));
            // Get the MarkupOption's field names
            ArrayList<String> optionFieldNames = (ArrayList<String>) (optionClass.getField("fieldNames").get(null));
            // Get the MarkupOption's field descriptions
            HashMap<String, String> optionFieldNameToDescription = (HashMap<String, String>) (optionClass.getField("fieldNameToDescription").get(null));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;

            // Get stored reflected option spec for the MarkupOption field associated with this value of the enum
            String fieldNameForConstant = enumValueToFieldName.get((Enum) constant);
            Object temp = fieldNameToDefinition.get(fieldNameForConstant);
            MarkupOption option = null;
            if (temp != null && temp instanceof MarkupOption) {
                option = (MarkupOption) temp;
            } else {
                try {
                    option = (MarkupOption) (optionClass.newInstance());
                } catch (InstantiationException ex) {
                    Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            for (String optionFieldName : optionFieldNames) {
                // Get corresponding field
                Field optionField = ReflectionHelper.getField(optionClass, optionFieldName);
                if (optionField == null) {
                    LOGGER.severe("Could not find field \"" + optionFieldName + "\" in class " + optionClass.getSimpleName() + " or any super class");
                    continue;
                }
                // Add description for this field of the markup option
                JLabel description = new JLabel(" " + optionFieldNameToDescription.get(optionFieldName), SwingConstants.LEFT);
                description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                constantPanel.add(description, constraints);
                constraints.gridy = constraints.gridy + 1;
                // Add combo box for selecting variable name
                addVariableComboBox(enumField.getName(), constant.toString(), optionField, constantPanel, constraints, option);
                constraints.gridy = constraints.gridy + 1;

//                // Get quantity setting
//                ClassQuantity quantity = null;
//                Class optionFieldClass = optionField.getType();
//                if (optionFieldClass == ArrayList.class) {
//                    quantity = ClassQuantity.ARRAY_LIST;
//                } else {
//                    quantity = ClassQuantity.SINGLE;
//                }
                // Get previous definition (if any)
                Object definition = null;
                if (option != null) {
                    try {
                        definition = optionField.get(option);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                // Get creation panel
                ComplexMarkupCreationComponentP creationP = new ComplexMarkupCreationComponentP(optionField, definition);
//                ComplexMarkupCreationComponentP creationP = new ComplexMarkupCreationComponentP(quantity, optionFieldClass, definition);
                enumToConstantToFieldToCreationP.get(enumField.getName()).get(constant.toString()).put(optionField, creationP);
                constantPanel.add(creationP, constraints);
                constraints.gridy = constraints.gridy + 1;
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Add combo box for selecting variable name for a MarkupOption field
     *
     * @param enumName
     * @param enumValueName
     * @param optionField
     * @param optionFieldToDefinition
     * @param constantPanel
     * @param constraints
     * @param option
     */
    protected void addVariableComboBox(String enumName, String enumValueName, Field optionField, JPanel constantPanel, GridBagConstraints constraints, MarkupOption option) {
        ArrayList<String> existingVariables = mediator.getProject().getVariablesInScope(optionField, mSpec);
        existingVariables.add(0, Event.NONE);
        JComboBox comboBox = new JComboBox(existingVariables.toArray());

        if (option != null) {
            String variableName = option.getVariableForField(optionField);
            if (variableName != null && variableName.startsWith("@")) {
                // This field is already defined with a variable name
                if (existingVariables.contains(variableName)) {
                    comboBox.setSelectedItem(variableName);
                } else {
                    LOGGER.severe("Nonexistent variable name referenced: " + variableName);
                }
            }
        }
        constantPanel.add(comboBox, constraints);
        enumToConstantToFieldToCombo.get(enumName).get(enumValueName).put(optionField, comboBox);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        fieldNameToDefinition = markupSpec.getFieldDefinitions();
        if (fieldNameToDefinition == null) {
            fieldNameToDefinition = new HashMap<String, Object>();
        }
        paramsPanel = new javax.swing.JPanel(new GridBagLayout());
        addComponents();
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
        setVisible(false);
        markupSpec.setFieldDefinitions(getDefinitionsFromComponents());
    }

    private HashMap<String, Object> getDefinitionsFromComponents() {
        HashMap<String, Object> fieldNameToDefinition = new HashMap<String, Object>();

        // Store enum values
        for (String enumName : enumFieldNameToCombo.keySet()) {
            fieldNameToDefinition.put(enumName, enumFieldNameToCombo.get(enumName).getSelectedItem());
        }

        try {
            HashMap<Enum, String> constantToFieldName = (HashMap<Enum, String>) (markupClass.getField("enumValueToFieldName").get(null));
            // Store enum value field values
            for (String enumName : enumFieldNameToCombo.keySet()) {
                // Save values in reflected markup option spec
                // Get the markup option class associated with the enum value
                Object constant = enumFieldNameToCombo.get(enumName).getSelectedItem();
                Class optionClass = enumToConstantToClass.get(enumName).get(constant.toString());
                if (optionClass == null) {
                    continue;
                }
                HashMap<Field, ComplexMarkupCreationComponentP> valueComponentLookup = enumToConstantToFieldToCreationP.get(enumName).get(constant.toString());
                HashMap<Field, JComboBox> variableComboBoxLookup = enumToConstantToFieldToCombo.get(enumName).get(constant.toString());
                MarkupOption option = (MarkupOption) optionClass.newInstance();

                for (Field optionField : variableComboBoxLookup.keySet()) {
                    Object definition = null;
                    JComboBox variableComboBox = variableComboBoxLookup.get(optionField);
                    if (!variableComboBox.getSelectedItem().toString().equalsIgnoreCase(Event.NONE)) {
                        // User selected a variable other than NONE to define the field - store the variable name
                        option.addVariable(variableComboBox.getSelectedItem().toString(), optionField);
                    } else {
                        // User selected NONE for variable, now see if they created a value definition
                        ComplexMarkupCreationComponentP creationP = valueComponentLookup.get(optionField);
                        if (creationP != null) {
                            // Store the value from the component
                            definition = creationP.getValue();
                            optionField.set(option, definition);
                        }
                    }
                }
                // Store refl markup option spec
                String markupValueFieldName = constantToFieldName.get((Enum) constant);
                fieldNameToDefinition.put(markupValueFieldName, option);
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
        }

        return fieldNameToDefinition;
    }

    public static void main(String argv[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                ReflectedMarkupD dialog = new ReflectedMarkupD(null, true, new ReflectedMarkupSpecification("sami.markup.Attention"), new MissionPlanSpecification("Anon"));
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
