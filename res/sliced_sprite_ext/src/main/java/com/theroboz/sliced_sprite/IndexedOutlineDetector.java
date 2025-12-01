package com.theroboz.sliced_sprite;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class IndexedOutlineDetector
{
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

    public static Rect[] detect(File file) throws IOException
    {
        BufferedImage img = ImageIO.read(file);
        if (!(img.getColorModel() instanceof IndexColorModel)) throw new IllegalArgumentException("Must be indexed-color PNG");
        IndexColorModel cm = (IndexColorModel) img.getColorModel();
        int transparent = cm.getTransparentPixel(); // usually -1

        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] visited = new boolean[h][w];

        // Only marks border pixels as visited
        List<Rect> result = new ArrayList<>();
        for (int y = 0; y < h-8; y++)
        {
            for (int x = 0; x < w-8; x++)
            {
                if (visited[y][x])
                {
                    continue;
                }

                int idx = pixels[y * w + x] & 0xFF;
                if (idx == transparent || idx == 0)
                {
                    continue;
                }

                //System.out.println("Testing rectangle " + x +" "+ y);

                // Try to detect rectangle with top border starting at (x,y)
                Rect r = tryFindRectangle(pixels, w, h, x, y, (byte)idx, visited);
                if (r != null)
                {
                    result.add(r);
                    System.out.println("Found rectangle " + r.x +" "+ r.y +" " + r.width +" " + r.height);
                    x+=r.width;
                }
            }
        }

        return result.toArray(new Rect[0]);
    }

    private static Rect tryFindRectangle(byte[] pixels, int imgW, int imgH, int startX, int startY, byte color, boolean[][] visited)
    {
        // 1. Find right end of top horizontal border
        int rightX = startX;

        while (rightX < imgW - 1 &&
            ((pixels[startY * imgW + rightX + 1] == color) || (visited[startY][rightX+1]))) rightX++;

        int minSize = 7;
        if (rightX - startX < minSize)
        {
            //System.out.println("Rejected small rectangle from " + startX +" "+ startY +" to "+ rightX + " "+ startY +" w " + (rightX - startX + 1));
            return null;
        }

        // 2. Find matching bottom border (same length, same color)
        int bottomY = -1;
        for (int y = startY + minSize; y < imgH; y++)
        {
            if (isFullHorizontal(pixels, imgW, startX, rightX, y, color))
            {
                bottomY = y;
                break;
            }
        }

        if (bottomY == -1) {
            return null;
        }

        // 3. Verify left and right vertical borders are complete
        if (!isFullVertical(pixels, imgW, startX, startY, bottomY, color))
        {
            return null;
        }

        if (!isFullVertical(pixels, imgW, rightX, startY, bottomY, color))
        {
            return null;
        }

        // Success! Mark all border pixels as visited
        markBorder(visited, startX, startY, rightX, bottomY);
        int width = rightX - startX + 1;
        int height = bottomY - startY + 1;
        return new Rect(startX, startY, width, height, color & 0xFF);
    }

    private static boolean isFullHorizontal(byte[] p, int w, int x1, int x2, int y, byte c)
    {
        for (int x = x1; x <= x2; x++)
        {
            byte pixel = p[y * w + x];
            byte next = x<x2 ? p[y * w + x+1] : -1;
            if ((pixel != c) && (next!= c))
            {
                if (pixel > 0)
                    System.out.println("Incomplete horizontal border at " + x + " " + y + " color " + pixel + " lookign for " + c);
                return false;
            }
        }
        return true;
    }

    private static boolean isFullVertical(byte[] p, int w, int x, int y1, int y2, byte c)
    {
        for (int y = y1; y <= y2; y++)
        {
            byte pixel = p[y * w + x];
             byte next = y<y2 ? p[(y+1) * w + x] : -1;
            if ((pixel != c) && (next!= c))
            {
                if (pixel > 0)
                    System.out.println("Incomplete vertical border at " + x + " " + y+ " color " + pixel + " lookign for " + c);
                return false;
            }
        }
        return true;
    }

    private static void markBorder(boolean[][] v, int x1, int y1, int x2, int y2)
    {
        // top + bottom
        for (int x = x1; x <= x2; x++)
        {
            v[y1][x] = true;
            v[y2][x] = true;
        }

        // left + right (skip corners already marked)
        for (int y = y1 + 1; y < y2; y++)
        {
            v[y][x1] = true;
            v[y][x2] = true;
        }
    }

}
