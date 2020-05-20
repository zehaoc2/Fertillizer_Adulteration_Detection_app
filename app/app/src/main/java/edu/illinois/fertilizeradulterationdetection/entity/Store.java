package edu.illinois.fertilizeradulterationdetection.entity;

import java.util.ArrayList;

public class Store {

    private String name, district;
    private ArrayList<Image> images;

    public Store(String name, String district){
        this(name, district, new ArrayList<Image>());
    }

    public Store(String name, String district, ArrayList<Image> images){
        this.name = name;
        this.district = district;
        this.images = images;
    }

    public void addImage(Image img){
        images.add(img);
    }
    public ArrayList<Image> getImages(){ return images; }

    public String getName() { return name; }
    public String getDistrict() { return district; }
}
