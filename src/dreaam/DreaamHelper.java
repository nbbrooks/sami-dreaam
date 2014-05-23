package dreaam;

import java.util.ArrayList;

/**
 *
 * @author nbb
 */
public class DreaamHelper {

    public static String getUniqueName(String name, ArrayList<String> existingNames) {
        boolean invalidName = existingNames.contains(name);
        while (invalidName) {
            int index = name.length() - 1;
            if ((int) name.charAt(index) < (int) '0' || (int) name.charAt(index) > (int) '9') {
                // name does not end with a number - attach a "2"
                name += "2";
            } else {
                // Find the number the name ends with and increment it
                int numStartIndex = -1, numEndIndex = -1;
                while (index >= 0) {
                    if ((int) name.charAt(index) >= (int) '0' && (int) name.charAt(index) <= (int) '9') {
                        if (numEndIndex == -1) {
                            numEndIndex = index;
                        }
                    } else if (numEndIndex != -1) {
                        numStartIndex = index + 1;
                        break;
                    }
                    index--;
                }
                int number = Integer.parseInt(name.substring(numStartIndex, numEndIndex + 1));
                name = name.substring(0, numStartIndex) + (number + 1);
            }
            invalidName = existingNames.contains(name);
        }
        return name;
    }
}
