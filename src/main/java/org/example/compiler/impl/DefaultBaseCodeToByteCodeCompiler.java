package org.example.compiler.impl;

import org.example.compiler.BaseCodeToByteCodeCompiler;
import org.example.utility.StringUtility;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.AddressingTypeConstants.*;

public class DefaultBaseCodeToByteCodeCompiler implements BaseCodeToByteCodeCompiler {

    private static final String TYPE_PLACE_HOLDER = "TYPE";

    private static final String DATA_SCOPE_SEARCH_REGEX = "data:.+end_data";

    private static final String CODE_SCOPE_SEARCH_REGEX = "code:.+end_code";

    private static final String PRIMITIVE_VAR_DECL_SEARCH_REGEX = "[a-zA-Z]+[\\s]*[=][\\s]*[\\d]+";

    private static final String REFERENCE_VAR_DECL_SEARCH_REGEX = "[a-zA-Z]+[\\s]*[=][\\s]*\\[[\\d]+([,]?[\\s]+[\\d]+)*\\]";

    private static final String VAR_TO_NAME_VALUE_SPLIT_REGEX = "[\\s]*[=][\\s]*";

    private static final String REF_VAR_VALUES_SPLIT_REGEX = "[\\s]*[,\\s][\\s]*";

    private static final String BRACKET_REPLACE_REGEX = "[(\\[)(\\])]";

    private static final String INSTRUCTION_SEARCH_REGEX = "[a-zA-Z]+([\\s]+[a-zA-Z\\d]+)*[\\s]*;";

    private static final String INSTRUCTION_DECL_SPLIT_REGEX = "[\\s]+";

    private static final String FUNCTION_SEARCH_REGEX = "TYPE [\\w]+[(][(TYPE [\\w]+)((TYPE [\\w]+)(,TYPE [\\w]+)+)][)]";

    private static final Map<String, Integer> instructionsOpCodes;

    private static final Map<String, Integer> instructionsAddrTypes;

    static {
        instructionsOpCodes = new HashMap<>();
        instructionsOpCodes.put("halt", 0x00);
        instructionsOpCodes.put("push", 0x01);
        instructionsOpCodes.put("pop", 0x02);
        instructionsOpCodes.put("loads", 0x03);
        instructionsOpCodes.put("loop", 0x04);
        instructionsOpCodes.put("endloop", 0x05);
        instructionsOpCodes.put("add", 0x06);
        instructionsOpCodes.put("div", 0x07);

        instructionsAddrTypes = new HashMap<>();
        instructionsAddrTypes.put("halt", NO_OPERAND_ADDR_TYPE);
        instructionsAddrTypes.put("push", STACK_DISP_ADDR_TYPE);
        instructionsAddrTypes.put("pop", STACK_DISP_ADDR_TYPE);
        instructionsAddrTypes.put("loads", DEFAULT_STACK_ADDR_TYPE);
        instructionsAddrTypes.put("loop", STACK_DISP_ADDR_TYPE);
        instructionsAddrTypes.put("endloop", DEFAULT_STACK_ADDR_TYPE);
        instructionsAddrTypes.put("add", DEFAULT_STACK_ADDR_TYPE);
        instructionsAddrTypes.put("div", DEFAULT_STACK_ADDR_TYPE);
    }


    private final Set<FunctionInfo> functions;

    public DefaultBaseCodeToByteCodeCompiler() {
        this.functions = new HashSet<>();
    }

    @Override
    public int[] compileBaseCodeToByteCode(String baseCode, String byteCodeFilePath,
                                            Map<String, Integer> varsNamesToLoadedVarsStackDisps) {
        String compiledBaseCode = StringUtility.findAllRegexMatches(baseCode, CODE_SCOPE_SEARCH_REGEX).iterator().next();
        compiledBaseCode = replaceVarsWithStackDisps(compiledBaseCode, varsNamesToLoadedVarsStackDisps);
        int[] encodedInstructions = encodeInstructions(compiledBaseCode);
        System.out.println("Encoded instructions: " + Arrays.toString(encodedInstructions));
        return encodedInstructions;
    }

