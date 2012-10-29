package aaronsantos.worldbuilder;

import aaronsantos.worldbuilder.io.GeoJSONWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PVector;

public class WorldBuilder extends PApplet
{

    DrawMode drawMode = DrawMode.Geographical;
// Cell length in meters
    static final float l = 1;
// Cell area in meters^2
    static final float A = l * l;
// Cell volume in meters^3
    static final float V = A * l;
// Atmosphere extends 10000m above elevation
    static final float atmosphericHeight = 10000;
    World world = null;
    Vector<WorldSnapShot> snapShots = new Vector<WorldSnapShot>();
    WorldSnapShot snapShot = null;
    Thread t = null;
    PFont font;
    ColorMapper mapper;
    PImage c, e, r, temp;
    boolean writeGeoJSON = false;

    @Override
    public void setup()
    {
        size(600, 600);
        background(0);
        frameRate(60);

        world = new World(this, width, height);
        font = createFont("Arial Bold", 48);
        println("Analyzing colors");
        c = loadImage("color.png");
        e = loadImage("elevation.jpeg");
        r = loadImage("rainfall.png");
        temp = loadImage("temperature.png");

        c.resize(814, 463);
        e.resize(814, 470);
        r.resize(814, 400);
        temp.resize(814, 390);

        mapper = new ColorMapper(this, c, e, r, temp);

        println("Rendering");
        //noLoop();

        t = new Thread()
        {
            @Override
            public void run()
            {
                for (;;)
                {
                    world.step();
                    if (snapShots.size() > 3)
                    {
                        snapShots.clear();
                    }
                    snapShots.add(world.getSnapShot());
                }
            }
        };
        t.start();
    }

    @Override
    public synchronized void draw()
    {
        if (snapShots.size() > 0)
        {

            snapShot = snapShots.get(0);
            snapShots.clear();
        }

        if (snapShot == null)
        {
            return;
        }
        
        if (writeGeoJSON)
        {
            println("Writing GeoJSON snapshot to file...");
            writeGeoJSON = false;
            final GeoJSONWriter writer = new GeoJSONWriter();
            try
            {
                writer.write(snapShot);
                println("Finished.");
            }
            catch (Throwable e)
            {
                println("Error");
                println(e.toString());
                e.printStackTrace();
                Logger.getLogger(WorldBuilder.class.getName()).log(Level.SEVERE, "Error writing GeoJSON", e);
            }
        }
        background(0);
        switch (drawMode)
        {
            case Geographical:
                drawGeographical(snapShot);
                break;
            case Political:
                drawPolitical(snapShot);
        }
    }

    @Override
    public synchronized void keyPressed()
    {
        if (key == 'z')
        {
            switch (drawMode)
            {
                case Geographical:
                    drawMode = DrawMode.Political;
                    break;
                case Political:
                    drawMode = DrawMode.Geographical;
            }
        }
        else if (key == 's')
        {
            writeGeoJSON = true;
        }  
    }

