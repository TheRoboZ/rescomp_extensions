package com.theroboz.sliced_sprite;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class IndexedOutlineDetector
{

    //static int transparent;

    public static class Rect
    {
        public final int x, y, width, height;
        public final int colorIndex;

        public Rect(int x, int y, int w, int h, int colorIndex)
        {
            this.x = x;
            this.y = y;
            this.width = snap(w, 8);
            this.height = snap(h, 8);
            this.colorIndex = colorIndex;
        }

        public Rect(int x, int y, int w, int h)
        {
            this.x = x;
            this.y = y;
            this.width = snap(w, 8);
            this.height = snap(h, 8);
            this.colorIndex = 0;
        }

        private int snap(int v, int g)
        {
            if (v <= 0) return g;
            int lo = (v / g) * g;
            int hi = lo + g;
            return (v - lo) < (hi - v) ? lo : hi;
        }

        @Override public String toString()
        {
            return String.format("Rect[x=%d, y=%d, w=%d, h=%d, borderIndex=%d]", x, y, width, height, colorIndex);
        }
    }


    private static int transIdx;

    private static final int MIN_SIZE = 8;
    private static final int MAX_SIZE = 32;

    private static BufferedImage img;
    private static  byte[] pixels;
    private static int w, h;
    private static int cornerColor;

    private static boolean isSolid(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        return (pixels[y * w + x] & 0xFF) != transIdx;
    }

    private static int getColor(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return transIdx;
        return (pixels[y * w + x] & 0xFF);
    }

    // Checkerboard: valid if current or next pixel is solid
    private static boolean hasColumnPixel(int x, int y) {
        return (getColor(x, y) == cornerColor || getColor(x, y+1) == cornerColor);
    }

    // Checkerboard: valid if current or next pixel is solid
    private static boolean hasTopborderPixel(int x, int y) {
        return (getColor(x, y) == cornerColor) || (getColor(x + 1, y) == cornerColor);
    }

    private static Rect tryExtend(int sx, int sy) {

        int rx = sx+1;
        while (rx < w && hasTopborderPixel(rx, sy)) rx++;
        if (!isSolid(rx, sy) || (getColor(rx,sy+1)!=cornerColor && getColor(rx, sy+2)!=cornerColor)) rx--;
        if (rx>sx)
        {
            if (rx - sx + 1 < MIN_SIZE) rx = sx + MIN_SIZE-1;
            //System.out.println("found top horizontal span from "+sx+", "+sy+" to " + rx + ", "+sy);
        }
        else return null;

        int ry = sy + 1;
        while (ry < h && hasColumnPixel(sx, ry)) ry++;
        if (!isSolid(sx, ry) || (getColor(sx+1,ry)!=cornerColor && getColor(sx, ry+2)!=cornerColor)) ry--;
        if (ry>sy)
        {
            if (ry - sy + 1 < MIN_SIZE) ry = sy + MIN_SIZE-1;
            //System.out.println("found left vertical span from "+sx+", "+sy+" to  " + sx + ", "+ry);
        }
        else return null;

        int dy = sy + 1;
          while (dy < ry) {
          if (!isSolid(rx, dy)) return null;
          dy++;
        }

        //System.out.println("found right vertical span from "+rx+", "+sy+" to " + dy );

        // Verify bottom edge
        int dx = sx+1;
        while (dx < rx) {
            if (!isSolid(dx, ry)) return null;
            dx++;
        }

        //System.out.println("found bottom horizontal span from "+sx+", "+by+" to " + bx );
        //System.out.println("found RECT "+sx+", "+sy+", " + (rx - sx + 1) +", "+ (ry - sy + 1));

        return new Rect(sx, sy, rx - sx + 1, ry - sy + 1);
    }

    public static Rect[] detect(File file) throws IOException
    {
        img = ImageIO.read(file);
        if (!(img.getColorModel() instanceof IndexColorModel)) throw new IllegalArgumentException("Must be indexed-color PNG");
        IndexColorModel cm = (IndexColorModel) img.getColorModel();
        transIdx = cm.getTransparentPixel();
        if (transIdx == -1) throw new RuntimeException("No transparent color found");

        pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

        w = img.getWidth();
        h = img.getHeight();

        List<Rect> result = new ArrayList<>();

        for (int y = 0; y < h - MIN_SIZE; y++) {
            for (int x = 0; x < w - MIN_SIZE; x++) {

                cornerColor = getColor(x,y);

                // Geometric top-left corner
                if (cornerColor == transIdx) continue;
                if (getColor(x - 1, y) == cornerColor) continue;
                if (getColor(x - 2, y) == cornerColor) continue;
                if (getColor(x, y - 1) == cornerColor) continue;
                if (getColor(x, y - 2) == cornerColor) continue;

                //System.out.println("Possible TOP CORNER at "+x+", "+y);

                Rect raw = tryExtend(x, y);
                if (raw == null) continue;

                // Clamp to 8–32 range
                int clampedW = Math.max(MIN_SIZE, Math.min(MAX_SIZE, raw.width));
                int clampedH = Math.max(MIN_SIZE, Math.min(MAX_SIZE, raw.height));

                Rect clamped = new Rect(raw.x, raw.y, clampedW, clampedH);

                result.add(clamped);
                x += 1;
            }
        }
        return result.toArray(new Rect[0]);
    }

}
