package jp.kyori.tusky;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RichTextUtil {

    public static Spanned replaceSpanned (Spanned targetText, String replaceBefore, String replaceAfter){
        String targetString = targetText.toString();
        SpannableStringBuilder builder = new SpannableStringBuilder(targetText);
        Pattern pattern = Pattern.compile(replaceBefore);
        Matcher matcher = pattern.matcher(targetString);
        while (matcher.find()){
            builder.replace(matcher.start(), matcher.end(), replaceAfter);
        }
        return builder;
    }

}