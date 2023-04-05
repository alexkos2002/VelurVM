package org.example.vm;

public interface VelurVM {

    void init(String baseCodeFilePath, String byteCodeFilePath);

    void run();

    void fetch();

    void decode();

    void execute();

}
