package useful.be.can.useful.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by fruitware on 2/16/16.
 */
public class HelperClass {

    public static int getPx(Context context, int  dp){
        try {
            Resources r = context.getResources();
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());

            return (int) px;
        } catch (Exception e){
            e.printStackTrace();
            return dp;
        }
    }
}
