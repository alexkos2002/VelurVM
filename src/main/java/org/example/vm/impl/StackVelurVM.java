package org.example.vm.impl;

import org.example.compiler.BaseCodeToByteCodeCompiler;
import org.example.compiler.impl.DefaultBaseCodeToByteCodeCompiler;
import org.example.utility.FileUtility;
import org.example.vm.VelurVM;

import java.io.File;
import java.util.*;

import static org.example.AddressingTypeConstants.*;

// INSTRUCTIONS
// halt
// push (op/[opAddr])
// pop ([opAddr])
// loads ([opAddr], size)
// loadh ([opAddr], size)
// mov ([destAddr], [sourceAddr])
// call ([instrAddr])
// ret (disp)
// add
// div
// loop
public class StackVelurVM implements VelurVM {

    private static final int MEMORY_SIZE = 60000;
    private static final int CS = 0;
    private static final int SS = 45000;

    private static final int HS = 30000;

    private static final int OP_CODE_MASK = 0xff000000;
    private static final int ADDR_TYPE_MASK = 0b00000000110000000000000000000000;
    private static final int OPERAND_MASK = 0b00000000001111111111111111111111;
    private boolean isAlive;
    private int[] memory;
    private int ip;
    private int sp;
    private int bp;

    private int opCode;

    private int addrType;
    private int operand;
    private int heapSize;
    private final BaseCodeToByteCodeCompiler compiler;

    private Map<String, Integer> varNamesToAddresses;

    public StackVelurVM() {
        this.isAlive = true;
        this.memory = new int[MEMORY_SIZE];
        this.opCode = 0;
        this.addrType = 0;
        this.operand = 0;
        this.ip = -1;
        this.sp = 0;
        this.bp = 0;
        this.heapSize = 0;
        this.compiler = new DefaultBaseCodeToByteCodeCompiler();
    }

    @Override
    public void init(String baseCodeFilePath, String byteCodeFilePath) {
        final String baseCode = FileUtility.readStringFromFile(baseCodeFilePath);
        Map<String, Integer> varsNamesToLoadedVarsStackDisps = loadData(baseCode);
        bp = sp; // local variable array
        int[] byteCodeInstructions = compiler.compileBaseCodeToByteCode(baseCode, byteCodeFilePath, varsNamesToLoadedVarsStackDisps);
        FileUtility.writeStringToFile(byteCodeFilePath, Arrays.toString(byteCodeInstructions));
        loadByteCodeInstructions(byteCodeInstructions);
    }

    @Override
    public void run() {
        while (isAlive) {
            fetch();
            decode();
            execute();
        }
    }

    @Override
    public void fetch() {
        ip++;
    }

    @Override
    public void decode() {
        int instruction = memory[CS + ip];
        opCode = (instruction & OP_CODE_MASK) >>> 24;
        addrType = (instruction & ADDR_TYPE_MASK) >>> 22;
        operand = instruction & OPERAND_MASK;
        printStack();
    }

    @Override
    public void execute() {
        switch (opCode) {
            case 0x00:
                halt();
                break;
            case 0x01:
                if (addrType == VALUE_ADDR_TYPE) {
                    pushByValue(operand);
                } else if (addrType == STACK_DISP_ADDR_TYPE){
                    pushByAddress(SS + operand);
                }
                break;
            case 0x02:
                if (addrType == NO_OPERAND_ADDR_TYPE) {
                    pop();
                } else if (addrType == STACK_DISP_ADDR_TYPE) {
                    popToAddr(memory[SS + operand]);
                }
                break;
            case 0x03:
                if (addrType == DEFAULT_STACK_ADDR_TYPE) {
                    loads();
                }
                break;
            case 0x04:
                if (addrType == STACK_DISP_ADDR_TYPE) {
                    loop(memory[SS + operand]);
                }
                break;
            case 0x05:
                if (addrType == DEFAULT_STACK_ADDR_TYPE) {
                    endloop();
                }
                break;
            case 0x06:
                if (addrType == DEFAULT_STACK_ADDR_TYPE) {
                    add();
                }
                break;
            case 0x07:
                if (addrType == DEFAULT_STACK_ADDR_TYPE) {
                    div();
                }
                break;
        }
    }

    private void pushByValue(int data) {
        System.out.println("push");
        if (SS + sp < MEMORY_SIZE) {
            memory[SS + sp] = data;
            sp++;
            bp--;
        } else {
            throw new IllegalStateException("Stack overflow. Can't push an element on the stack.");
        }
    }

