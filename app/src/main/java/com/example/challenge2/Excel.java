package com.example.challenge2;

import android.Manifest;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import android.content.res.AssetManager;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Excel {

    private static final String TAG = "MyActivity";

    public HashMap<String,ArrayList> getData() throws IOException{

        HashMap<String,ArrayList> data = new HashMap<String,ArrayList>();

        try {

            FileInputStream fileInputStream = new FileInputStream( "beacons_info.xls");

            //AssetManager assetManager = getAssets();

            //InputStream inputStream = assetManager.open("beacon_info.xlsx");

            Workbook workbook= new XSSFWorkbook(fileInputStream);

            // Sheet at index 0. As there are no more sheets
            Sheet sheet = workbook.getSheetAt(0);

            int lastRow = sheet.getLastRowNum();

            for (int i = 0; i <= lastRow; i++){
                ArrayList rest = new ArrayList();
                Row row = sheet.getRow(i);
                Cell beacon_id_Cell = row.getCell(0);
                String beacon_id = beacon_id_Cell.getStringCellValue().trim();
                Cell device_name_Cell = row.getCell(1);
                String device_name = device_name_Cell.getStringCellValue().trim();
                Cell mac_address_Cell = row.getCell(2);
                String mac_address = mac_address_Cell.getStringCellValue().trim();
                Cell longitude_Cell = row.getCell(3);
                String longitude = longitude_Cell.getStringCellValue().trim();
                Cell latitude_Cell = row.getCell(4);
                String longitudlatitudee = latitude_Cell.getStringCellValue().trim();
                Cell floor_Cell = row.getCell(5);
                String floor = floor_Cell.getStringCellValue().trim();

                rest.add(device_name);
                rest.add(mac_address);
                rest.add(longitude);
                rest.add(longitudlatitudee);
                rest.add(floor);

                data.put(beacon_id, rest);
            }

        } catch (FileNotFoundException e) {
            Log.d(TAG,"1 FileNotFound");
            e.printStackTrace();
        }
        return data;
    }
}
