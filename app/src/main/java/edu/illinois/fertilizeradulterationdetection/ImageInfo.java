package edu.illinois.fertilizeradulterationdetection;

public class ImageInfo {
    private double longitude;
    private double latitude;
    private String date;
    private String prediction;
    private String note;

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }
}
