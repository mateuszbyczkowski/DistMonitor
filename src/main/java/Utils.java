public class Utils {
    public static void printReversedReceivedData(int[] data) {

        for (int i = data.length - 1; i >= 0; i--) {
            System.out.print(data[i] + " | ");
        }
        System.out.print('\n');
    }

    public static void printReceivedData(int[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.print(data[i] + " | ");
        }
        System.out.print('\n');
    }


}
