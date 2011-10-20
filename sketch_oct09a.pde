
private static class WorldSnapShot
{
  // Height of world
  float[][] world;
  // Height of water 
  float[][] water;
  // Height of snow.
  float[][] snow;
  
  public WorldSnapShot(final float[][] world, final float[][] water, final float[][] snow)
  {
    this.world = world;
    this.water = water;
    this.snow = snow;
  }
  
  public float[][] getWorld()
  {
    return world;
  }
  
  public float[][] getWater()
  {
    return water;
  }
  
  public float[][] getSnow()
  {
    return snow;
  }
  
  public void setWorld(final float[][] world)
  {
    this.world = world;
  }
  
  public void setWater(final float[][] water)
  {
    this.water = water;
  }
  
  public void setSnow(final float[][] snow)
  {
    this.snow = snow;
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
  
  private float[][] snow;
  
  // Calculate how much water evaporated last frame,
  // this is the rain budget for the next frame.
  // The amount of water is then conserved.
  float totalEvaporation = 0;
  
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
    
    
    noiseDetail(9, 0.4);
    for(int j = 0; j<width; j++) 
    {
      for(int k = 0; k<height; k++)
      {
        world[j][k]=(float)255*noise((float)(j*0.009), (float)(k*0.009));
        if(world[j][k]<127)
        {
          water[j][k]=127-world[j][k];
        }
        else
        {
          world[j][k] = pow(1.2, (world[j][k]-127)/2)+125.5;
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
  
  public synchronized void getSnapShot(final WorldSnapShot snapShot)
  {
     snapShot.setWorld(world.clone());
     snapShot.setWater(water.clone()); 
     snapShot.setSnow(snow.clone());
  }
  
  // See http://www-evasion.imag.fr/Publications/2007/MDH07/FastErosion_PG07.pdf
  // and http://www2.tech.purdue.edu/cgt/facstaff/bbenes/private/papers/Stava08SCA.pdf
  public synchronized void step()
  {
    float dt = 0.002;
    dt=0.05;
    
    // smooth the terrain a little
    // smoothing constant
    float Ksm = 0.01;
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
    }
    
    boolean waterCycle = frameCount < 300;
    
    if (frameCount == 300)
    {
      println("stopping water cycle");
    }
    
    // Run the evaporation/precipitation part of the sim?
    if (waterCycle)
    {
      // Water increases due to rainfall or river sources.
      float numberOfRainDrops = random(0, width*height*dt/50000);
      float rainRate = 100.9;
      
      while(totalEvaporation>4*rainRate*dt)
      {
        int x = (int)random(0, width-1);
        int y = (int)random(0, height-1);
        // Don't water the edge for simplicity.
        // And don't water the ocean because we don't want tsunamis.
        if (x>0 && y>0 && x<width-1 && y<height-1 && water[x][y]<0.1)
        {
          // Level of snow.
          float Ksnowline = 140;
          // Rain.
          if (world[x][y] < Ksnowline)
          {
            water[x][y]+=rainRate*dt;
            
            water[x-1][y]+=0.5*rainRate*dt;
            water[x+1][y]+=0.5*rainRate*dt;
            water[x][y-1]+=0.5*rainRate*dt;
            water[x][y+1]+=0.5*rainRate*dt;
            
            water[x-1][y-1]+=0.25*rainRate*dt;
            water[x+1][y-1]+=0.25*rainRate*dt;
            water[x-1][y+1]+=0.25*rainRate*dt;
            water[x+1][y+1]+=0.25*rainRate*dt;
            
            totalEvaporation-=4*rainRate*dt;
          }
          // Snow.
          else
          {
            snow[x][y]+=rainRate*dt;
  
            snow[x-1][y]+=0.5*rainRate*dt;
            snow[x+1][y]+=0.5*rainRate*dt;
            snow[x][y-1]+=0.5*rainRate*dt;
            snow[x][y+1]+=0.5*rainRate*dt;
            
            snow[x-1][y-1]+=0.25*rainRate*dt;
            snow[x+1][y-1]+=0.25*rainRate*dt;
            snow[x-1][y+1]+=0.25*rainRate*dt;
            snow[x+1][y+1]+=0.25*rainRate*dt;
            
            totalEvaporation-=4*rainRate*dt;
          }
        }
      }
    }
    
    // Flow is simulated with the shallow-water model. Then
    // the velocity field and the water surface are updated.
    // Pipe cross-sectional area.
    
    float[][] tmpOutflowFluxL = new float[width][height];
    float[][] tmpOutflowFluxR = new float[width][height];
    float[][] tmpOutflowFluxT = new float[width][height];
    float[][] tmpOutflowFluxB = new float[width][height];
    float[][] tmpSediment = new float[width][height];
    
    float l = 1;
    float A = l*l;
    float g = 9.81;
    // Left
    for(int x = 1; x<width; x++)
    {
      for(int y = 0; y<height; y++)
      {
        float dh = world[x][y]+water[x][y]-world[x-1][y]-water[x-1][y];
        tmpOutflowFluxL[x][y]=max(0, outflowFluxL[x][y]+dt*A*g*dh/l);
      }
    }
    // Right
    for(int x = 0; x<width-1; x++) 
    {
      for(int y = 0; y<height; y++)
      {
        float dh = world[x][y]+water[x][y]-world[x+1][y]-water[x+1][y];
        tmpOutflowFluxR[x][y]=max(0, outflowFluxR[x][y]+dt*A*g*dh/l);
      }
    }
    // Top
    for(int x = 0; x<width; x++)
    {
      for(int y = 1; y<height; y++)
      {
        float dh = world[x][y]+water[x][y]-world[x][y-1]-water[x][y-1];
        tmpOutflowFluxT[x][y]=max(0, outflowFluxT[x][y]+dt*A*g*dh/l);
      }
    }
    // Bottom
    for(int x = 0; x<width; x++)
    {
      for(int y = 0; y<height-1; y++)
      {
        float dh = world[x][y]+water[x][y]-world[x][y+1]-water[x][y+1];
        tmpOutflowFluxB[x][y]=max(0, outflowFluxB[x][y]+dt*A*g*dh/l);
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
          
          if (outflowFluxL[x][y]<0 || outflowFluxR[x][y]<0 || outflowFluxT[x][y]<0 || outflowFluxB[x][y]<0)
          {
            println("flux < 0");
          }
        }
      }
    }
    // Water surface and velocity field
    for(int x = 0; x<width; x++)
    {
      for(int y = 0; y<height; y++)
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
        
        float mx = (
          ((x==0?world[x][y]:world[x-1][y]) - world[x][y])
          + ((x==width-1?world[x][y]:world[x+1][y]) - world[x][y])
          )/2;
        float my = (
          ((y==0?world[x][y]:world[x][y-1]) - world[x][y])
          + ((y==height-1?world[x][y]:world[x][y+1]) - world[x][y])
          )/2;
        
        PVector nx = new PVector(1, 0, mx);
        nx.normalize();
        PVector ny = new PVector(0, 1, my);
        ny.normalize();
        PVector n = (nx).cross(ny);
        PVector vup = new PVector(0,0,1);
        float incidence = vup.dot(n);
        
        // Calculate transport capacity
        float waterVelocity = (new PVector(waterVelocityX[x][y], waterVelocityY[x][y])).mag();
        //Terminal water velocity
        float Ktw = 90*sqrt(l);
        if (waterVelocity>Ktw)
        {
          waterVelocity = Ktw;
        }
        
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
        // Suspended sediment is transported by the velocity field.
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
        
        // Run the evaporation/precipitation part of the sim?
        if (waterCycle)
        {
          // Water decreases due to evaporation.
          // Evaporation constant
          float Ke = 0.00005;
          float evaporation = water[x][y]*(Ke*dt);
          totalEvaporation+=evaporation;
          water[x][y]-=evaporation;
          if (water[x][y] < 0.0001)
          {
            totalEvaporation+=water[x][y];
            water[x][y]=0;
          }
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
            //snow[x][y]=0;
          }
          if(melt!=0)
          {
            //println("snow "+snow[x][y]);
            //println("melt "+melt);
          }
        }
      }
    }
    for(int x = 0; x<width; x++)
    {
      for(int y = 0; y<height; y++)
      {
        suspendedSediment[x][y] = tmpSediment[x][y];
      }
    }
  }
}

