package org.example.demo.model;

public class TrainInfo {
    public static String generate(String trainId, String lat, String lon,
                                  int speed, boolean emergency, int speedLimit, String signal,
                                  int platforms, int occupied, int delay) {
        return String.format(
                "[Train ID: %s] Status: In service | Position: Latitude %s, Longitude %s | Speed: %d km/h\n" +
                        "[Alert] Speed limit set to %d km/h%s\n" +
                        "[Station Status] Signal: %s | Available Platforms: %d, Occupied: %d | Delay: %d seconds\n" +
                        "[Control Request] Speed report requested\n\n",
                trainId, lat, lon, speed, speedLimit,
                emergency ? " due to emergency" : ", no emergency",
                signal, platforms, occupied, delay
        );
    }
}
