package xyz.emlyn.snapnote;

import android.content.Context;

public class Constants {
    /* Format for storing notes:
    *   ([note_id]SEP1[note_text]SEP2[note_timestamp]SEP3[note_color]SEP4[tag]HS_SEP)*[tag1][tag2][tag3][tag4]
    *   [tag1] binary (0x0 - 0x4)
    */

    public static char SEP1 = '\ue000';
    public static char SEP2 = '\ue001';
    public static char SEP3 = '\ue002';
    public static char SEP4 = '\ue003';

    public static char HS_SEP = '\ue005';

    public static int[][] getNoteGradients(Context context) {
        return new int[][] {
                context.getResources().getIntArray(R.array.gradient_1),
                context.getResources().getIntArray(R.array.gradient_2),
                context.getResources().getIntArray(R.array.gradient_3),
                context.getResources().getIntArray(R.array.gradient_4),

        };
    }

    public static int[][] getTagChoices(Context context) {
        return new int[][] {
                context.getResources().getIntArray(R.array.tagColor_r1),
                context.getResources().getIntArray(R.array.tagColor_r2),
                context.getResources().getIntArray(R.array.tagColor_r3),
                context.getResources().getIntArray(R.array.tagColor_r4),
        };
    }

}


/* TODO: WISHLIST

- prev color validation

 */