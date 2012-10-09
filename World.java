import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import processing.core.PApplet;
import processing.core.PVector;

public class World
{
  // Cell length in meters
  static final float l = 1;
  
  // Cell area in meters^2
  static final float A = l*l;

  // Cell volume in meters^3
  static final float V = A*l;

  // Gravitational constant
  static final float g = 9.81f;

  // Atmosphere extends 10000m above elevation
  static final float atmosphericHeight = 10000;
  
  final int width, height;

  // Height of world
  private float[][] world;
  // Height of water 
  private float[][] water;
  private float[][] suspendedSediment;
  private float[][] outflowFluxL;
  private float[][] outflowFluxR;
  private float[][] outflowFluxT;
  private float[][] outflowFluxB;
  // Velocity of water
  private float[][] waterVelocityX;
  private float[][] waterVelocityY;
  
  float[][] tmpOutflowFluxL = null;
  float[][] tmpOutflowFluxR = null;
  float[][] tmpOutflowFluxT = null;
  float[][] tmpOutflowFluxB = null;
  float[][] tmpSediment = null;
  float[][] tmpWaterVapor = null;
  
  // Amount of snow on ground
  private float[][] snow;
  
  // Amount of water vapor in the air in grams.
  private float[][] waterVapor;
  
  // Amount of vegetation
  private float[][] vegetation;

  // Citizens in the world
  private List<Citizen> citizens = new ArrayList<Citizen>();
  
  // Cities of the world
  private List<City> cities = new ArrayList<City>();
  
  // Fluid solver for water vapor transport
  private NavierStokesSolver fluidSolver;
  
  private PApplet applet;
  
  private float visc = 0.00006f;
  private float diff = 0.35f;
  private float velocityScale = 1.0f;
  
  private long stepCount = 0;
  
  // Measure the elapsed time
  Date date = null;
  
  // For adding more time to the elapsed time.
  Calendar calendar = Calendar.getInstance() ;
  
  public World(final PApplet applet, final int width, final int height)
  {
    world = new float[width][height];
    water = new float[width][height];
    suspendedSediment = new float[width][height];
    outflowFluxL = new float[width][height];
    outflowFluxR = new float[width][height];
    outflowFluxT = new float[width][height];
    outflowFluxB = new float[width][height];
    
  
    tmpOutflowFluxL = new float[width][height];
    tmpOutflowFluxR = new float[width][height];
    tmpOutflowFluxT = new float[width][height];
    tmpOutflowFluxB = new float[width][height];
    tmpSediment = new float[width][height];
    tmpWaterVapor = new float[width][height];
    
    waterVelocityX = new float[width][height];
    waterVelocityY = new float[width][height];
    snow = new float[width][height];
    waterVapor = new float[width][height];
    vegetation = new float[width][height];
    
    this.applet = applet;
    this.width = width;
    this.height = height;
    fluidSolver = new NavierStokesSolver(width, height);
    
    calendar.set(1970, 1, 1, 12, 1);
    
    applet.noiseDetail(9, 0.4f);
    for(int j = 0; j<width; j++) 
    {
      for(int k = 0; k<height; k++)
      {
        world[j][k]=(float)5000*applet.noise((float)(j*8.0/width), (float)(k*8.0/height));
        
        if(world[j][k]<2500)
        {
          water[j][k]=2500-world[j][k];
        }
        else
        {
          world[j][k] = (float)Math.pow(1.009, world[j][k]-2600)+2501;
          if (world[j][k] > 10000)
          {
            world[j][k]=(float)(100*Math.log(world[j][k])+10000);
          }
          //println("land "+world[j][k]);
        }
      }
    }
    // Smoothing factor
    float Ks = 0.00001f;
    for (int step = 0; step<0; step++)
    {
      // smooth the terrain a little
      for(int x = 0; x<width; x++)
      {
        for(int y = 0; y<height; y++)
        {
          // Interior
          if (x>0 && y>0 && x<width-1 && y<height-1)
          {
            world[x][y]=(world[x][y]+Ks*world[x-1][y]+Ks*world[x+1][y]+Ks*world[x][y-1]+Ks*world[x][y+1])/(1+4*Ks);
          }
        }
      }
    }
  }
  
