package dreaam.developer;

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
import sami.event.Event;
import sami.event.ReflectionHelper;
import sami.markup.Markup;
import sami.markup.MarkupOption;
import sami.markup.ReflectedMarkupSpecification;
import sami.mission.MissionPlanSpecification;
import sami.uilanguage.MarkupComponent;

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
    private HashMap<String, JPanel> enumFieldNameToPanel;
    private HashMap<String, JComboBox> enumFieldNameToCombo;
    private HashMap<String, HashMap<String, JPanel>> enumToConstantToPanel;
    private HashMap<String, HashMap<String, Class>> enumToConstantToClass;
    private HashMap<String, HashMap<String, HashMap<Field, MarkupComponent>>> enumToConstantToFieldToComp;
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
    private final Mediator mediator = new Mediator();

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
        enumToConstantToFieldToComp = new HashMap<String, HashMap<String, HashMap<Field, MarkupComponent>>>();
        enumToConstantToFieldToCombo = new HashMap<String, HashMap<String, HashMap<Field, JComboBox>>>();
        enumToConstantToFieldToDefinition = new HashMap<String, HashMap<String, HashMap<Field, Object>>>();

        try {
            markupClass = Class.forName(markupSpec.getClassName());
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        initComponents();
        setTitle("ReflectedMarkupD");
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
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridy = 0;
            constraints.gridx = 0;
            constraints.weightx = 1.0;

            // Add components for each Markup enums
            for (String enumFieldName : enumFieldNames) {
                try {
                    final Field enumField = ReflectionHelper.getField(markupClass, enumFieldName);
                    if (enumFieldName == null) {
                        LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markupClass.getSimpleName() + " or any super class");
                        continue;
                    }
                    if (enumField.getType().isEnum()) {
                        // Initialize hashmaps
                        enumToConstantToPanel.put(enumFieldName, new HashMap<String, JPanel>());
                        enumToConstantToClass.put(enumFieldName, new HashMap<String, Class>());
                        HashMap<String, HashMap<Field, MarkupComponent>> valueToFieldToComp = new HashMap<String, HashMap<Field, MarkupComponent>>();
                        HashMap<String, HashMap<Field, JComboBox>> valueToFieldToCombo = new HashMap<String, HashMap<Field, JComboBox>>();
                        HashMap<String, HashMap<Field, Object>> constantToFieldToDefinition = new HashMap<String, HashMap<Field, Object>>();
                        for (Object constant : enumField.getType().getEnumConstants()) {
                            valueToFieldToComp.put(constant.toString(), new HashMap<Field, MarkupComponent>());
                            valueToFieldToCombo.put(constant.toString(), new HashMap<Field, JComboBox>());
                            constantToFieldToDefinition.put(constant.toString(), new HashMap<Field, Object>());
                        }
                        enumToConstantToFieldToComp.put(enumFieldName, valueToFieldToComp);
                        enumToConstantToFieldToCombo.put(enumFieldName, valueToFieldToCombo);
                        enumToConstantToFieldToDefinition.put(enumFieldName, constantToFieldToDefinition);

                        // Add description for this enum to panel
                        JLabel description = new JLabel(enumNameToDescription.get(enumField.getName()), SwingConstants.LEFT);
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

                        // Add panel for each enum constant to handle the associated MarkupOption (if there is one)
                        for (Object constant : enumField.getType().getEnumConstants()) {
                            JPanel constantPanel = constructConstantPanel(enumField, constant);
                            maxComponentWidth = Math.max(maxComponentWidth, constantPanel.getPreferredSize().width);
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

                    } else {
                        LOGGER.severe("Markup class field " + enumField.getName() + " is not an enum");
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
            HashMap<String, HashMap<Field, MarkupComponent>> constantToFieldToComp = enumToConstantToFieldToComp.get(enumField.getName());
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
                            constantToFieldToComp.put(constant.toString(), new HashMap<Field, MarkupComponent>());
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
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridy = 0;
            constraints.gridx = 0;
            constraints.weightx = 1.0;

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
                JLabel description = new JLabel(optionFieldNameToDescription.get(optionFieldName), SwingConstants.LEFT);
                description.setMaximumSize(new Dimension(Integer.MAX_VALUE, description.getPreferredSize().height));
                constantPanel.add(description, constraints);
                constraints.gridy = constraints.gridy + 1;
                // Add combo box for selecting variable name
                addVariableComboBox(enumField.getName(), constant.toString(), optionField, constantPanel, constraints, option);
                constraints.gridy = constraints.gridy + 1;
                // Add component for defining value
                addValueComponent(enumField.getName(), constant.toString(), optionField, constantPanel, constraints, option);
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
        ArrayList<String> existingVariables = mediator.getProjectSpec().getVariables(optionField, mSpec);
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
        maxComponentWidth = Math.max(maxComponentWidth, comboBox.getPreferredSize().width);
        constantPanel.add(comboBox, constraints);
        enumToConstantToFieldToCombo.get(enumName).get(enumValueName).put(optionField, comboBox);
    }

    /**
     * Add component for creating a value for a MarkupOption field
     *
     * @param enumName
     * @param constantName
     * @param optionField
     * @param fieldToDefinition
     * @param constantPanel
     * @param constraints
     * @param option
     */
    protected void addValueComponent(String enumName, String constantName, Field optionField, JPanel constantPanel, GridBagConstraints constraints, MarkupOption option) {
        MarkupComponent markupComponent = UiComponentGenerator.getInstance().getCreationComponent(optionField.getType(), new ArrayList<Markup>());
        JComponent visualization = null;
        Object definition = null;
        if (option != null) {
            //            definition = option.getFieldDefinitions().get(optionField.getName());
            try {
                definition = optionField.get(option);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ReflectedMarkupD.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (markupComponent != null && definition != null) {
            // This field already has been defined with a value
            // Earlier we had to replace primitive fields with their wrapper object, take that into account here
            if (definition.getClass().equals(optionField.getType())
                    || (definition.getClass().equals(Double.class) && optionField.getType().equals(double.class))
                    || (definition.getClass().equals(Float.class) && optionField.getType().equals(float.class))
                    || (definition.getClass().equals(Integer.class) && optionField.getType().equals(int.class))
                    || (definition.getClass().equals(Long.class) && optionField.getType().equals(long.class))) {
                UiComponentGenerator.getInstance().setComponentValue(markupComponent, definition);
            }
        }
        if (markupComponent == null) {
            // There is no component that can be used to define a value for this field
            // The field can only be set to a variable name
            // Show a message for now, may remove this later...
            visualization = new JLabel("No component", SwingConstants.LEFT);
        } else {
            enumToConstantToFieldToComp.get(enumName).get(constantName).put(optionField, markupComponent);
            visualization = markupComponent.getComponent();
        }
        maxComponentWidth = Math.max(maxComponentWidth, visualization.getPreferredSize().width);
        constantPanel.add(visualization, constraints);
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        fieldNameToDefinition = markupSpec.getFieldDefinitions();
        if (fieldNameToDefinition == null) {
            fieldNameToDefinition = new HashMap<String, Object>();
        }
        paramsPanel = new javax.swing.JPanel();
        BoxLayout paramsLayout = new BoxLayout(paramsPanel, BoxLayout.Y_AXIS);
        paramsPanel.setLayout(paramsLayout);
        paramsPanel.setLayout(new GridBagLayout());
        addComponents();
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
                HashMap<Field, MarkupComponent> valueComponentLookup = enumToConstantToFieldToComp.get(enumName).get(constant.toString());
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
                        MarkupComponent valueComponent = valueComponentLookup.get(optionField);
                        if (valueComponent != null) {
                            // Store the value from the component
                            definition = UiComponentGenerator.getInstance().getComponentValue(valueComponent, optionField);
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
