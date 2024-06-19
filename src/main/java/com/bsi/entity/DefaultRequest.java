package com.bsi.entity;

import java.util.List;

public class DefaultRequest {
    private String tanggal;
    private String username;
    private String desc;
    private List<String> namafile;

    public DefaultRequest(List<String> namafile, String desc, String username, String tanggal) {
        this.namafile = namafile;
        this.desc = desc;
        this.username = username;
        this.tanggal = tanggal;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<String> getNamafile() {
        return namafile;
    }

    public void setNamafile(List<String> namafile) {
        this.namafile = namafile;
    }
}
