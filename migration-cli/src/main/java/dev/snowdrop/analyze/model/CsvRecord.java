package dev.snowdrop.analyze.model;

import com.opencsv.bean.CsvBindByPosition;

/**
 * Model class for CSV records from OpenRewrite datatables
 */
public class CsvRecord {

    @CsvBindByPosition(position = 0)
    private String matchId;

    @CsvBindByPosition(position = 1)
    private String pattern;

    @CsvBindByPosition(position = 2)
    private String symbol;

    @CsvBindByPosition(position = 3)
    private String type;

    // Additional fields can be added as needed
    @CsvBindByPosition(position = 4)
    private String additionalInfo1;

    @CsvBindByPosition(position = 5)
    private String additionalInfo2;

    // Default constructor
    public CsvRecord() {
    }

    // Getters and setters
    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAdditionalInfo1() {
        return additionalInfo1;
    }

    public void setAdditionalInfo1(String additionalInfo1) {
        this.additionalInfo1 = additionalInfo1;
    }

    public String getAdditionalInfo2() {
        return additionalInfo2;
    }

    public void setAdditionalInfo2(String additionalInfo2) {
        this.additionalInfo2 = additionalInfo2;
    }

    @Override
    public String toString() {
        return "CsvRecord{" + "matchId='" + matchId + '\'' + ", pattern='" + pattern + '\'' + ", symbol='" + symbol
                + '\'' + ", type='" + type + '\'' + '}';
    }
}