package org.example.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtility {

    public static List<String> findAllRegexMatches(String input, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        ArrayList allMatches = new ArrayList();
        //Matching the compiled pattern in the String
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            allMatches.add(matcher.group());
        }
        return allMatches;
    }

}
