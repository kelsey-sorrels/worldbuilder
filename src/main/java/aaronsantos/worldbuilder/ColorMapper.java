package aaronsantos.worldbuilder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import processing.core.PApplet;
import processing.core.PImage;

public class ColorMapper
{

    final PApplet applet;
    final PImage c, e, r, t;

    private class Entry
    {

        float x, y, z, w;
        int c;

        Entry(float precipitation, float elevation, float temperature, float lattitude, int c)
        {
            x = precipitation;
            y = elevation;
            z = temperature;
            w = lattitude;
            this.c = c;
        }

        float distance(Entry e)
        {
            return PApplet.sqrt(PApplet.sq(e.x - x) + PApplet.sq(e.y - y) + PApplet.sq(e.z - z) + PApplet.sq(e.w - w));
        }
    }
    // colorMap [precipitation][elevation][temperature][lattitude] => color
    List<Entry> colorMap = new ArrayList<Entry>();

    public ColorMapper(PApplet applet, PImage ci, PImage ei, PImage ri, PImage ti)
    {
        this.applet = applet;
        c = ci;
        e = ei;
        r = ri;
        t = ti;

        processR();
        processT();
        processE();
        loadColorMap();
    }

    boolean close(float value, float target)
    {
        return (Math.abs(value - target) < 10);
    }

    // See http://people.oregonstate.edu/~jennyb/pdf/2006_JennyHurni_SwissStyleShading.pdf
    public int colorMapQuery(float precipitation, float elevation, float temperature, float lattitude)
    {
        float k = 0.0016f;
        float rWC = 0;
        float gWC = 0;
        float bWC = 0;

        float rW = 0;
        float gW = 0;
        float bW = 0;

        Entry sample = new Entry(precipitation, elevation, temperature, lattitude, applet.color(0));

        for (Entry e : colorMap)
        {
            float d = e.distance(sample);
            //float w = exp(-k*sq(d));
            float w = 1 / (-k * PApplet.sq(d));
            rWC += w * applet.red(e.c);
            gWC += w * applet.green(e.c);
            bWC += w * applet.blue(e.c);

            rW += w;
            gW += w;
            bW += w;
        }

        return applet.color(rWC / rW, gWC / gW, bWC / bW);
    }

    private void processR()
    {
        r.loadPixels();
        for (int i = 0; i < r.pixels.length; i++)
        {
            int pRed = (int) applet.red(r.pixels[i]);
            int pGreen = (int) applet.green(r.pixels[i]);
            int pBlue = (int) applet.blue(r.pixels[i]);

            if (pRed == 229 && pGreen == 229 && pBlue == 229)
            {
                r.pixels[i] = applet.color(10, 10, 10);
            }
            else if (close(pRed, 189) && pGreen == 189 && pBlue == 189)
            {
                r.pixels[i] = applet.color(20, 20, 30);
            }
            else if (close(pRed, 149) && pGreen == 149 && pBlue == 149)
            {
                r.pixels[i] = applet.color(30, 30, 30);
            }
            else if (pRed == 195 && pGreen == 111 && pBlue == 109)
            {
                r.pixels[i] = applet.color(40, 40, 40);
            }
            else if (pRed == 199 && pGreen == 178 && pBlue == 149)
            {
                r.pixels[i] = applet.color(50, 50, 50);
            }
            else if (pRed == 254 && pGreen == 198 && pBlue == 116)
            {
                r.pixels[i] = applet.color(60, 60, 60);
            }
            else if (pRed == 254 && pGreen == 254 && pBlue == 83)
            {
                r.pixels[i] = applet.color(70, 70, 70);
            }
            else if (pRed == 144 && pGreen == 254 && pBlue == 152)
            {
                r.pixels[i] = applet.color(80, 80, 80);
            }
            else if (pRed == 0 && pGreen == 254 && pBlue == 0)
            {
                r.pixels[i] = applet.color(90, 90, 90);
            }
            else if (pRed == 63 && pGreen == 198 && pBlue == 55)
            {
                r.pixels[i] = applet.color(100, 100, 100);
            }
            else if (pRed == 12 && pGreen == 149 && pBlue == 4)
            {
                r.pixels[i] = applet.color(110, 110, 110);
            }
            else if (pRed == 4 && pGreen == 111 && pBlue == 93)
            {
                r.pixels[i] = applet.color(120, 120, 120);
            }
            else if (pRed == 254 && pGreen == 0 && pBlue == 254)
            {
                r.pixels[i] = applet.color(130, 130, 130);
            }
            else if (pRed == 127 && pGreen == 0 && pBlue == 127)
            {
                r.pixels[i] = applet.color(140, 140, 140);
            }
            else if (pRed == 0 && pGreen == 137 && pBlue == 254)
            {
                r.pixels[i] = applet.color(150, 150, 150);
            }
            else
            {
                r.pixels[i] = applet.color(1, 0, 0, 0);
            }
        }
        r.updatePixels();
    }

