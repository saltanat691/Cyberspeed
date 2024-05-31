import Dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Random rand = new Random();

    public static void main(String[] args) throws IOException {
        String configFilePath = null;
        double betAmount = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config":
                    if (i + 1 < args.length) {
                        configFilePath = args[++i];
                    } else {
                        System.err.println("Missing value for --config");
                        return;
                    }
                    break;
                case "--betting-amount":
                    if (i + 1 < args.length) {
                        try {
                            betAmount = Double.parseDouble(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid value for --betting-amount");
                            return;
                        }
                    } else {
                        System.err.println("Missing value for --betting-amount");
                        return;
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    return;
            }
        }

        if (configFilePath == null) {
            System.err.println("Config file path is required");
            return;
        }
        // Deserialize JSON string to Java object
        ObjectMapper objectMapper = new ObjectMapper();
        Configuration configuration = objectMapper.readValue(new File(configFilePath), Configuration.class);

        String[][] matrix = generateMatrix(configuration.getRows(),
                configuration.getColumns(), configuration.getProbabilities());
        Map<Symbol, Map<String, WinCombination>> winCombinations = getWinCombinations(matrix, configuration);
        Map<String, Symbol> bonusList = winCombinations.isEmpty() ? new HashMap<>() : getBonus(matrix, configuration.getSymbols());
        double reward = winCombinations.isEmpty() ? 0 : calculateReward(betAmount, winCombinations, bonusList);
        //
        Map<String, Symbol> symbols = configuration.getSymbols();
        Map<String, List<String>> appliedWinningCombinations = getAppliedWinningCombinations(symbols, winCombinations);
        var output = new Output(matrix, reward, appliedWinningCombinations, bonusList);
        // Convert to JSON
        ObjectMapper outputMapper = new ObjectMapper();
        try {
            String json = outputMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static Double calculateReward(double betAmount, Map<Symbol, Map<String, WinCombination>> winCombinations,
                                         Map<String, Symbol> bonusList){
        double reward = 0;
        //calculate all standard_symbol rewards
        for(var symbol : winCombinations.entrySet()){
            double result = betAmount*symbol.getKey().getRewardMultiplier();
            for (var combination : symbol.getValue().values()) {
                result*=combination.getRewardMultiplier();
            }
            reward += result;
        }
        //add bonus_symbol rewards
        for(var symbol : bonusList.values()){
            switch(symbol.getImpact()){
                case "multiply_reward": reward *= symbol.getRewardMultiplier();
                break;
                case "extra_bonus": reward += symbol.getExtra();
                break;
                default: break;
            }
        }
        return reward;
    }

    public static String[][] generateMatrix(int rows, int columns, Probabilities probabilities) {
        String[][] matrix = new String[rows][columns];
        for (SymbolProbabilities cellProbability: probabilities.getStandardSymbols()){
            RandomStringGenerator generator = new RandomStringGenerator(cellProbability.getSymbols());
            matrix[cellProbability.getRow()][cellProbability.getColumn()] = generator.getRandomString();
        }
        addRandomBonusSymbol(matrix, rows, columns, probabilities.getBonusSymbols());
        return matrix;
    }

    public static void addRandomBonusSymbol(String[][] matrix, int rows, int columns, SymbolProbabilities bonusProbabilities) {
        RandomStringGenerator generator = new RandomStringGenerator(bonusProbabilities.getSymbols());
        int row = rand.nextInt(rows);
        int column = rand.nextInt(columns);
        matrix[row][column] = generator.getRandomString();
    }

    public static Map<Symbol, Map<String, WinCombination>> getWinCombinations(String[][] matrix, Configuration configuration) {
        Map<Symbol, Map<String, WinCombination>> symbolWinCombinations = new HashMap<>();
        Map<String, Symbol> symbols = configuration.getSymbols().entrySet().stream()
                .filter(x-> x.getValue().getType().equals("standard"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (int i = 0; i < configuration.getRows(); i++) {
            for (int j = 0; j < configuration.getColumns(); j++) {
                String symbol = matrix[i][j];
                if(symbols.containsKey(symbol)){
                    Map<String, WinCombination> winCombinationMap = new HashMap<>();
                    //Find all same_symbols group wins
                    winCombinationMap.putAll(getSameSymbols(symbol, matrix, configuration.getWinCombinations()));
                    //Find all linear_symbols wins
                    winCombinationMap.putAll(getLinearSymbols(symbol, matrix, i, j, configuration.getWinCombinations()));
                    //Create or Add to existing Symbol-WinCombinations map
                    if (!winCombinationMap.isEmpty()){
                        var old = symbolWinCombinations.getOrDefault(symbols.get(symbol), new HashMap<>());
                        old.putAll(winCombinationMap);
                        symbolWinCombinations.put(symbols.get(symbol), new HashMap<>(old));
                    }
                }
            }
        }
        return symbolWinCombinations;
    }

    public static Map<String, WinCombination> getSameSymbols(String symbol, String[][] matrix,
                                                             Map<String, WinCombination> combinations) {
        int count = (int) Arrays.stream(matrix).flatMap(Arrays::stream).filter(symbol::equals).count();
        return combinations.entrySet().stream()
                .filter(win -> (win.getValue().getCount() == count && win.getValue().getGroup().equals("same_symbols")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, WinCombination> getLinearSymbols(String symbol, String[][] matrix, int row, int column,
                                                               Map<String, WinCombination> combinations) {

        Map<String, WinCombination> result = new HashMap<>();
        var checkCombinations = combinations.entrySet().stream()
                .filter(win -> (win.getValue().getWhen().equals("linear_symbols")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (var combination: checkCombinations.entrySet()){
            for (List<String> area : combination.getValue().getCoveredAreas()) {
                if (area.contains(row + ":" + column)) {
                    if (checkArea(matrix, area, symbol)){
                        result.put(combination.getKey(), combination.getValue());
                    }
                }
            }
        }
        return result;
    }

    private static boolean checkArea(String[][] matrix, List<String> area, String symbol) {
        for (String coordinates: area) {
            String[] parts = coordinates.split(":");
            int row = Integer.parseInt(parts[0]);
            int column = Integer.parseInt(parts[1]);
            if (!Objects.equals(matrix[row][column], symbol)) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Symbol> getBonus(String[][] matrix, Map<String, Symbol> symbols) {
        Map<String, Symbol> bonusSymbols = symbols.entrySet().stream()
                .filter(x-> x.getValue().getType().equals("bonus"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Arrays.stream(matrix)
                .flatMap(Arrays::stream)
                .filter(bonusSymbols::containsKey)
                .collect(Collectors.toMap(key -> key, bonusSymbols::get));
    }

    public static Map<String, List<String>> getAppliedWinningCombinations(Map<String, Symbol> symbols,
                                                                          Map<Symbol, Map<String, WinCombination>> winCombinations) {
        return symbols.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<String> wins = new ArrayList<>(winCombinations.getOrDefault(entry.getValue(), Collections.emptyMap())
                            .keySet());
                    return new AbstractMap.SimpleEntry<>(key, wins);
                })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

}
