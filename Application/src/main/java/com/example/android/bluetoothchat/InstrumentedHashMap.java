package com.example.android.bluetoothchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InstrumentedHashMap {
    private final int maxSize;
    private HashMap<String, byte[]> byteArrayImage;
    private ArrayList<String> keyList = new ArrayList<String>();

    public InstrumentedHashMap(int maxSize) {
        byteArrayImage = new HashMap<>();
        this.maxSize = maxSize;
    }

    public byte[] get(String key){
        return byteArrayImage.get(key);
    }

    public void put(String key, byte[] value) {
        if(byteArrayImage.size() < maxSize){
            byteArrayImage.put(key, value);
            keyList.add(key);
        } else {
            byteArrayImage.remove(keyList.get(0));
            keyList.remove(0);
        }
    }

    public void clear(){
        byteArrayImage.clear();
        keyList.clear();
    }

    public int size(){
        return byteArrayImage.size();
    }
}