package org.example;

import org.example.vm.VelurVM;
import org.example.vm.impl.StackVelurVM;

public class Main {

    public static final String BASE_CODE_FILE_PATH = "baseCode.txt";
    public static final String BYTE_CODE_FILE_PATH = "byteCode.txt";

    public static void main(String[] args) {
        VelurVM velurVM = new StackVelurVM();
        velurVM.init(BASE_CODE_FILE_PATH, BYTE_CODE_FILE_PATH);
        velurVM.run();
        int a = 0b00000100100000000000000000001111;
        System.out.println(a);
    }
}