  public synchronized WorldSnapShot getSnapShot()
  {
     float[][] worldClone = new float[world.length][world[0].length];
     for(int i = 0; i < world.length; i++)
     {
       System.arraycopy(world[i], 0, worldClone[i], 0, world[0].length);
     }
       
     float[][] waterClone = new float[water.length][water[0].length];
     for(int i = 0; i < water.length; i++)
     {
       System.arraycopy(water[i], 0, waterClone[i], 0, snow[0].length);
     }
       
     float[][] snowClone = new float[snow.length][snow[0].length];
     for(int i = 0; i < snow.length; i++)
     {
       System.arraycopy(snow[i], 0, snowClone[i], 0, snow[0].length);
     }
     
     float[][] waterVaporClone = new float[waterVapor.length][waterVapor[0].length];
     for(int i = 0; i < waterVapor.length; i++)
     {
       System.arraycopy(waterVapor[i], 0, waterVaporClone[i], 0, waterVapor[0].length);
     }
     
     float[][] vegetationClone = new float[vegetation.length][vegetation[0].length];
     for(int i = 0; i < vegetation.length; i++)
     {
       System.arraycopy(vegetation[i], 0, vegetationClone[i], 0, vegetation[0].length);
     }
     List<Citizen> citizensClone = new ArrayList<Citizen>(citizens);
     List<City> citiesClone = new ArrayList<City>(cities);
     return (new WorldSnapShot(worldClone, waterClone, snowClone, waterVaporClone, vegetationClone,
       citizensClone, citiesClone));
  }
  
