package me.demetoir.a3dsound_ndk.util;

import android.graphics.Rect;
import android.util.Log;


public class Logger {
    private void rectLog(String Tag, Rect rect) {
        float a = rect.left;
        float b = rect.top;
        float c = rect.right;
        float d = rect.bottom;
        String str = String.format("%f %f %f %f", a, b, c, d);
        Log.i(Tag, "rectLog: " + str);
    }

    private void pointLog(String Tag, Point2D p) {
        String str = String.format("%f %f", p.x, p.y);
        Log.i(Tag, "pointLog: point " + str);
    }



}
