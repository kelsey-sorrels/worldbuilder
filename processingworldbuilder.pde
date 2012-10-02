public static double fastpow(final double a, final double b)
{
    final long tmp = Double.doubleToLongBits(a);
    final long tmp2 = (long)(b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
    return Double.longBitsToDouble(tmp2);
}

 // Cell length in meters
  static final float l = 1;
  
  // Cell area in meters^2
  static final float A = l*l;
  
  // Cell volume in meters^3
  static final float V = A*l;
  
  // Gravitational constant
  static final float g = 9.81;
  
  // Atmosphere extends 500m above elevation
  static final float atmosphericHeight = 10000;

private static class WorldSnapShot
{
  // Height of world
  float[][] world;
  // Height of water 
  float[][] water;
  // Height of snow.
  float[][] snow;
  // Mass of water vapor.
  float[][] waterVapor;
  // Amount of vegetation
  float[][] vegetation;
  
  
  public WorldSnapShot(final float[][] world, final float[][] water, final float[][] snow, final float[][] waterVapor, float[][] vegetation)
  {
    this.world = world;
    this.water = water;
    this.snow = snow;
    this.waterVapor = waterVapor;
    this.vegetation = vegetation;
  }
  
  public float getWorld(float x, float y)
  {
    return world[(int)(x * world[0].length)][(int)(y * world.length)];
  }
  
  public float getWater(float x, float y)
  {
    return water[(int)(x * water[0].length)][(int)(y * water.length)];
  }
  
  public float getSnow(float x, float y)
  {
    return snow[(int)(x * snow[0].length)][(int)(y * snow.length)];
  }
  
  public float getWaterVapor(float x, float y)
  {
    return waterVapor[(int)(x * waterVapor[0].length)][(int)(y * waterVapor.length)];
  }
  
  public float getVegetation(float x, float y)
  {
    return vegetation[(int)(x * vegetation[0].length)][(int)(y * vegetation.length)];
  }
}

private class World
{
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
  
  float[][] tmpOutflowFluxL = new float[width][height];
  float[][] tmpOutflowFluxR = new float[width][height];
  float[][] tmpOutflowFluxT = new float[width][height];
  float[][] tmpOutflowFluxB = new float[width][height];
  float[][] tmpSediment = new float[width][height];
  float[][] tmpWaterVapor = new float[width][height];
  
  // Amount of snow on ground
  private float[][] snow;
  
  // Amount of water vapor in the air in grams.
  private float[][] waterVapor;
  
  // Amount of vegetation
  private float[][] vegetation;
  
  // Fluid solver for water vapor transport
  private NavierStokesSolver fluidSolver = new NavierStokesSolver();
  
  private float visc = 0.00006f;
  private float diff = 0.35f;
  private float velocityScale = 1.0f;
  
  private long stepCount = 0;
  
  // Measure the elapsed time
  Date date = null;
  
  // For adding more time to the elapsed time.
  Calendar calendar = Calendar.getInstance() ;
  
  public World(final int width, final int height)
  {
    world = new float[width][height];
    water = new float[width][height];
    suspendedSediment = new float[width][height];
    outflowFluxL = new float[width][height];
    outflowFluxR = new float[width][height];
    outflowFluxT = new float[width][height];
    outflowFluxB = new float[width][height];
    waterVelocityX = new float[width][height];
    waterVelocityY = new float[width][height];
    snow = new float[width][height];
    waterVapor = new float[width][height];
    vegetation = new float[width][height];
    
    calendar.set(1970, 1, 1, 12, 1);
    
    noiseDetail(9, 0.4);
    for(int j = 0; j<width; j++) 
    {
      for(int k = 0; k<height; k++)
      {
        world[j][k]=(float)5000*noise((float)(j*8.0/width), (float)(k*8.0/height));
        
        if(world[j][k]<2500)
        {
          water[j][k]=2500-world[j][k];
        }
        else
        {
          world[j][k] = pow(1.009, world[j][k]-2600)+2501;
          if (world[j][k] > 10000)
          {
            world[j][k]=100*log(world[j][k])+10000;
          }
          //println("land "+world[j][k]);
        }
      }
    }
    // Smoothing factor
    float Ks = 0.00001;
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
     
     return (new WorldSnapShot(worldClone, waterClone, snowClone, waterVaporClone, vegetationClone));
  }
  
  // See http://www-evasion.imag.fr/Publications/2007/MDH07/FastErosion_PG07.pdf
  // and http://www2.tech.purdue.edu/cgt/facstaff/bbenes/private/papers/Stava08SCA.pdf
  public synchronized void step()
  {
    
    println(String.format("step %d", stepCount++));
    
   
    final float dt = 0.005;
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
      // = 277g/m^s/s
      float maxRainRate = 0.3*A;
      //rainRate = 27;

      for(int x = 0; x<width; x++)
      {
        for(int y = 0; y<height; y++)
        {
          
          tmpWaterVapor[x][y] = 0;
          
          // Vegetation decay rate.
          float Vk = 0.0000002;
          vegetation[x][y]*=1-Vk*dt;
          
          if (vegetation[x][y] < 0)
          {
            vegetation[x][y] = 0;
          }
          
          // Temperature for the given cell
          float Tk = temperatureByHeightAndLattitudeAndTime(world[x][y]+water[x][y], y, minutesElapsedSinceMidnight);
          
          // Grow vegetation
          if (water[x][y]>0 && water[x][y]<0.05)
          {
            // Amount of vegetation based on temperature.
            // positive from about 44F to 80F
            float Dv = max(0, (-(float)fastpow(0.15*(Tk-290), 2)+2)*dt);
            
            // Adjustment for water
            // positive between 0 and 0.1 m of water, peaking at 0.05m of water
            float Wadj = max(0, -(float)fastpow(20*(water[x][y]-0.05), 2)+1);
            if (Dv*Wadj > 0)
              //println("adding vegetation:"+1000000*Dv*Wadj+" Dv:"+Dv+"  Wadj:"+Wadj);
              
              vegetation[x][y]+=1000000*Dv*Wadj;
              
              if (vegetation[x][y]>3)
              {
                vegetation[x][y] = 3;
              }
          }
          
          // Condenstation threshold of water in grams.
          
          float Kc = waterVaporPartialPressureToMass(atmosphericHeight*V, waterVaporSaturationThreshold(pressurePaByHeightM(world[x][y]+water[x][y]), Tk), Tk);
          
          // Water vapor greater than carrying capacity of the air?
          if (waterVapor[x][y] > Kc/2)
          {
            // Amount of precipitation in grams
            float precipitation = min(waterVapor[x][y], waterVapor[x][y]*maxRainRate*dt);
            float precipitationHeight = waterMassToWaterHeight(precipitation, A);
            
            //println(String.format("precipication in g %f", precipitation));
            
            if (precipitation < 0)
            {
              println("precipitation<0");
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
            float Kf = 0.1;
            float freeze = min(water[x][y]*Kf*dt, water[x][y]);
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
            tmpOutflowFluxL[x][y]=max(0, outflowFluxL[x][y]+dt*A*g*dh/l);
          }
          
          // Right
          if (x < width-1)
          {
            float dh = world[x][y]+water[x][y]-world[x+1][y]-water[x+1][y];
            tmpOutflowFluxR[x][y]=max(0, outflowFluxR[x][y]+dt*A*g*dh/l);
          }
          // Top
          if (y > 0)
          {
              float dh = world[x][y]+water[x][y]-world[x][y-1]-water[x][y-1];
              tmpOutflowFluxT[x][y]=max(0, outflowFluxT[x][y]+dt*A*g*dh/l);
          }
          // Bottomn
          if (y < height-1)
          {
            float dh = world[x][y]+water[x][y]-world[x][y+1]-water[x][y+1];
            tmpOutflowFluxB[x][y]=max(0, outflowFluxB[x][y]+dt*A*g*dh/l);
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
            float K = min(1, water[x][y]*l*l/((tmpOutflowFluxL[x][y]+tmpOutflowFluxR[x][y]+tmpOutflowFluxT[x][y]+tmpOutflowFluxB[x][y])*dt));
            
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
   
      int cellX = (int)random(0, n);
      int cellY = (int)random(0, n);
      float force = 2000;
      float mouseDx =  force;
      float mouseDy = force* random(0, 1) > 0.5 ? -1 : 1;
   
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
            float Ktw = 90*sqrt(l);
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
              float Kc = 0.2;
              float C = Kc * (1.0f-incidence) * waterVelocity;
              
              // Erosion-deposition process is computed with the velocity field
              // Dissolving constant
              float Ks = 0.1;
              // Erosion
              if (C > suspendedSediment[x][y])
              {
                float erodedAmount = max(0, min(world[x][y], Ks*(C-suspendedSediment[x][y])));
                world[x][y]-=erodedAmount;
                suspendedSediment[x][y]+=erodedAmount;
              }
              // Deposition
              else
              {
                float depositionAmount = max(0, min(Ks*(C-suspendedSediment[x][y]), suspendedSediment[x][y]));
                world[x][y]+=depositionAmount;
                suspendedSediment[x][y]-=depositionAmount;
              }
            }
          }
          // Run the evaporation/precipitation part of the sim?
          float elevation = world[x][y]+water[x][y];
          // Temperature for the given cell by height and by lattitude
          float Tk = temperatureByHeightAndLattitudeAndTime(elevation, y, minutesElapsedSinceMidnight);
          // Rate of evaporation.
          float Re = 0.15;
          // Water decreases due to evaporation.
          // Evaporation rate
          float Ke = Re*Math.max(0, Tk)*dt;
          
          // The amount of water vapor in grams if the air was to be saturated.
          float waterVaporMassSat = waterVaporPartialPressureToMass(atmosphericHeight*V,
            waterVaporSaturationThreshold(pressurePaByHeightM(elevation), Tk), Tk);
                
          // Adjust the temperature by the humidity to account for cloud coverage
          //Tk = temperatureByHeightAndLattitudeAndTime(world[x][y]+water[x][y], y, 100*waterVapor[x][y]/waterVaporMassSat, minutesElapsedSinceMidnight);
          
          // The amount of water (in grams) that it would be needed to evaporate so
          // that the air was saturated.
          float dWMax = waterVaporMassSat-waterVapor[x][y];

          
          // The amount of water in grams that is evaporated this step.
          // Don't evaporate less than 0 water.
          float evaporationMass = constrain(dWMax*Ke, 0, dWMax);

          // Maximum allowable water vapor in air in height of meters of water.
          float waterHeight = waterMassToWaterHeight(evaporationMass, A);

          // The amount of water evaporated in meters of height
          // Don't evaporate more water than what exists.
          float evaporation = min(water[x][y], waterHeight);
          
          
          if (x == 300 && y == 300 && stepCount%10==0)
          {
            println(String.format("evaopration in g %f", evaporationMass));
            println(String.format("water height in m %f", waterHeight));
            println(String.format("water in m %f", water[x][y]));
            println(String.format("waterVaporMassSat in g %f", waterVaporMassSat));
            println(String.format("Tk %f", Tk));
            println(String.format("waterVapor[x][y] %f", waterVapor[x][y]));
            println(String.format("elevation %f",elevation));
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
          float Km = 0.1;
          float melt = min(snow[x][y]*Km*dt, snow[x][y]);
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
              float lX = fx - tx - 0.5;
              float lY = fy - ty - 0.5;
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
              
              int ocx = constrain(tx+h, 0, width-1);
              int ocy = constrain(ty+v, 0, height-1);
              
              
              if(x==300 && y==300 && stepCount%10==0)
              {
                println("moving "+(x+dx)+","+(y+dy)+" to "+x+","+y);
                println("amount "+waterVapor[x][y]+" g");
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
  }
}

/**
 * Calculate pressure in Pa given height in m.
 * @see http://en.wikipedia.org/wiki/Atmospheric_pressure
 */
float pressurePaByHeightM(float h)
{
  return  101.325*(float)fastpow(1.0-0.0065*h/288.15, (8.80665*0.0289)/(8.3144/0.0065));
}

/**
 * Atmospheric temperature by height in meters.
 * @see http://www.kansasflyer.org/index.asp?nav=Avi&sec=Alti&tab=Theory&pg=2
 * @param h Height in meters above sea level.
 * @param lattitude [0, height), 0 is most northern, height-1 is most southern.
 * @return The atmospheric temperature at this height in Kelvin.
 */
float temperatureByHeightAndLattitudeAndTime(float h, float lattitude, float minutesElapsedInDay)
{
  return temperatureByHeightAndLattitudeAndTime(h, lattitude, 0, minutesElapsedInDay);
}

float temperatureByHeightAndLattitudeAndTime(float h, float lattitude, float humidity, float minutesElapsedInDay)
{
  // temperature in C.
  float Tc = 15 + (-6.5 * (h-3200)/1000);
  
  float lattitudeAdjustmentC = (80.0f/height)*abs(lattitude-height/2)-15;
  
  // Under clouds? Let the clouds block the light, cooler temperature.
  float humidityAdjustment = 0;//humidity> 100 ? 20*humidity/100:0;
  
  // Adjust for day/night temperatures +/- 7C
  float diurnalAdjustment = 0;
  float temperatureVariance = -7;
  diurnalAdjustment = temperatureVariance * sin(minutesElapsedInDay*2*PI/720);
  
  // Temperature in K.
  // Don't return a negative number, absolute zero and all.
  return max(0.01, (Tc+273-lattitudeAdjustmentC-humidityAdjustment)+diurnalAdjustment);
}

/**
 * Calculate the partial pressure of water vapor required to saturate air.
 * Shamelessly copied from
 * http://www.nco.ncep.noaa.gov/pmb/codes/nwprod/rap.v1.0.6/sorc/rap_gsi.fd/gsdcloud/adaslib.f90
 * @param Ps the air pressure in Pascals
 * @param T the air temperature in Kelvin
 * @return The partial pressure of water vapor required to saturate the air in Pascals
 */
float waterVaporSaturationThreshold(float Ps, float T)
{
  final float satfwa = 1.0007;
  final float satfwb = 3.46E-8;
  final float satewa = 611.21;
  final float satewb = 17.502;
  final float satewc = 32.18;
  
  final float f = satfwa + satfwb * Ps;
  final float fesl = (float)(f * satewa * fastpow((float)Math.E, satewb*(T-273.15)/(T-satewc)));
  return fesl;
}

/**
 * Calculate the mass of water vapor in grams that occupies a given volume
 * with a given pressure and temperature.
 * @param V The volume occupied by the water vapor in m^3.
 * @param P The partial pressure exerted by the vapor in Pascals.
 * @param T The temperature of the vapor in Kelvin.
 * @return Mass of water in grams.
 */
float waterVaporPartialPressureToMass(float V, float P, float T)
{
  // R is the ideal gas constant and has the value 8.314 J·K^−1·mol^−1.
  float R = 8.314;
  
  //PV=nRT
  // or PV/RT = n
  // n is in moles
  float n = P*V/(R*T);
  
  // Moar mass of water 18.01528(33) g/mol
  float mm = 18.01528;
  
  // moles * grams/mole = grams.
  float mass = n * mm;
  
  // return mass in grams
  return (mass);
}

/**
 * Convert grams of water into height of water
 * @param g Amount of water in grams
 * @param A The area the water takes up.
 * @return The height the water would take up in meters
 */
float waterMassToWaterHeight(float g, float A)
{
  return (g/(A*100*100*100));
}


/**
 * Convert height of water into mass of water
 * @param h The height of the water in meters
 * @param A The area the water takes up.
 * @return The mass of the water in grams.
 */
float waterHeightToWaterMass(float h, float A)
{
  return (h*(A*100*100*100));
}

World world = null;
Vector<WorldSnapShot> snapShots = new Vector<WorldSnapShot>();
WorldSnapShot snapShot = null;
Thread t = null;
PFont font;
ColorMapper mapper;
PImage c, e, r, temp;

void setup() {
  size(300, 300);
  background(0);
  frameRate(60);
      
  world = new World(width, height);
  font = createFont("Arial Bold",48);
  println("Analyzing colors");
  c = loadImage("color.png");
  e = loadImage("elevation.jpeg");
  r = loadImage("rainfall.png");
  temp = loadImage("temperature.png");
    
  c.resize(814, 463);
  e.resize(814, 470);
  r.resize(814, 400);
  temp.resize(814, 390);
    
  mapper = new ColorMapper(c, e, r, temp);
    
  println("Rendering");
  //noLoop();
  
  t = new Thread(){
    public void run()
    {
      for(;;)
      {
        world.step();
        if (snapShots.size() > 10)
        {
          snapShots.clear();
        }
        snapShots.add(world.getSnapShot());
      }
    }
  };
  t.start();
    
}

void draw()
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
  
  loadPixels();
  int res = 1;
  for(float x = 0; x<width; x+=res)
  {
    for(float y = 0; y<height; y+=res)
    {
      float u = x/width;
      float v = y/height;
      color c = color(0, 0, 0);
      float landHeight = snapShot.getWorld(u, v);
      float waterHeight = snapShot.getWater(u, v);
      float snowHeight = snapShot.getSnow(u, v);
      float massWaterVapor = snapShot.getWaterVapor(u, v);
      float vegetation = snapShot.getVegetation(u, v);
      float elevation = landHeight + waterHeight;
      // Temperature for the given cell by height and by lattitude
      float Tk = temperatureByHeightAndLattitudeAndTime(elevation, y, 0);
      
      // show height map and rain?
      if (false)
      {
        c = color(landHeight, landHeight, landHeight);
        if(waterHeight > 0.0001 /*
          && false
        //*/
        )
        {
          c = color(3*24, 3*34, 89*waterHeight);
          if (massWaterVapor > 1)
          {
            c = color(0, 10*massWaterVapor+50, 0);
          }
        }
        set((int)x, (int)y, c);
      }
      else
      {
        if(snowHeight > 0.0)
        {
          c = color(255, 255, 255);
        }
        else if(waterHeight > 0.000006)
        { 
          c = color(15, 60*5/(waterHeight/20+2)+20, 89*5/(waterHeight/20+3)+60);
          color lightWater = color(86, 181, 188);
          color mediumWater = color(12, 45, 58);
          color darkWater =  color(3, 5, 15);
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
          if(x>2 && y>2 && x<(width-2) && y<(height-2))
          {
            // Calculate slope 
            PVector nx = new PVector(-1, 0, (snapShot.getWorld(u-(2.0f/width), v) - landHeight)/200);
            nx.normalize();
            PVector ny = new PVector(0, -1, (snapShot.getWorld(u, v-(2.0f/height)) - landHeight)/200);
            ny.normalize();
            PVector n = (nx).cross(ny);
            PVector sun = new PVector(-1,-1,0.5);
            sun.normalize();
            //n*l lighting
            float incidence = sun.dot(n);
            //float incidence = 0.5;
            // c=n*l+a
            float Kambient = 0.4;
            if(incidence>0)
            {
              
              color d = color(128*incidence, 128*incidence, 128*incidence);
              float Vadj = 1.0/(1+(float)fastpow(2.8, -vegetation+3));
              
              c = color(80, 140, 90);
              c = mapper.colorMapQuery(vegetation, (elevation-2200)/5, Tk-273, v);
              if (vegetation>0)
                c = color(255, 0, 0);
              c = blendColor(c, d, ADD);
            }
            else
            {
              c = color(max(15*-incidence+snowHeight*150, Kambient*15), max(23*-incidence+snowHeight*150, Kambient*60), max(12*-incidence+snowHeight*150, Kambient*140));
            }
          }
        }
            
          // The amount of water vapor in grams if the air was to be saturated.
          float waterVaporMassSat = waterVaporPartialPressureToMass(atmosphericHeight*V,
            waterVaporSaturationThreshold(pressurePaByHeightM(elevation), Tk), Tk);
          float humidity = massWaterVapor/waterVaporMassSat;
         //if (massWaterVapor > waterVaporMassSat*0.14)
         {
            final int cloudCover = constrain((int)(255.0/(1+fastpow(2.3, -16*humidity+8))-10), 0, 255);
            c = blendColor(c, color(255, 255, 255, cloudCover), SCREEN);
         }
          //}
        for (int i = 0; i < res; i++)
        {
          for (int j = 0; j < res; j++)
          {
            pixels[((int)y+j)*width+(int)x+i] = c;
          }
        }
      }
    }
  }
  updatePixels();
  /*
  //println("done rendering");
  textFont(font,36);
  // white float frameRate
  fill(0);
  text(frameRate,23,33);
  fill(255);
  text(frameRate,20,30);
  
  //*/
  if (mouseX >= 0 && mouseX < width && mouseY >=0 && mouseY < height)
  {
    textFont(font,12);
    float mu = (float)mouseX/width;
    float mv = (float)mouseY/height;
    
       
    float vegetation = snapShot.getVegetation(mu, mv);
    float Vadj = 1.0/(1+(float)fastpow(2.8, -vegetation+3));
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
      "elev %f\n" +
      "Cdeg %f\n" +
      "lat  %f\n" +
      "es   %f\n" +
      "evap %f\n" +
      "humd %f\n",
      mu, mv, vegetation, (elevation-2200)/5, Tk-273, mv,
      waterVaporMassSat, massWaterVapor, massWaterVapor/waterVaporMassSat
      ), 20, 40);//*/
  }
    
          
  
}

void shadowedText(String s, int x, int y)
{
  
  fill(0);
  text(s, x+1, y+1);
  fill(255);
  text(s, x, y);
}

