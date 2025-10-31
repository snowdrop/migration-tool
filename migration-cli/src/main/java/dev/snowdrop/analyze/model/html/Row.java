package dev.snowdrop.analyze.model.html;

import java.util.List;

public record Row(List<Cell> cells) {
    public List<Cell> getCells() {
        return cells;
    }
}