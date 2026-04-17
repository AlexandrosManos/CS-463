package org.example;

public class DocInfo {
    Integer documentID;
    String filePath;
    double documentNorm;

    public DocInfo(int ID, String path, double norm) {
        this.documentID = ID;
        this.filePath = path;
        this.documentNorm = norm;
    }
}
