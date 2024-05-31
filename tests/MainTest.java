import Dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private Configuration configuration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        configuration = objectMapper.readValue(new File("src/config.json"), Configuration.class);
    }

    @Test
    void testGenerateMatrix() {
        int rows = configuration.getRows();
        int columns = configuration.getColumns();
        Probabilities probabilities = configuration.getProbabilities();
        String[][] matrix = Main.generateMatrix(rows, columns, probabilities);

        assertNotNull(matrix);
        assertEquals(rows, matrix.length);
        assertEquals(columns, matrix[0].length);
    }

    @Test
    void testAddRandomBonusSymbol() {
        int rows = configuration.getRows();
        int columns = configuration.getColumns();
        String[][] matrix = new String[rows][columns];
        SymbolProbabilities bonusProbabilities = configuration.getProbabilities().getBonusSymbols();

        Main.addRandomBonusSymbol(matrix, rows, columns, bonusProbabilities);

        boolean bonusSymbolFound = false;
        for (String[] row : matrix) {
            for (String cell : row) {
                if (bonusProbabilities.getSymbols().containsKey(cell)) {
                    bonusSymbolFound = true;
                    break;
                }
            }
        }
        assertTrue(bonusSymbolFound);
    }

    @Test
    void testCalculateReward() {
        double betAmount = 100;
        Map<Symbol, Map<String, WinCombination>> winCombinations = new HashMap<>();
        Map<String, Symbol> bonusList = new HashMap<>();

        Symbol symbol = new Symbol();
        symbol.setRewardMultiplier(2.0);
        WinCombination winCombination = new WinCombination();
        winCombination.setRewardMultiplier(3.0);

        Map<String, WinCombination> combinationMap = new HashMap<>();
        combinationMap.put("win1", winCombination);
        winCombinations.put(symbol, combinationMap);

        double reward = Main.calculateReward(betAmount, winCombinations, bonusList);
        assertEquals(600, reward);
    }

    @Test
    void testGetWinCombinations() {
        String[][] matrix = {
                {"A", "A", "A"},
                {"B", "A", "B"},
                {"C", "C", "A"}
        };
        Map<Symbol, Map<String, WinCombination>> winCombinations = Main.getWinCombinations(matrix, configuration);

        assertNotNull(winCombinations);
    }

    @Test
    void testGetWinCombinationsEmpty() {
        String[][] matrix = {
                {"A", "A", "+500"},
                {"D", "D", "+500"},
                {"C", "C", "+500"}
        };
        Map<Symbol, Map<String, WinCombination>> winCombinations = Main.getWinCombinations(matrix, configuration);

        assertEquals(winCombinations, new HashMap<>());
    }

    @Test
    void testGetSameSymbols() {
        String[][] matrix = {
                {"A", "A", "A"},
                {"B", "B", "B"},
                {"A", "A", "A"}
        };
        Map<String, WinCombination> combinations = new HashMap<>();
        WinCombination winCombination = new WinCombination();
        winCombination.setCount(6);
        winCombination.setGroup("same_symbols");
        combinations.put("win1", winCombination);

        Map<String, WinCombination> result = Main.getSameSymbols("A", matrix, combinations);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetSameSymbolsIsEmpty() {
        String[][] matrix = {
                {"A", "A", "C"},
                {"B", "C", "B"},
                {"E", "E", "+500"}
        };
        Map<String, WinCombination> combinations = new HashMap<>();
        WinCombination winCombination = new WinCombination();
        winCombination.setCount(3);
        winCombination.setGroup("same_symbols");
        combinations.put("win1", winCombination);

        Map<String, WinCombination> result = Main.getSameSymbols("A", matrix, combinations);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLinearSymbols() {
        String[][] matrix = {
                {"A", "A", "A"},
                {"B", "B", "B"},
                {"A", "A", "A"}
        };
        Map<String, WinCombination> combinations = new HashMap<>();
        WinCombination winCombination = new WinCombination();
        winCombination.setWhen("linear_symbols");
        winCombination.setCoveredAreas(List.of(Arrays.asList("0:0", "0:1", "0:2")));
        combinations.put("win1", winCombination);

        Map<String, WinCombination> result = Main.getLinearSymbols("A", matrix, 0, 0, combinations);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetLinearSymbolsIsEmpty() {
        String[][] matrix = {
                {"A", "D", "E"},
                {"B", "B", "B"},
                {"F", "E", "MISS"}
        };
        Map<String, WinCombination> combinations = new HashMap<>();
        WinCombination winCombination = new WinCombination();
        winCombination.setWhen("linear_symbols");
        winCombination.setCoveredAreas(List.of(Arrays.asList("0:0", "0:1", "0:2")));
        combinations.put("win1", winCombination);

        Map<String, WinCombination> result = Main.getLinearSymbols("A", matrix, 0, 0, combinations);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBonus() {
        String[][] matrix = {
                {"A", "A", "C"},
                {"D", "A", "MISS"},
                {"E", "E", "+1000"}
        };
        Map<String, Symbol> symbols = configuration.getSymbols();

        Map<String, Symbol> bonusList = Main.getBonus(matrix, symbols);
        assertNotNull(bonusList);
        assertEquals(bonusList.size(), 2);
    }

    @Test
    void testGetBonusIsEmpty() {
        String[][] matrix = {
                {"A", "B", "C"},
                {"D", "E", "F"},
                {"G", "H", "I"}
        };
        Map<String, Symbol> symbols = configuration.getSymbols();

        Map<String, Symbol> bonusList = Main.getBonus(matrix, symbols);
        assertNotNull(bonusList);
        assertTrue(bonusList.isEmpty());
    }

    @Test
    void testGetAppliedWinningCombinations() {
        Map<String, Symbol> symbols = configuration.getSymbols();
        Map<Symbol, Map<String, WinCombination>> winCombinations = new HashMap<>();

        Symbol symbol = new Symbol();
        winCombinations.put(symbol, new HashMap<>());

        Map<String, List<String>> appliedWinningCombinations = Main.getAppliedWinningCombinations(symbols, winCombinations);
        assertNotNull(appliedWinningCombinations);
    }
}