    private void pushByAddress(int addr) {
        System.out.println("push");
        if (SS + sp < MEMORY_SIZE) {
            memory[SS + sp] = memory[addr];
            sp++;
            bp--;
        } else {
            throw new IllegalStateException("Stack overflow. Can't push an element on the stack.");
        }
    }

    private int pop() {
        System.out.println("pop");
        if (sp > 0) {
            int data = memory[SS + sp - 1];
            sp--;
            bp++;
            return data;
        } else {
            throw new IllegalStateException("Stack is empty. Can't pop an element from the stack.");
        }
    }

    private void popToAddr(int addr) {
        int data = pop();
        movByValue(addr, data);
    }

    /**
     * Is used only for loading reference data throughout VM initialization.
     * @return heap address of the first element of reference type variable after its successful location on heap.
     */

    private int loadh(List<Integer> refVarValueElements) {
        pushByValue(HS + heapSize);
        for (Integer refVarValueElement : refVarValueElements) {
            movByValue(HS + heapSize, refVarValueElement);
        }
        return pop();
    }

    private void loads() {
        System.out.println("loads");
        if (sp > 2) {
            int sourceAddr = pop();
            int size = pop();
            int i = 0;
            for (; i < size; i++) {
                pushByAddress(sourceAddr + i);
            }
        } else {
            throw new IllegalStateException("Can't execute loads. Not enough elements on the stack.");
        }
    }

    private void movByValue(int destAddr, int sourceValue) {
        memory[destAddr] = sourceValue;
        if (destAddr >= HS + heapSize && destAddr < SS + sp) {
            heapSize++;
        }
    }

    private void movByAddr(int destAddr, int sourceAddr) {
        memory[destAddr] = memory[sourceAddr];
        if (destAddr >= HS + heapSize && destAddr < SS + sp) {
            heapSize++;
        }
    }

    private void add() {
        System.out.println("add");
        if (sp > 2) {
            pushByValue( pop() + pop());
        } else {
            throw new IllegalStateException("Can't execute add. Not enough elements on the stack.");
        }
    }

    private void div() {
        System.out.println("div");
        if (sp > 2) {
            int divisible = pop();
            int divisor = pop();
            pushByValue( divisible % divisor);
            pushByValue(divisible / divisor);
        } else {
            throw new IllegalStateException("Can't execute add. Not enough elements on the stack.");
        }
    }

    private void jmp(int goTo) {
        ip = goTo;
    }

    private void loop(int counter) {
        System.out.println("loop");
        // one time
        pushByValue(ip);
        pushByValue(counter - 1);
        printStack();
        sp -= 2;
        bp = 0;
    }

    private void endloop() {
        // multiple times
        System.out.println("endloop");
        if (memory[SS + sp + bp + 1] > 0) {
            (memory[SS + sp + bp + 1])--;
            jmp(memory[SS + sp + bp]);
        } else {
            bp = 0;
        }
    }

    private void halt() {
        System.out.println("halt");
        isAlive = false;
    }

    private Map<String, Integer> loadData(String baseCode) {
        Map<String, Integer> primVars = compiler.getPrimitiveDataToLoad(baseCode);
        System.out.println("Primitive vars loaded: " + primVars);
        Map<String, List<Integer>> refVars = compiler.getReferenceDataToLoad(baseCode);
        System.out.println("Reference vars loaded: " + refVars);
        Map<String, Integer> varsNamesToLoadedVarsStackDisps = new HashMap<>();
        for (Map.Entry<String, List<Integer>> refVar : refVars.entrySet()) {
            int refVarHeapAddr = loadh(refVar.getValue());
            pushByValue(refVarHeapAddr);
            varsNamesToLoadedVarsStackDisps.put(refVar.getKey(), sp - 1);
        }

        for (Map.Entry<String, Integer> primVar : primVars.entrySet()) {
            pushByValue(primVar.getValue());
            varsNamesToLoadedVarsStackDisps.put(primVar.getKey(), sp - 1);
        }

        System.out.println("Memory after data loading: " + Arrays.toString(memory));
        System.out.println(varsNamesToLoadedVarsStackDisps);
        return varsNamesToLoadedVarsStackDisps;
    }

    private void loadByteCodeInstructions(int[] byteCodeInstructions) {
        int instructionsNum = byteCodeInstructions.length;
        for (int i = 0; i < instructionsNum; i++) {
            memory[CS + i] = byteCodeInstructions[i];
        }
    }

    public void printStack() {
        System.out.println("Stack: ");
        for (int i = SS; i < SS + sp; i++) {
            System.out.println(memory[i]);
        }
        System.out.println();
    }

}