    void drawGeographical(final WorldSnapShot snapShot)
    {
        loadPixels();
        int res = 4;
        for (float x = 0; x < width; x += res)
        {
            for (float y = 0; y < height; y += res)
            {
                float u = (float) x / width;
                float v = (float) y / height;
                int c = color(0, 0, 0);
                float landHeight = snapShot.getWorld(u, v);
                float waterHeight = snapShot.getWater(u, v);
                float snowHeight = snapShot.getSnow(u, v);
                float massWaterVapor = snapShot.getWaterVapor(u, v);
                float vegetation = snapShot.getVegetation(u, v);
                float elevation = landHeight + waterHeight;
                // Temperature for the given cell by height and by lattitude
                float Tk = Util.temperatureByHeightAndLattitudeAndTime(elevation, v, 0);

                if (snowHeight > 0.0)
                {
                    c = color(255, 255, 255);
                }
                // Else if water?
                else if (waterHeight > 0.1)
                {
                    //c = color(15, 60*5/(waterHeight/20+2)+20, 89*5/(waterHeight/20+3)+60);
                    int lightWater = color(86, 181, 188);
                    int mediumWater = color(12, 45, 58);
                    int darkWater = color(3, 5, 15);
                    if (waterHeight < 50)
                    {
                        c = lerpColor(lightWater, mediumWater, map(waterHeight, 0, 50, 0, 1));
                    }
                    else
                    {
                        c = lerpColor(mediumWater, darkWater, map(waterHeight, 50, 2500, 0, 1));
                    }
                }
                // Else land
                else
                {
                    // Not an edge pixel?
                    if (x > 2 && y > 2 && x < (width - 2) && y < (height - 2))
                    {
                        // Calculate slope 
                        PVector nx = new PVector(-1, 0, (snapShot.getWorld(u - (2.0f / width), v) - landHeight) / 200);
                        nx.normalize();
                        PVector ny = new PVector(0, -1, (snapShot.getWorld(u, v - (2.0f / height)) - landHeight) / 200);
                        ny.normalize();
                        PVector n = (nx).cross(ny);
                        PVector sun = new PVector(-1, -1, 0.5f);
                        sun.normalize();
                        //n*l lighting
                        float incidence = sun.dot(n);
                        //incidence = 0.5;
                        // c=n*l+a
                        float Kambient = 0.4f;
                        if (incidence > 0)
                        {
                            int d = color(128 * incidence, 128 * incidence, 128 * incidence);
                            c = color(80, 140, 90);
                            c = mapper.colorMapQuery(15 * vegetation, (elevation - 2500) / 20, Tk - 273, v);
                            c = color(8.0f * (red(c) - 95), 1.2f * (green(c) + 0), 1.2f * (blue(c) - 0));
                            //if (vegetation>0)
                            //  c = color(255, 0, 0);
                            c = blendColor(c, d, DODGE);
                        }
                        else
                        {
                            c = color(max(15 * -incidence + snowHeight * 150, Kambient * 15), max(23 * -incidence + snowHeight * 150, Kambient * 60), max(12 * -incidence + snowHeight * 150, Kambient * 140));
                        }
                    }
                }

                // The amount of water vapor in grams if the air was to be saturated.
                float waterVaporMassSat = Util.waterVaporPartialPressureToMass(atmosphericHeight * V,
                        Util.waterVaporSaturationThreshold(Util.pressurePaByHeightM(elevation), Tk), Tk);
                float humidity = massWaterVapor / waterVaporMassSat;

                final int cloudCover = constrain((int) (255.0 / (1 + Util.fastpow(2.3, -16 * humidity + 8)) - 10), 0, 255);
                c = blendColor(c, color(255, 255, 255, cloudCover), SCREEN);

                for (int i = 0; i < res; i++)
                {
                    for (int j = 0; j < res; j++)
                    {
                        pixels[((int) y + j) * width + (int) x + i] = c;
                    }
                }
            }
        }
        List<City> cities = snapShot.getCities();
        List<Citizen> citizens = snapShot.getCitizens();
        for (City city : cities)
        {
            for (int i = 0; i < constrain(city.getPopulation(), 2, 20); i++)
            {
                for (int j = 0; j < constrain(city.getPopulation(), 2, 20); j++)
                {
                    pixels[constrain(city.getY() + j, 0, height - 1) * width
                            + constrain(city.getX() + i, 0, width - 1)] = color(255, 0, 0);
                }
            }
        }
        int iconRes = 2;
        for (Citizen citizen : citizens)
        {
            //println(String.format("Drawing citizen @ %d, %d", citizen.getX(), citizen.getY()));

            for (int i = 0; i < iconRes; i++)
            {
                for (int j = 0; j < iconRes; j++)
                {
                    pixels[constrain(citizen.getY() + j, 0, height - 1) * width
                            + constrain(citizen.getX() + i, 0, width - 1)] = color(128, 0, 255);
                }
            }
        }
        updatePixels();
        //*
        //println("done rendering");
        textFont(font, 36);
        // white float frameRate
        fill(0);
        text(frameRate, 23, 33);
        fill(255);
        text(frameRate, 20, 30);

        //*
        if (mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height)
        {
            textFont(font, 12);
            float mu = (float) mouseX / width;
            float mv = (float) mouseY / height;


            float vegetation = snapShot.getVegetation(mu, mv);
            float snow = snapShot.getSnow(mu, mv);
            float landHeight = snapShot.getWorld(mu, mv);
            float waterHeight = snapShot.getWater(mu, mv);
            float elevation = landHeight + waterHeight;
            float massWaterVapor = snapShot.getWaterVapor(mu, mv);
            // Temperature for the given cell by height and by lattitude
            float Tk = Util.temperatureByHeightAndLattitudeAndTime(elevation, mv, 0);

            // The amount of water vapor in grams if the air was to be saturated.
            float waterVaporMassSat = Util.waterVaporPartialPressureToMass(atmosphericHeight * V,
                    Util.waterVaporSaturationThreshold(Util.pressurePaByHeightM(elevation), Tk), Tk);


            shadowedText(String.format(
                    "mu mv %f %f\n"
                    + "veg   %f\n"
                    + "water %f\n"
                    + "snow  %f\n"
                    + "elev  %f\n"
                    + "Cdeg  %f\n"
                    + "lat   %f\n"
                    + "es    %f\n"
                    + "evap  %f\n"
                    + "humd  %f\n",
                    mu, mv, vegetation, waterHeight, snow, (elevation - 2500) / 20, Tk - 273, mv,
                    waterVaporMassSat, massWaterVapor, massWaterVapor / waterVaporMassSat), 20, 40);
        }//*/
    }

