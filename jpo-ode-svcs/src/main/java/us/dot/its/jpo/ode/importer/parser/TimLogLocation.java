package us.dot.its.jpo.ode.importer.parser;

/**
 * POJO class for TIM log file location data
 */
public class TimLogLocation {

   private int latitude;
   private int longitude;
   private int elevation;
   private short speed;
   private short heading;

   public TimLogLocation() {
      super();
   }

   public int getLatitude() {
      return latitude;
   }

   public void setLatitude(int latitude) {
      this.latitude = latitude;
   }

   public int getLongitude() {
      return longitude;
   }

   public void setLongitude(int longitude) {
      this.longitude = longitude;
   }

   public int getElevation() {
      return elevation;
   }

   public void setElevation(int elevation) {
      this.elevation = elevation;
   }

   public short getSpeed() {
      return speed;
   }

   public void setSpeed(short speed) {
      this.speed = speed;
   }

   public short getHeading() {
      return heading;
   }

   public void setHeading(short heading) {
      this.heading = heading;
   }
}