  // See http://www-evasion.imag.fr/Publications/2007/MDH07/FastErosion_PG07.pdf
  // and http://www2.tech.purdue.edu/cgt/facstaff/bbenes/private/papers/Stava08SCA.pdf
  public synchronized void step()
  {
    
    PApplet.println(String.format("step %d", stepCount++));
    
   
    final float dt = 0.005f;
    //  0.05;
    
    calendar.add(Calendar.MINUTE, 10);
    date = calendar.getTime();
    float minutesElapsedSinceMidnight = calendar.get(Calendar.MINUTE);
    //if (minutesElapsedSinceMidnight!=0)
    //{
    //  return;
    //}
    // move the wind
    final int windSteps = 2;
    for (int i = 0; i < windSteps; i++)
    {
      fluidSolver.tick(dt/windSteps, visc, diff);
    }
    
    // smooth the terrain a little
    // smoothing constant
    /*float Ksm = 0.0001;
    for(int x = 0; x<width; x++)
    {
      for(int y = 0; y<height; y++)
      {
        
        // Interior
        if (x>0 && y>0 && x<width-1 && y<height-1)
        {
          world[x][y]=(world[x][y]+Ksm*dt*world[x-1][y]+Ksm*dt*world[x+1][y]+Ksm*dt*world[x][y-1]+Ksm*dt*world[x][y+1])/(1+4*Ksm*dt);
        }
      }
    }*/
    
    boolean waterCycle = true; //frameCount < 300000;
    
    //if (frameCount == 300000)
    ////{
    //  println("stopping water cycle");
    //}
    
    // Run the evaporation/precipitation part of the sim?
    if (true)
    {
      // Maximum rate of precipitation in g/m^s/s
      // http://en.wikipedia.org/wiki/Rain#Intensity
      // 10mm = 1cm = .1m/hr
      // = 0.000277777778 m/s
      // = 277g/m^2/s
      float maxRainRate = 277*A/dt;
      //rainRate = 27;

      for(int x = 0; x<width; x++)
      {
        for(int y = 0; y<height; y++)
        {
          
          tmpWaterVapor[x][y] = 0;
          
          // Vegetation decay rate.
          float Vk = 0.2f;
          vegetation[x][y]*=1-Vk*dt;
          
          if (vegetation[x][y] < 0)
          {
            vegetation[x][y] = 0;
          }
          
          // Temperature for the given cell
          float Tk = Util.temperatureByHeightAndLattitudeAndTime(world[x][y]+water[x][y], (float)y/height, minutesElapsedSinceMidnight);
          
          // Grow vegetation
          if (water[x][y]>0 && water[x][y]<0.05)
          {
            // Amount of vegetation based on temperature.
            // positive from about 44F to 80F
            float Dv = Math.max(0, (-(float)Util.fastpow(0.15*(Tk-290), 2)+2)*dt);
            
            // Adjustment for water
            // positive between 0 and 0.1 m of water, peaking at 0.05m of water
            float Wadj = Math.max(0, -(float)Util.fastpow(20*(water[x][y]-0.05), 2)+1);
            if (Dv*Wadj > 0)
              //println("adding vegetation:"+1000000*Dv*Wadj+" Dv:"+Dv+"  Wadj:"+Wadj);
              
              vegetation[x][y]+=10*Dv*Wadj;
              
              if (vegetation[x][y]>3)
              {
                vegetation[x][y] = 3;
              }
          }
          
          // Condenstation threshold of water in grams.
          
          float Kc = Util.waterVaporPartialPressureToMass(atmosphericHeight*V, Util.waterVaporSaturationThreshold(Util.pressurePaByHeightM(world[x][y]+water[x][y]), Tk), Tk);
          
          // Water vapor greater than carrying capacity of the air?
          if (waterVapor[x][y] > Kc/2)
          {
            // Amount of precipitation in grams
            float precipitation = Math.min(waterVapor[x][y], waterVapor[x][y]*maxRainRate*dt);
            float precipitationHeight = Util.waterMassToWaterHeight(precipitation, A);
            
            //println(String.format("precipication in g %f", precipitation));
            
            if (precipitation < 0)
            {
              applet.println("precipitation<0");
            }
            waterVapor[x][y]-=precipitation;
            
            // Rain.
            if (Tk > 273)
            {
              water[x][y]+=precipitationHeight;
            }
            // Snow.
            else
            {
              snow[x][y]+=precipitationHeight;
            }
          }
          // Freezing?
          if (Tk < 273)
          {
            float Kf = 0.1f;
            float freeze = Math.min(water[x][y]*Kf*dt, water[x][y]);
            snow[x][y] += freeze;
            water[x][y] -= freeze;
          }
        }
      }
    }
    
    // Flow is simulated with the shallow-water model. Then
    // the velocity field and the water surface are updated.
    // Pipe cross-sectional area.
    if (waterCycle)
    {
      for(int x = 0; x<width; x++)
      {
        for(int y = 0; y<height; y++)
        {
          // Left
          if (x > 0)
          {
            float dh = world[x][y]+water[x][y]-world[x-1][y]-water[x-1][y];
            tmpOutflowFluxL[x][y] = Math.max(0, outflowFluxL[x][y]+dt*A*g*dh/l);
          }
          
          // Right
          if (x < width-1)
          {
            float dh = world[x][y]+water[x][y]-world[x+1][y]-water[x+1][y];
            tmpOutflowFluxR[x][y] = Math.max(0, outflowFluxR[x][y]+dt*A*g*dh/l);
          }
          // Top
          if (y > 0)
          {
              float dh = world[x][y]+water[x][y]-world[x][y-1]-water[x][y-1];
              tmpOutflowFluxT[x][y] = Math.max(0, outflowFluxT[x][y]+dt*A*g*dh/l);
          }
          // Bottomn
          if (y < height-1)
          {
            float dh = world[x][y]+water[x][y]-world[x][y+1]-water[x][y+1];
            tmpOutflowFluxB[x][y] = Math.max(0, outflowFluxB[x][y]+dt*A*g*dh/l);
          }
        }
      }
      // Scaling factor.
      for(int x = 0; x<width; x++)
      {
        for(int y = 0; y<height; y++)
        {
          if(tmpOutflowFluxL[x][y]+tmpOutflowFluxR[x][y]+tmpOutflowFluxT[x][y]+tmpOutflowFluxB[x][y] > 0)
          {
            float K = Math.min(1, water[x][y]*l*l/((tmpOutflowFluxL[x][y]+tmpOutflowFluxR[x][y]+tmpOutflowFluxT[x][y]+tmpOutflowFluxB[x][y])*dt));
            
            outflowFluxL[x][y] = K * tmpOutflowFluxL[x][y];
            outflowFluxR[x][y] = K * tmpOutflowFluxR[x][y];
            outflowFluxT[x][y] = K * tmpOutflowFluxT[x][y];
            outflowFluxB[x][y] = K * tmpOutflowFluxB[x][y];
          }
        }
      }
    }
    
    boolean wind = true;
    if (wind && stepCount%1 == 0)
    {
      // Idea for wind
      // http://www.octas.statkart.no/archive/octas_study_course_2005/Ocean1_Basics.pdf
      int n = NavierStokesSolver.N;
      float cellHeight = height / n;
      float cellWidth = width / n;
   
      int cellX = (int)applet.random(0, n);
      int cellY = (int)applet.random(0, n);
      float force = 2000;
      float mouseDx =  force;
      float mouseDy = force* applet.random(0, 1) > 0.5 ? -1 : 1;
   
      fluidSolver.applyForce(cellX, cellY, mouseDx, mouseDy);
    }

    // Water surface and velocity field
    for (int x = 0; x<width; x++)
    {
      for (int y = 0; y<height; y++)
      {
        if (waterCycle)
        {
           // Change in water volume
           float dV = dt*(
              (x<=0?0.0f:outflowFluxR[x-1][y])
              + (y<=0?0.0f:outflowFluxB[x][y-1])
              + (x>=width-1?0.0f:outflowFluxL[x+1][y])
              + (y>=height-1?0.0f:outflowFluxT[x][y+1])
            - (
              outflowFluxL[x][y]
              +outflowFluxR[x][y]
              +outflowFluxT[x][y]
              +outflowFluxB[x][y]
              ));
            // Move water according to flow
            water[x][y]+=dV/(l*l);
            // Don't have negative water ever.
            if(water[x][y]<0)
            {
              water[x][y] = 0;
            }
            
            // Use flow to calculate water velocity
            
            waterVelocityX[x][y] = (
              (x<=0?0.0f:outflowFluxR[x-1][y])
              - outflowFluxL[x][y]
              + outflowFluxR[x][y]
              - (x>=width-1?0.0f:outflowFluxL[x+1][y])
              )/2;
            waterVelocityY[x][y] = (
              (y==0?0.0f:outflowFluxB[x][y-1])
              - outflowFluxT[x][y]
              + outflowFluxB[x][y]
              - (y>=height-1?0.0f:outflowFluxT[x][y+1])
            )/2;
            
            // Calculate transport capacity
            float waterVelocity = (new PVector(waterVelocityX[x][y], waterVelocityY[x][y])).mag();
            //Terminal water velocity
            float Ktw = (float)(90*Math.sqrt(l));
            if (waterVelocity>Ktw)
            {
              waterVelocity = Ktw;
            }
            
            if (waterVelocity > 0)
            {
              // calculate slope of cell
              PVector nx = new PVector(-1, 0, (x==0?world[x+1][y]:world[x-1][y]) - world[x][y]);
              nx.normalize();
              PVector ny = new PVector(0, -1, (y==0?world[x][y+1]:world[x][y-1]) - world[x][y]);
              ny.normalize();
              PVector n = (nx).cross(ny);
              PVector vup = new PVector(0,0,1);
              float incidence = vup.dot(n);
              
              // Transport capacity.
              float Kc = 0.2f;
              float C = Kc * (1.0f-incidence) * waterVelocity;
              
              // Erosion-deposition process is computed with the velocity field
              // Dissolving constant
              float Ks = 0.1f;
              // Erosion
              if (C > suspendedSediment[x][y])
              {
                float erodedAmount = Math.max(0, Math.min(world[x][y], Ks*(C-suspendedSediment[x][y])));
                world[x][y]-=erodedAmount;
                suspendedSediment[x][y]+=erodedAmount;
              }
              // Deposition
              else
              {
                float depositionAmount = Math.max(0, Math.min(Ks*(C-suspendedSediment[x][y]), suspendedSediment[x][y]));
                world[x][y]+=depositionAmount;
                suspendedSediment[x][y]-=depositionAmount;
              }
            }
          }
          // Run the evaporation/precipitation part of the sim?
          float elevation = world[x][y]+water[x][y];
          // Temperature for the given cell by height and by lattitude
          float Tk = Util.temperatureByHeightAndLattitudeAndTime(elevation, (float)y/height, minutesElapsedSinceMidnight);
          // Rate of evaporation.
          float Re = 0.1f;
          // Water decreases due to evaporation.
          // Evaporation rate
          float Ke = Re*Math.max(0, Tk)*dt;
          
          // The amount of water vapor in grams if the air was to be saturated.
          float waterVaporMassSat = Util.waterVaporPartialPressureToMass(atmosphericHeight*V,
            Util.waterVaporSaturationThreshold(Util.pressurePaByHeightM(elevation), Tk), Tk);
                
          // Adjust the temperature by the humidity to account for cloud coverage
          //Tk = temperatureByHeightAndLattitudeAndTime(world[x][y]+water[x][y], y, 100*waterVapor[x][y]/waterVaporMassSat, minutesElapsedSinceMidnight);
          
          // The amount of water (in grams) that it would be needed to evaporate so
          // that the air was saturated.
          float dWMax = waterVaporMassSat-waterVapor[x][y];

          
          // The amount of water in grams that is evaporated this step.
          // Don't evaporate less than 0 water.
          float evaporationMass = applet.constrain(dWMax*Ke, 0, dWMax);

          // Maximum allowable water vapor in air in height of meters of water.
          float waterHeight = Util.waterMassToWaterHeight(evaporationMass, A);

          // The amount of water evaporated in meters of height
          // Don't evaporate more water than what exists.
          float evaporation = Math.min(water[x][y], waterHeight);
          
          
          if (x == 300 && y == 300 && stepCount%10000==0)
          {
            applet.println(String.format("evaopration in g %f", evaporationMass));
            applet.println(String.format("water height in m %f", waterHeight));
            applet.println(String.format("water in m %f", water[x][y]));
            applet.println(String.format("waterVaporMassSat in g %f", waterVaporMassSat));
            applet.println(String.format("Tk %f", Tk));
            applet.println(String.format("waterVapor[x][y] %f", waterVapor[x][y]));
            applet.println(String.format("elevation %f",elevation));
          }
          
          // Take evaporation from water and add it to the air as water vapor.
          water[x][y]-=evaporation;
          waterVapor[x][y]+=evaporationMass;
          
          if (water[x][y] < 0.000000000001)
          {
           // water[x][y]=0;
          }
        
        // Water increase due to snow melt.
        // Melting constant
        if (snow[x][y]>0)
        {
          float Km = 100.1f;
          float melt = Math.min(snow[x][y]*Km*dt, snow[x][y]);
          //snow[x][y]=-melt;
          water[x][y]+=melt;
          if (snow[x][y] < 0.00001)
          {
            snow[x][y]=0;
          }
          if(melt!=0)
          {
            //println("snow "+snow[x][y]);
            //println("melt "+melt);
          }
        }
        
        // Suspended sediment is transported by the velocity field.
        if (water[x][y] > 0)
        {
          int u = (int)(x-waterVelocityX[x][y]*dt);
          int v = (int)(y-waterVelocityY[x][y]*dt);
          
          if(u>=0 && u<width && v>=0 && v<height)
          {
            tmpSediment[x][y] = suspendedSediment[u][v];
          }
          else if(x>0 && x<width-1 && y>0 && y<height-1)
          {
            tmpSediment[x][y] = (suspendedSediment[x-1][y]+suspendedSediment[x+1][y]+suspendedSediment[x][y-1]+suspendedSediment[x][y+1])/4;
          }
        }
        
        // Move sea ice
        if (snow[x][y] > 0 && water[x][y] > 20 && applet.random(0, 10)<1)
        {
          float v = (float)y/height;
          // North pole?
          if (v < 0.5)
          {
             // no ice to the south?
             if (snow[x][y+1] == 0 && water[x][y+1] > 20)
             {
               // move berg south
               snow[x][y+1] = snow[x][y];
               snow[x][y] = 0;
             }
          }
          else
          {
            // no ice to the north?
             if (snow[x][y-1] == 0 && water[x][y-1] > 20)
             {
               // move berg north
               snow[x][y-1] = snow[x][y];
               snow[x][y] = 0;
             }
          }
        }  
        
        // Water vapor is transported by wind velocity field inside the fluid solver
        if (wind)
        {
          if (x<width-1 && y<height-1)
          {
            float dx = (float) fluidSolver.getDx(x, y, width, height);
            float dy = (float) fluidSolver.getDy(x, y, width, height);
            
            // Move water vapor
            if ((int)(x+dx)>=0 && (int)(x+dx)<=width-1 && (int)(y+dy)>=0 && (int)(y+dy)<=height-1)
            {
              float fx = x+dx;
              float fy = y+dy;
              int tx = (int)fx;
              int ty = (int)fy;
              float lX = fx - tx - 0.5f;
              float lY = fy - ty - 0.5f;
              int v, h;
              // upper left?
              if (lY > lX)
              {
                // upper right?
                if (lY > -lX) { v = 1; h = 0; }
                // lower left?
                else { v = 0; h = -1; }
              }
              // lower right?
              else
              {
                // upper right?
                if (lY > -lX) { v = 0; h = 1; }
                // lower right?
                else { v = -1; h = 0; }
              }
              // other cell amount
              float oca = lX * h + lY * v;
              // this cell amount
              float tca = 1 - oca;
              
              int ocx = applet.constrain(tx+h, 0, width-1);
              int ocy = applet.constrain(ty+v, 0, height-1);
              
              
              if(x==300 && y==300 && stepCount%10==0)
              {
                applet.println("moving "+(x+dx)+","+(y+dy)+" to "+x+","+y);
                applet.println("amount "+waterVapor[x][y]+" g");
              }
            
              tmpWaterVapor[x][y] += tca * waterVapor[tx][ty] + oca * waterVapor[ocx][ocy];
              waterVapor[tx][ty]-=tca * waterVapor[tx][ty];
              waterVapor[ocx][ocy]-=oca * waterVapor[ocx][ocy];
            }
            else if(x>0 && x<width-1 && y>0 && y<height-1)
            {
              tmpWaterVapor[x][y] += (waterVapor[x-1][y]+waterVapor[x+1][y]+waterVapor[x][y-1]+waterVapor[x][y+1])/4;
            }
          }
        }
      }
    }
    // Copy tmp values into real values.
    for(int i = 0; i<waterVapor.length; i++)
    {
      System.arraycopy(tmpWaterVapor[i], 0, waterVapor[i], 0, tmpWaterVapor[i].length);
      System.arraycopy(tmpSediment[i], 0, suspendedSediment[i], 0, tmpSediment[i].length);
    }
    
    if (stepCount%2 == 0 && stepCount > 30 && stepCount < 130)
    {
      for (int i=0; i<10; i++)
      {
        releaseCitizen();
      }
    }
    List<Citizen> citizensToDelete = new ArrayList<Citizen>();
    for (Citizen citizen : citizens)
    {
      if (stepCitizen(citizen))
      {
        citizensToDelete.add(citizen);
      }
    }
    citizens.removeAll(citizensToDelete);
  }
  