    void drawPolitical(final WorldSnapShot snapShot)
    {
        final List<City> cities = snapShot.getCities();
        final List<Citizen> citizens = snapShot.getCitizens();
        loadPixels();
        int res = 1;
        for (float x = 0; x < width; x += res)
        {
            for (float y = 0; y < height; y += res)
            {
                float u = x / width;
                float v = y / height;
                int c = color(0, 0, 0);
                float landHeight = snapShot.getWorld(u, v);
                float waterHeight = snapShot.getWater(u, v);
                float snowHeight = snapShot.getSnow(u, v);
                float elevation = landHeight + waterHeight;

                if (snowHeight > 0.0)
                {
                    c = color(255, 255, 255);
                }
                // Else if water?
                else if (waterHeight > 1)
                {
                    // Edge of water near land?
                    float dx = (1.0f / width);
                    float dy = (1.0f / height);
                    float wn = snapShot.getWater(u, constrain(v - dy, 0, 0.999f));
                    float ws = snapShot.getWater(u, constrain(v + dy, 0, 0.999f));
                    float we = snapShot.getWater(constrain(u + dx, 0, 0.999f), v);
                    float ww = snapShot.getWater(constrain(u - dx, 0, 0.999f), v);
                    float wne = snapShot.getWater(constrain(u + dx, 0, 0.999f), constrain(v - dy, 0, 0.999f));
                    float wsw = snapShot.getWater(constrain(u - dx, 0, 0.999f), constrain(v + dy, 0, 0.999f));
                    float wse = snapShot.getWater(constrain(u + dx, 0, 0.999f), constrain(v + dy, 0, 0.999f));
                    float wnw = snapShot.getWater(constrain(u - dx, 0, 0.999f), constrain(v - dy, 0, 0.999f));
                    // Near land?
                    if (wn < 0.01 || ws < 0.01 || we < 0.01 || ww < 0.01
                            || wne < 0.01 || wsw < 0.01 || wse < 0.01 || wnw < 0.01)
                    {
                        c = color(50, 200, 255);
                    }
                    // Water all around
                    else
                    {
                        c = color(217, 255, 255);
                    }
                }
                // Else land
                else
                {
                    // Not an edge pixel?
                    if (x > 2 && y > 2 && x < (width - 2) && y < (height - 2))
                    {
                        // Calculate slope 
                        PVector nx = new PVector(-1, 0, (snapShot.getWorld(u - (2.0f / width), v) - landHeight) / 200);
                        nx.normalize();
                        PVector ny = new PVector(0, -1, (snapShot.getWorld(u, v - (2.0f / height)) - landHeight) / 200);
                        ny.normalize();
                        PVector n = (nx).cross(ny);
                        PVector sun = new PVector(-1, -1, 0.5f);
                        sun.normalize();
                        //n*l lighting
                        float incidence = sun.dot(n);
                        //incidence = 0.5;
                        // c=n*l+a
                        float Kambient = 0.4f;
                        int d = color(128 * incidence, 128 * incidence, 128 * incidence);
                        c = getPoliticalColor((int) x, (int) y, cities, snapShot);
                        c = blendColor(c, d, SCREEN);
                    }
                }
                for (int i = 0; i < res; i++)
                {
                    for (int j = 0; j < res; j++)
                    {
                        pixels[((int) y + j) * width + (int) x + i] = c;
                    }
                }
            }
        }
        int iconRes = 2;
        for (Citizen citizen : citizens)
        {
            //println(String.format("Drawing citizen @ %d, %d", citizen.getX(), citizen.getY()));

            for (int i = 0; i < iconRes; i++)
            {
                for (int j = 0; j < iconRes; j++)
                {
                    pixels[constrain(citizen.getY() + j, 0, height - 1) * width
                            + constrain(citizen.getX() + i, 0, width - 1)] = color(128, 0, 255);
                }
            }
        }
        updatePixels();

        stroke(color(0, 0, 0));
        for (City city : cities)
        {
            fill(city.getCulture().getColor());
            float d = constrain(city.getPopulation() / 3.0f, 2, 10);
            ellipse(city.getX(), city.getY(), d, d);
        }
        /*
         //println("done rendering");
         textFont(font,36);
         // white float frameRate
         fill(0);
         text(frameRate,23,33);
         fill(255);
         text(frameRate,20,30);
  
         //*
         if (mouseX >= 0 && mouseX < width && mouseY >=0 && mouseY < height)
         {
         textFont(font,12);
         float mu = (float)mouseX/width;
         float mv = (float)mouseY/height;
    
       
         float vegetation = snapShot.getVegetation(mu, mv);
         float snow = snapShot.getSnow(mu, mv);
         float landHeight = snapShot.getWorld(mu, mv);
         float waterHeight = snapShot.getWater(mu, mv);
         float elevation = landHeight + waterHeight;
         float massWaterVapor = snapShot.getWaterVapor(mu, mv);
         // Temperature for the given cell by height and by lattitude
         float Tk = temperatureByHeightAndLattitudeAndTime(elevation, mv*height, 0);
       
         // The amount of water vapor in grams if the air was to be saturated.
         float waterVaporMassSat = waterVaporPartialPressureToMass(atmosphericHeight*V,
         waterVaporSaturationThreshold(pressurePaByHeightM(elevation), Tk), Tk);
   
      
         shadowedText(String.format(
         "mu mv %f %f\n" +
         "veg  %f\n" +
         "snow %f\n" +
         "elev %f\n" +
         "Cdeg %f\n" +
         "lat  %f\n" +
         "es   %f\n" +
         "evap %f\n" +
         "humd %f\n",
         mu, mv, vegetation, snow, (elevation-2500)/20, Tk-273, mv,
         waterVaporMassSat, massWaterVapor, massWaterVapor/waterVaporMassSat
         ), 20, 40);
         }//*/
    }