    public String replaceVarsWithStackDisps(String baseCode, Map<String, Integer> varsNamesToLoadedVarsStackDisps) {
        String varsReplacedBaseCode = baseCode;
        for (Map.Entry<String, Integer> varNameToLoadedVarStackDisp: varsNamesToLoadedVarsStackDisps.entrySet()) {
            varsReplacedBaseCode =
                    varsReplacedBaseCode.replaceAll("[\\s]+" + varNameToLoadedVarStackDisp.getKey() + "[\\s]+",
                            " " + varNameToLoadedVarStackDisp.getValue().toString() + " ");
            varsReplacedBaseCode =
                    varsReplacedBaseCode.replaceAll("[\\s]+" + varNameToLoadedVarStackDisp.getKey() + "[\\s]*;",
                            " " + varNameToLoadedVarStackDisp.getValue().toString() + ";");
        }
        return varsReplacedBaseCode;
    }

    public int[] encodeInstructions(String codeToCompile) {
        List<String> instructionsDecls = StringUtility.findAllRegexMatches(codeToCompile, INSTRUCTION_SEARCH_REGEX);
        int instructionsNum = instructionsDecls.size();
        int [] encodedInstructions = new int[instructionsNum];
        for (int i = 0; i < instructionsNum; i++) {
            encodedInstructions[i] = encodeInstruction(instructionsDecls.get(i));
        }
        return encodedInstructions;
    }

    public int encodeInstruction(String instructionDecl) {
        instructionDecl = instructionDecl.replace(";", "");
        String[] instructionDeclNameAndOperand = instructionDecl.split(INSTRUCTION_DECL_SPLIT_REGEX);
        String instructionName = instructionDeclNameAndOperand[0];
        int instructionOpCode = instructionsOpCodes.get(instructionName);
        int instructionAddrType = instructionsAddrTypes.get(instructionName);
        int instructionOperand = 0;
        if (instructionDeclNameAndOperand.length > 1) {
            instructionOperand = Integer.valueOf(instructionDeclNameAndOperand[1]);
        }
        int encodedInstruction = (instructionOpCode << 24) | (instructionAddrType << 22) | instructionOperand;
        return encodedInstruction;
    }
    @Override
    public Map<String, Integer> getPrimitiveDataToLoad(String baseCode) {
        String dataCode = StringUtility.findAllRegexMatches(baseCode, DATA_SCOPE_SEARCH_REGEX).iterator().next();
        List<String> primVarsDecls = StringUtility.findAllRegexMatches(dataCode, PRIMITIVE_VAR_DECL_SEARCH_REGEX);
        Map<String, Integer> primVars = new HashMap<>();
        String[] primVarNameAndValue;
        String primVarName;
        String primVarValue;
        for (String refVarDecl : primVarsDecls) {
            primVarNameAndValue = refVarDecl.split(VAR_TO_NAME_VALUE_SPLIT_REGEX);
            primVarName = primVarNameAndValue[0];
            primVarValue = primVarNameAndValue[1];
            primVars.put(primVarName, Integer.valueOf(primVarValue));
        }
        return primVars;
    }

    @Override
    public Map<String, List<Integer>> getReferenceDataToLoad(String baseCode) {
        String dataCode = StringUtility.findAllRegexMatches(baseCode, DATA_SCOPE_SEARCH_REGEX).iterator().next();
        List<String> refVarsDecls = StringUtility.findAllRegexMatches(dataCode, REFERENCE_VAR_DECL_SEARCH_REGEX);
        Map<String, List<Integer>> refVars = new HashMap<>();
        String[] refVarNameAndValue;
        String refVarName;
        String refVarValue;
        for (String refVarDecl : refVarsDecls) {
            refVarNameAndValue = refVarDecl.split(VAR_TO_NAME_VALUE_SPLIT_REGEX);
            refVarName = refVarNameAndValue[0];
            refVarValue = (refVarNameAndValue[1]).replaceAll(BRACKET_REPLACE_REGEX, "");
            refVars.put(refVarName,
                    Arrays.stream(refVarValue.split(REF_VAR_VALUES_SPLIT_REGEX))
                            .map(val -> Integer.valueOf(val))
                            .collect(Collectors.toList()));
        }
        return refVars;
    }

    static class FunctionInfo {
        private String signature;
        private TypeInfo returnType;
        private Map<String, TypeInfo> params;
        private String body;

        public FunctionInfo(String signature, TypeInfo returnType, Map<String, TypeInfo> params, String body) {
            this.signature = signature;
            this.returnType = returnType;
            this.params = params;
            this.body = body;
        }

        public FunctionInfo() {
        }
    }

    static class TypeInfo {
        private String name;
        private int size;

        public TypeInfo(String name, int size) {
            this.name = name;
            this.size = size;
        }
    }

}
