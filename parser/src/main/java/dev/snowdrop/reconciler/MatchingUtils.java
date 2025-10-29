package dev.snowdrop.reconciler;


import java.util.UUID;

public class MatchingUtils {

    public static UUID generateUID() {
        return UUID.randomUUID();
    }

    public static void main(String[] args) {
        System.out.printf("UUID: %s \n", generateUID() );
    }
}