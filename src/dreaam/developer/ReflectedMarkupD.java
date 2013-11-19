package dreaam.developer;

import sami.markup.Markup;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import sami.markup.OptionsDisplayFormat;

/**
 * Used to receive parameters from operator needed by an Output event
 *
 * @author pscerri
 */
public class ReflectedMarkupD extends javax.swing.JDialog {

    ArrayList<Field> requiredFields = null;
    HashMap<Field, Object> fieldValues = null;
    private final Markup markup;
    public static final int RECURSION_DEPTH = 2;

    public static void main(String argv[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                OptionsDisplayFormat markup = new OptionsDisplayFormat();
                ReflectedMarkupD dialog = new ReflectedMarkupD(markup, null, true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    /**
     * Creates new form ReflectedMarkupD
     */
    public ReflectedMarkupD(Markup markup, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("ReflectedMarkupD");

        mainP.setLayout(new GridLayout(0, 2));
        this.markup = markup;
        fieldValues = markup.getFieldValues();
        if (fieldValues == null) {
            fieldValues = new HashMap<Field, Object>();
        }
        addParamFields(markup, fieldValues);
    }

    /**
     * Create JComponents to hold the values of parameters
     *
     * @param obj
     * @param existingValues
     */
    protected void addParamFields(Markup obj, HashMap<Field, Object> existingValues) {
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "ReflectedMarkupD adding fields for " + obj + ", fields: " + obj.getFieldValues());

        for (Field f : obj.getFieldValues().keySet()) {
            try {
                addParamField(obj, f.getName(), f, 0, existingValues);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ReflectedMarkupD add threw exception " + ex, this);
                ex.printStackTrace();
                mainP.add(new JLabel("Uneditable"));
            }

        }
    }

    private void addParamField(Object o, String n, Field f, int depth, HashMap<Field, Object> localParams) {

        // @todo This is here to prevent infinite recursion, but is ugly
        if (depth > RECURSION_DEPTH) {
            return;
        }

        java.lang.reflect.Type t = f.getGenericType();
        if (t == String.class || t == Double.class || t == Integer.class || t == long.class || t == double.class || t == int.class || (t instanceof Class && ((Class) t).isEnum())) {
            simpleAddParamField(n + ":" + f.getName(), f, localParams);

        } else if (t instanceof Class && ((Class) t).isArray() && ((Class) t).getComponentType() == String.class) {
            // System.out.println("ReflectedMarkupD got String [] ");
            simpleAddParamField(n + ":" + f.getName(), f, localParams);

        } else {
            compoundAddParamField(n + ":" + f.getName(), f, localParams, depth);
        }
    }

    private void compoundAddParamField(String n, Field f, HashMap<Field, Object> localParams, int depth) {
        java.lang.reflect.Type t = f.getGenericType();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "ReflectedMarkupD handling compound type: " + t);
        try {
            // Create a sub hashmap to hold the params for this compound object
            HashMap<Field, Object> subParams = (HashMap<Field, Object>) localParams.get(f);
            if (subParams == null) {
                subParams = new HashMap<Field, Object>();
                localParams.put(f, subParams);
            }

            // Add the compound to allow variables to be selected
            // Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Before _add: " + c.getDeclaredFields().length, this);
            // Allow a variable to be used for a compound object
            // Clone is not strictly necessary - right now - because mediator is creating a new ArrayList each time
            mainP.add(new JLabel(n + ":" + f.getName()));

            ArrayList<String> variables = null;
            try {
                variables = (ArrayList<String>) (new Mediator()).getAllVariables().clone();
            } catch (NullPointerException e) {
                variables = new ArrayList<String>();
            }
            variables.add(0, "None");
            JComboBox cb = new JComboBox(variables.toArray());

            Object prevValue = localParams.get(f);
            if (prevValue != null) {
                cb.setSelectedItem(prevValue);
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Setting previous value for " + f + " to " + prevValue);
                if (prevValue instanceof HashMap && ((HashMap) prevValue).containsKey(f)) {
                    Object varValue = ((HashMap) prevValue).get(f);
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, "HAVE VARIABLE: " + varValue);
                    cb.setSelectedItem(varValue);
                }
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No previous value for " + f);
            }

            mainP.add(cb);
            subParams.put(f, cb);

            if (t instanceof Class) {
                Class c = (Class) t;

                localParams.put(f, subParams);

                for (Field fld : c.getDeclaredFields()) {
                    try {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Processing " + fld);

                        fld.setAccessible(true);
                        addParamField(null, n + ":" + f.getName(), fld, depth + 1, subParams);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, ">>>>>>> ReflectedMarkupD Failed to get field: " + ex, this);
                    }
                }
            }

        } catch (ClassCastException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "ReflectedMarkupD could not cast " + t + " to a class", this);
        }
    }

    private void simpleAddParamField(String n, Field f, HashMap<Field, Object> localParams) {
        mainP.add(new JLabel(n));
        try {

            java.lang.reflect.Type type = f.getGenericType();
            Object prevValue = localParams.get(f);
            if (prevValue != null) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "ReflectedMarkupD has previous value to use for " + f + " -> " + prevValue);
            }

            if (type == String.class || type == Double.class || type == Integer.class || type == double.class || type == int.class || type == long.class) {
                JTextField tf = new JTextField();
                if (prevValue != null) {
                    tf.setText(prevValue.toString());
                }
                mainP.add(tf);
                localParams.put(f, tf);
            } else if (((Class) type).isArray() && ((Class) type).getComponentType() == String.class) {
                JTextArea ta = new JTextArea();
                if (prevValue != null) {
                    String[] list = (String[]) prevValue;
                    StringBuffer sb = new StringBuffer();
                    for (String string : list) {
                        sb.append(string + "\n");
                    }
                    ta.setText(sb.toString());
                }
                mainP.add(ta);
                localParams.put(f, ta);
            } else if (((Class) type).isEnum()) {
                // System.out.println("Enum ... ");                                
                JComboBox cf = new JComboBox(((Class) type).getEnumConstants());
                mainP.add(cf);
                if (prevValue != null) {
                    cf.setSelectedItem(prevValue);
                }
                localParams.put(f, cf);

            } else {
            }
        } catch (Exception ex) {

            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ReflectedMarkupD exception: " + ex, this);
            ex.printStackTrace();

            mainP.add(new JLabel("Uneditable"));
        }
    }

    /**
     * This takes stuff from the various swing components and make them
     * available.
     *
     * @return
     */
    HashMap<Field, Object> getValuesFromComponents(HashMap<Field, Object> ps) {

        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "ReflectedMarkupD cloning from " + ps.keySet());

        HashMap<Field, Object> clone = new HashMap<Field, Object>(); // (HashMap<Field, Object>) params.clone();

        // Fill in the fields
        for (Field fld : ps.keySet()) {
            Object o = ps.get(fld);
            if (o instanceof JTextField) {

                String s = ((JTextField) o).getText();

                if (s.length() > 0) {
                    if (fld.getType() == Double.class || fld.getType() == double.class) {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Using type double");
                        clone.put(fld, Double.parseDouble(s));
                    } else if (fld.getType() == Integer.class || fld.getType() == int.class) {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Using type integer");
                        clone.put(fld, Integer.parseInt(s));
                    } else if (fld.getType() == Long.class || fld.getType() == long.class) {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Using type long");
                        clone.put(fld, Long.parseLong(s));
                    } else {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Using string due to " + fld.getType());
                        clone.put(fld, s);
                    }
                } else {
                    // This allows us to check for parameters that have not been instantiated
                    clone.put(fld, null);
                }

            } else if (o instanceof JTextArea) {
                String t = ((JTextArea) o).getText();
                if (t.length() > 0) {
                    StringTokenizer st = new StringTokenizer(t, "\n");
                    ArrayList<String> itemsA = new ArrayList<String>();
                    while (st.hasMoreElements()) {
                        String tok = st.nextToken();
                        itemsA.add(tok);
                        // System.out.println("Got: " + tok);
                    }
                    String[] items = new String[itemsA.size()];
                    itemsA.toArray(items);
                    clone.put(fld, items);
                }

            } else if (o instanceof JComboBox) {
                // @todo No way not to select something in combo box
                Object selectedObj = ((JComboBox) o).getSelectedItem();
                clone.put(fld, selectedObj);

                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Combo for " + fld + " value = " + selectedObj);

            } else if (o instanceof HashMap) {
                // System.out.println("Iterative ... for " + fld);
                clone.put(fld, getValuesFromComponents((HashMap<Field, Object>) o));

            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, ">>>>>>>>>>>>>> ReflectedMarkupD: Unhandled type: " + o.getClass() + " for " + fld, this);
            }
        }

        return clone;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        mainP = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout mainPLayout = new org.jdesktop.layout.GroupLayout(mainP);
        mainP.setLayout(mainPLayout);
        mainPLayout.setHorizontalGroup(
            mainPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 520, Short.MAX_VALUE)
        );
        mainPLayout.setVerticalGroup(
            mainPLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 310, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(mainP);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1)
                    .add(layout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 136, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(jScrollPane1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(okButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        setVisible(false);
        HashMap<Field, Object> ps = getValuesFromComponents(fieldValues);

        // System.out.println("ReflectedMarkupD set instance params to " + ps);

        markup.setFieldValues(ps);

    }//GEN-LAST:event_okButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainP;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
}
