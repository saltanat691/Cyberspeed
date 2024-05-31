package Dto;

import java.util.List;
import java.util.Map;

public class Output {
    private String[][] matrix;
    private double reward;
    private Map<String, List<String>> appliedWinningCombinations;
    private List<String> appliedBonusSymbol;

    // Constructor with parameters
    public Output(String[][] matrix, double reward, Map<String, List<String>> appliedWinningCombinations,
                  Map<String, Symbol> bonusList) {
        this.matrix = matrix;
        this.reward = reward;
        this.appliedWinningCombinations = appliedWinningCombinations;
        this.appliedBonusSymbol = bonusList.keySet().stream().toList();
    }

    public String[][] getMatrix() {
        return matrix;
    }

    public double getReward() {
        return reward;
    }

    public Map<String, List<String>> getAppliedWinningCombinations() {
        return appliedWinningCombinations;
    }

    public List<String> getAppliedBonusSymbol() {
        return appliedBonusSymbol;
    }
}

