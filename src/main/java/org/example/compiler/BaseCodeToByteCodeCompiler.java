package org.example.compiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface BaseCodeToByteCodeCompiler {

    int[] compileBaseCodeToByteCode(String baseCode, String byteCodeFilePath, Map<String, Integer> varsNamesToLoadedVarsStackDisps);

    Map<String, Integer> getPrimitiveDataToLoad(String baseCode);
    Map<String, List<Integer>> getReferenceDataToLoad(String baseCode);

}
