package Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Probabilities {
    @JsonProperty("standard_symbols")
    private List<SymbolProbabilities> standardSymbols;
    @JsonProperty("bonus_symbols")
    private SymbolProbabilities bonusSymbols;

    // Getters and Setters
    public List<SymbolProbabilities> getStandardSymbols() {
        return standardSymbols;
    }

    public void setStandardSymbols(List<SymbolProbabilities> standardSymbols) {
        this.standardSymbols = standardSymbols;
    }

    public SymbolProbabilities getBonusSymbols() {
        return bonusSymbols;
    }

    public void setBonusSymbols(SymbolProbabilities bonusSymbols) {
        this.bonusSymbols = bonusSymbols;
    }
}