World world = null;
WorldSnapShot snapShot = new WorldSnapShot(null, null, null);
Thread t = null;
PFont font;
void setup() {
  size(600, 400);
  background(0);
      
  world = new World(width, height);
  font = createFont("Arial Bold",48);
  println("Rendering");
  //noLoop();
  
  t = new Thread(){
    public void run()
    {
      for(;;)
      {
        world.step();
        delay(1);
      }
    }
  };
  t.start();
    
}

void draw()
{
  world.getSnapShot(snapShot);
  
  for(int x = 0; x<width; x++)
  {
    for(int y = 0; y<height; y++)
    { 
      color c = color(0, 0, 0);
      float landHeight = snapShot.getWorld()[x][y];
      float waterHeight = snapShot.getWater()[x][y];
      float snowHeight = snapShot.getSnow()[x][y];
      if (false)
      {
        c = color(landHeight, landHeight, landHeight);
        if(waterHeight > 0.000001 //*
          && false
        //*/
        )
        { 
          c = color(3*24, 3*34, 89*waterHeight);
        }
        set(x, y, c);
      }
      else
      {
        //println(landheight);
        // If sea
        /*if (world[x][y]<127)
        {
          c = color(2*24, 2*34, 2*89);
        }
        else */if(waterHeight > 0.05)
        { 
          c = color(15, 34*5/(waterHeight+2)+20, 89*5/(waterHeight+3)+60);
        }
        // Else land
        else
        {
          // Degree of shadowedness. 0=no shadow, 1+ = more shadowed
          /*float shadowed = 0;
          color shadowcolor = color(0, 0, 0);
          float shadowdistance = 100000;
          for(int i = Math.min(x,y)-1; i>=0; i--)
          {
            float distance = sqrt(2*i*i);
            float shadow = ((world[x-i][y-i]-world[x][y])-distance);
            if (shadow > shadowed)
            {
              shadowed = shadow; shadowdistance = distance;
            }
          }
          if(shadowed > 0 && false)
          {
            shadowed=shadowed*10/shadowdistance;
            float sambient = Math.min(shadowdistance*3, 255);
            shadowcolor = color(0.5*150/(1+shadowed)+0.15*sambient, 0.6*237/(1+shadowed)+0.2*sambient, 0.7*126/(1+shadowed)+0.35*sambient);
          }*/
          // Not an edge pixel?
          if(x>1 && y>1 && x<(width-1) && y<(height-1))
          {
            // Calculate slope 
            float mx = ((float)1.0/255*(snapShot.getWorld()[x-1][y] - snapShot.getWorld()[x][y]) + (snapShot.getWorld()[x+1][y] - snapShot.getWorld()[x][y]))/2.0;
            float my = ((float)1.0/255*(snapShot.getWorld()[x][y-1] - snapShot.getWorld()[x][y]) + (snapShot.getWorld()[x][y+1] - snapShot.getWorld()[x][y]))/2.0;
            PVector nx = new PVector(1, 0, mx);
            nx.normalize();
            PVector ny = new PVector(0, 1, my);
            ny.normalize();
            PVector n = (nx).cross(ny);
            PVector sun = new PVector(-1,-1,0.5);
            sun.normalize();
            //n*l lighting
            float incidence = sun.dot(n);
            // c=n*l+a
            float Kambient = 0.4;
            if(incidence>0)
            {
              c = color(max(180*incidence+snowHeight*150, Kambient*15), max(146*incidence-0.4*landHeight+140+snowHeight*150, Kambient*60), max(146*incidence+snowHeight*150, Kambient*140));
            }
            else
            {
              c = color(max(15*-incidence+snowHeight*150, Kambient*15), max(23*-incidence+snowHeight*150, Kambient*60), max(12*-incidence+snowHeight*150, Kambient*140));
            }
            // incidence = 0;
            //println(incidence);
            //if(shadowcolor != color(0, 0, 0))
            ////{
            //  c = color((red(c)+red(shadowcolor))/2, (green(c)+green(shadowcolor))/2, (blue(c)+blue(shadowcolor))/2);
            //}
          }
        }
        set(x, y, c);
      }
    }
  }
  //println("done rendering");
  textFont(font,36);
  // white float frameRate
  fill(0);
  text(frameRate,23,33);
  fill(255);
  text(frameRate,20,30);
}

;