  void releaseCitizen()
  {
    boolean embarcationFound = false;
    while (!embarcationFound)
    {
      int x = (int)applet.random(0, width-1);
      int y = (int)applet.random(0, height-1);
      // Don't start people off in the ocean. They will drown :(
      // Don't start people off at high altitude, they will get sick.
      if (water[x][y] < 0.01 && world[x][y] < 5000)
      {
        embarcationFound = true;
        // Create a new citizen here with a random urban affinity between 0 and 1.
        citizens.add(new Citizen(x, y));
        applet.println(String.format("Citizen released at %d, %d", x, y));
      }
    }
  }

  boolean stepCitizen(final Citizen citizen)
  {
    PVector cp = new PVector(citizen.getX(), citizen.getY());
    // find nearby most habitable point.
    PVector p = mostHabitablePoint(citizen);
    if (p == null)
    {
      return false;
    }
    
    boolean deleteCitizen = false;
    
    // Is it possible to get there?
    // No? loop to next most habitable point.
    
    // Move one step closer to most habitable point
    citizen.setX((int)p.x);
    citizen.setY((int)p.y);
    // At most habitable point?
    if (cp.equals(p))
    {
      // Citizen should be deleted
      deleteCitizen = true;
      // Find city at point
      City foundCity= null;
      for (final City city : cities)
      {
        if (city.getX() == p.x && city.getY() == p.y)
        {
          foundCity = city;
          break;
        }
      }
      if (foundCity == null)
      {
        foundCity = new City((int)p.x, (int)p.y, 0);
        cities.add(foundCity);
        applet.println(String.format("City created at %d, %d", (int)p.x, (int)p.y));
      }
      foundCity.setPopulation(1+foundCity.getPopulation());
    }
    return deleteCitizen;
  }
  