    private void processT()
    {
        t.loadPixels();
        for (int i = 0; i < t.pixels.length; i++)
        {
            int pRed = (t.pixels[i] >> 16) & 0xFF;
            int pGreen = (t.pixels[i] >> 8) & 0xFF;
            int pBlue = (t.pixels[i]) & 0xFF;

            if (close(pRed, 158) && close(pGreen, 158) && close(pBlue, 158))
            {
                t.pixels[i] = applet.color(10, 10, 10);
            }
            else if (close(pRed, 190) && close(pGreen, 190) && close(pBlue, 190))
            {
                t.pixels[i] = applet.color(20, 20, 30);
            }
            else if (close(pRed, 218) && close(pGreen, 218) && close(pBlue, 218))
            {
                t.pixels[i] = applet.color(30, 30, 30);
            }
            else if (close(pRed, 136) && close(pGreen, 198) && close(pBlue, 228))
            {
                t.pixels[i] = applet.color(40, 40, 40);
            }
            else if (close(pRed, 0) && close(pGreen, 238) && close(pBlue, 253))
            {
                t.pixels[i] = applet.color(50, 50, 50);
            }
            else if (close(pRed, 136) && close(pGreen, 238) && close(pBlue, 143))
            {
                t.pixels[i] = applet.color(60, 60, 60);
            }
            else if (close(pRed, 232) && close(pGreen, 253) && close(pBlue, 143))
            {
                t.pixels[i] = applet.color(70, 70, 70);
            }
            else if (close(pRed, 253) && close(pGreen, 253) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(80, 80, 80);
            }
            else if (close(pRed, 179) && close(pGreen, 141) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(90, 90, 90);
            }
            else if (close(pRed, 253) && close(pGreen, 141) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(100, 100, 100);
            }
            else if (close(pRed, 253) && close(pGreen, 83) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(110, 110, 110);
            }
            else if (close(pRed, 253) && close(pGreen, 0) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(120, 120, 120);
            }
            else if (close(pRed, 192) && close(pGreen, 0) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(130, 130, 130);
            }
            else if (close(pRed, 96) && close(pGreen, 0) && close(pBlue, 0))
            {
                t.pixels[i] = applet.color(140, 140, 140);
            }
            else if (close(pRed, 140) && close(pGreen, 140) && close(pBlue, 140))
            {
                t.pixels[i] = applet.color(150, 150, 150);
            }
            else
            {
                t.pixels[i] = applet.color(1, 0, 0, 0);
            }
            //t.pixels[i] = color(red(t.pixels[i]), green(t.pixels[i]), blue(t.pixels[i]), 127);
        }
        t.updatePixels();
    }

    private void processE()
    {
        e.loadPixels();
        for (int i = 0; i < e.pixels.length; i++)
        {
            e.pixels[i] = applet.color(applet.red(e.pixels[i]), applet.green(e.pixels[i]), applet.blue(e.pixels[i]), 0);
        }
        e.updatePixels();
    }

    private void addToColorMap(final Entry e)
    {
        boolean addToColorMap = true;
        for (Entry entry : colorMap)
        {
            if (e.distance(entry) < 10)
            {
                addToColorMap = false;
                break;
            }
        }
        if (addToColorMap)
        {
            colorMap.add(e);
        }
    }

    private void loadColorMap()
    {
        c.loadPixels();
        e.loadPixels();
        r.loadPixels();
        t.loadPixels();

        int x = 0;
        int y = 0;
        for (int i = 0; i < c.pixels.length || i < e.pixels.length || i < r.pixels.length || i < t.pixels.length; i++, x++)
        {
            if (x >= c.width || x >= e.width || x >= r.width || x >= t.width)
            {
                // End of image, new line.
                x = 0;
                y++;
            }
            if (x + y * c.width > c.pixels.length
                    || x + y * e.width > e.pixels.length
                    || x + y * r.width > r.pixels.length
                    || x + y * t.width > t.pixels.length)
            {
                break;
            }

            try
            {
                int cc = c.pixels[x + y * c.width];
                int ce = e.pixels[x + y * e.width];
                int cr = r.pixels[x + y * r.width];
                int ct = t.pixels[x + y * t.width];

                if (applet.red(cr) == applet.green(cr) && applet.green(cr) == applet.blue(cr)
                        && applet.red(ct) == applet.green(ct) && applet.green(ct) == applet.blue(ct))
                {

                    float lattitude = (float) y / c.height;
                    float elevation = applet.red(ce);
                    float precipitation = applet.red(cr);
                    float temperature = applet.red(ct);
                    // color [precipitation][elevation][temperature][lattitude]
                    addToColorMap(new Entry(precipitation, elevation, temperature, lattitude, cc));
                }
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                PApplet.println("x:" + x + " y:" + y);
            }
        }
    }

    void printColorMap(int x, int y)
    {
        for (int i = 0; i < 150; i++)
        {
            //println("printing line "+i+" of 150...");
            for (int j = 0; j < 150; j++)
            {
                // color [precipitation][elevation][temperature][lattitude]
                applet.set(x + i, y + j, colorMapQuery(70, i, j, 0.5f));
            }
        }
    }
}
