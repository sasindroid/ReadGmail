package com.sasi.readgmail.data;

/**
 * Created by sasikumarlakshmanan on 01/05/16.
 */
public class CsvBean {

    public String getDate1() {
        return date1;
    }

    public void setDate1(String date1) {
        this.date1 = date1;
    }

    public String getConnType() {
        return ConnType;
    }

    public void setConnType(String connType) {
        ConnType = connType;
    }

    public String getLat1() {
        return Lat1;
    }

    public void setLat1(String lat1) {
        Lat1 = lat1;
    }

    public String getLon1() {
        return Lon1;
    }

    public void setLon1(String lon1) {
        Lon1 = lon1;
    }

    public String getDownload() {
        return Download;
    }

    public void setDownload(String download) {
        Download = download;
    }

    public String getUpload() {
        return Upload;
    }

    public void setUpload(String upload) {
        Upload = upload;
    }

    public String getLatency() {
        return Latency;
    }

    public void setLatency(String latency) {
        Latency = latency;
    }

    public String getServerName() {
        return ServerName;
    }

    public void setServerName(String serverName) {
        ServerName = serverName;
    }

    public String getInternalIp() {
        return InternalIp;
    }

    public void setInternalIp(String internalIp) {
        InternalIp = internalIp;
    }

    public String getExternalIp() {
        return ExternalIp;
    }

    public void setExternalIp(String externalIp) {
        ExternalIp = externalIp;
    }

    String date1,ConnType,Lat1,Lon1,Download,Upload,Latency,ServerName,InternalIp,ExternalIp;



}