  PVector mostHabitablePoint(final Citizen citizen)
  {
    int x = citizen.getX();
    int y = citizen.getY();
    int w = 20;
    int h = 20;
    PVector bestMatch = null;
    float bestScore = 0;
    for (int i = x-w/2; i<x+w/2; i++)
    {
      for (int j = y-h/2; j<y+h/2; j++)
      {
        float score = habitability(
          applet.constrain(i, 0, width-1),
          applet.constrain(j, 0, height-1));
        if (score > bestScore)
        {
          bestMatch = new PVector(i, j);
          bestScore = score;
        }
      }
    }
    return bestMatch;
  }
  
  float habitability(int x, int y)
  {
    if (water[x][y] > 0.01)
    {
      return -1;
    }
    float dToNearestCity = width+height;
    for (City city : cities)
    {
      float d = applet.dist((float)x, (float)y, (float)city.getX(), (float)city.getY());
      if (d < dToNearestCity)
      {
        dToNearestCity = d;
      }
    }
    // Add non-affinity to edges of map
    float temperatureIndex = -Math.abs(290-Util.temperatureByHeightAndLattitudeAndTime(world[x][y], (float)y/height, 660)) + 10;
    float cityIndex = PApplet.constrain((float)(applet.sq(dToNearestCity/3)/2+1000/(dToNearestCity+0.5+0.1)-8), -10.0f, 100.0f);
    return cityIndex + 100*vegetation[x][y] + 10*temperatureIndex;
  }
}
