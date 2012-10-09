class City
{
  int x, y, population;
  public City(int x, int y, int population)
  {
    this.x = x;
    this.y = y;
    this.population = population;
  }
  public int getX()
  {
    return x;
  }
  public int getY()
  {
    return y;
  }
  public int getPopulation()
  {
    return population;
  }
  public void setPopulation(int population)
  {
    this.population = population;
  }
}
