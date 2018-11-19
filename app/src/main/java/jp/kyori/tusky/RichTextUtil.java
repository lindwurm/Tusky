package jp.kyori.tusky;

import android.graphics.Paint;

public class RichTextUtil {

    public static class GizaText {

        private Paint counter;

        private final String TOP_LEFT_STRING = "＿";
        private final String TOP_LOOP_STRING = "人";
        private final String TOP_RIGHT_STRING = "＿";
        private final String LEFT_LOOP_STRING = "＞";
        private final String TEXT_LOOP_STRING = "　";
        private final String RIGHT_LOOP_STRING = "＜";
        private final String BOTTOM_LEFT_STRING = "￣";
        private final String BOTTOM_START_STRING = "Y";
        private final String BOTTOM_LOOP_STRING = "^Y";
        private final String BOTTOM_RIGHT_STRING = "￣";

        public GizaText(){
            // Create text length measure
            counter = new Paint();
            counter.setTextSize(16);
        }

        public String enrich(String text) {
            String[] lines = text.split("\n");

            // Check maximum width
            float lineWidth = 0f;
            for (String line : lines) {
                float compareWidth = counter.measureText(line);
                if (compareWidth > lineWidth) {
                    lineWidth = compareWidth;
                }
            }

            StringBuilder builder = new StringBuilder();
            builder.append(generateTopString(lineWidth)).append("\n");
            for (String line: lines){
                builder.append(generateMiddleString(lineWidth, line)).append("\n");
            }
            builder.append(generateBottomString(lineWidth));
            return builder.toString();
        }

        private String generateTopString(float targetWidth) {
            return TOP_LEFT_STRING + doLoopString(targetWidth, TOP_LOOP_STRING) + TOP_RIGHT_STRING;
        }

        private String generateMiddleString(float targetWidth, String lineText){
            return LEFT_LOOP_STRING + addLoopString(targetWidth, lineText) + RIGHT_LOOP_STRING;
        }
        
        private String generateBottomString(float targetWidth){
            float startStringWidth = counter.measureText(BOTTOM_START_STRING);
            return BOTTOM_LEFT_STRING + BOTTOM_START_STRING + doLoopString(targetWidth - startStringWidth, BOTTOM_LOOP_STRING) + BOTTOM_RIGHT_STRING;
        }

        private String doLoopString(float targetWidth, String loopStr) {
            String text = "";
            do {
                text += loopStr;
            } while (!(counter.measureText(text) >= targetWidth));
            return text;
        }

        private String addLoopString(float targetWidth, String startStr) {
            String text = startStr;
            while (true) {
                if (counter.measureText(text) >= targetWidth) {
                    break;
                } else {
                    text += TEXT_LOOP_STRING;
                }
            }
            return text;
        }
    }

}
