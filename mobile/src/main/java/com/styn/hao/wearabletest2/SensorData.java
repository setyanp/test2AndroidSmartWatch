package com.styn.hao.wearabletest2;

/**
 * Created by Hao on 2016/3/3.
 */
public class SensorData {
    private Long timestamp;
    private String data;

    public Long getTimestamp()
    {
        return timestamp;
    }

    public String getData()
    {
        return data;
    }

    public SensorData(Long timestampParam,String dataParam)
    {
        timestamp = timestampParam;
        data = dataParam;
    }
}
