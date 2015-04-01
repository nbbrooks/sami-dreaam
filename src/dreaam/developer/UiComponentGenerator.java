package dreaam.developer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Logger;
import sami.config.DomainConfigManager;
import sami.uilanguage.UiComponentGeneratorInt;

/**
 *
 * @author nbb
 */
public class UiComponentGenerator {

    private final static Logger LOGGER = Logger.getLogger(UiComponentGenerator.class.getName());
    private static UiComponentGeneratorInt instance = null;

    public static synchronized UiComponentGeneratorInt getInstance() {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

    private static UiComponentGeneratorInt createInstance() {
        try {
            ArrayList<String> list = (ArrayList<String>) DomainConfigManager.getInstance().getDomainConfiguration().componentGeneratorList.clone();
            for (String className : list) {
                Class uiClass = Class.forName(className);
                Method factoryMethod = uiClass.getDeclaredMethod("getInstance");
                Object singleton = factoryMethod.invoke(null, null);
                if (singleton instanceof UiComponentGeneratorInt) {
                    return (UiComponentGeneratorInt) singleton;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        LOGGER.severe("Failed to create instance of UiComponentGeneratorInterface");
        return null;
    }
}