    void shadowedText(String s, int x, int y)
    {

        fill(0);
        text(s, x + 1, y + 1);
        fill(255);
        text(s, x, y);
    }

    City getClosestCity(final List<City> cities, int x, int y)
    {
        City closest = null;
        float distance = Float.MAX_VALUE;
        for (City city : cities)
        {
            final float d = dist(city.getX(), city.getY(), x, y);
            if (d < distance)
            {
                closest = city;
                distance = d;
            }
        }
        return closest;
    }

    List<PVector> getPointsBetween(int x, int y, int x2, int y2)
    {
        final List<PVector> points = new ArrayList<PVector>();
        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0)
        {
            dx1 = -1;
        }
        else if (w > 0)
        {
            dx1 = 1;
        }
        if (h < 0)
        {
            dy1 = -1;
        }
        else if (h > 0)
        {
            dy1 = 1;
        }
        if (w < 0)
        {
            dx2 = -1;
        }
        else if (w > 0)
        {
            dx2 = 1;
        }
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest))
        {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0)
            {
                dy2 = -1;
            }
            else if (h > 0)
            {
                dy2 = 1;
            }
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++)
        {
            points.add(new PVector(x, y));
            numerator += shortest;
            if (!(numerator < longest))
            {
                numerator -= longest;
                x += dx1;
                y += dy1;
            }
            else
            {
                x += dx2;
                y += dy2;
            }
        }
        return points;
    }

    int getPoliticalColor(int x, int y, List<City> cities, final WorldSnapShot snapShot)
    {
        float d = 0;
        int x1 = x;
        int y1 = y;
        float lastHeight = snapShot.getWorld(x / (float) width, y / (float) height);
        final City city = getClosestCity(cities, x, y);
        if (city != null)
        {
            for (PVector p : getPointsBetween(x, y, city.getX(), city.getY()))
            {
                float world = snapShot.getWorld(p.x / (float) width, p.y / (float) height);
                float water = snapShot.getWater(p.x / (float) width, p.y / (float) height);
                d += Math.abs(lastHeight - world);
                d += dist(x1, y1, p.x, p.y);
                d += 10 * water;
                lastHeight = world;
                x1 = (int) p.x;
                y1 = (int) p.y;
            }
            if (d < 100)
            {
                return city.getCulture().getColor();
            }
            else
            {
                return color(200, 200, 200);
            }
        }
        return color(200, 200, 200);
    }
    
    public static void main(String args[])
    {
        PApplet.main(new String[] { "--present", "aaronsantos.worldbuilder.WorldBuilder" });
    }
}