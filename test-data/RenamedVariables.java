import java.util.ArrayList;
import java.util.List;

public class RenamedVariables {

    // Checks whether a given number is prime
    public static boolean isPrime(int value) {
        if (value <= 1) {
            return false;
        }
        for (int divisor = 2; divisor <= Math.sqrt(value); divisor++) {
            if (value % divisor == 0) {
                return false;
            }
        }
        return true;
    }

    // Finds all prime numbers up to a specified limit
    public static List<Integer> findPrimes(int maxLimit) {
        List<Integer> primeNumbers = new ArrayList<>();
        int primeCount = 0;
        for (int currentNum = 2; currentNum <= maxLimit; currentNum++) {
            if (isPrime(currentNum)) {
                primeNumbers.add(currentNum);
                primeCount++;
            }
        }
        System.out.println("Total primes found: " + primeCount);
        return primeNumbers;
    }

    // Finds prime factors of a number
    public static List<Integer> getPrimeFactors(int inputVal) {
        List<Integer> factorListResult = new ArrayList<>();
        int remainderVal = inputVal;
        for (int d = 2; d <= remainderVal; d++) {
            while (remainderVal % d == 0) {
                factorListResult.add(d);
                remainderVal = remainderVal / d;
            }
        }
        return factorListResult;
    }

    public static void main(String[] args) {
        int upperBound = 50;
        List<Integer> primesOutput = findPrimes(upperBound);
        System.out.println("Primes up to " + upperBound + ": " + primesOutput);

        int numToFactor = 84;
        List<Integer> factorsOutput = getPrimeFactors(numToFactor);
        System.out.println("Factors of " + numToFactor + ": " + factorsOutput);
    }
}