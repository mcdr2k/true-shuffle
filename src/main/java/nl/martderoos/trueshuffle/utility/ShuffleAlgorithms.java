package nl.martderoos.trueshuffle.utility;

import java.util.Random;

class ShuffleAlgorithms {
    private ShuffleAlgorithms() {

    }

    /**
     * Shuffles an integer array in-place in a pseudorandom fashion
     */
    public static void shuffle(int[] array) {
        // Fisher–Yates
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}
