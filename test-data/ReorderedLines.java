import java.util.ArrayList;
import java.util.List;

public class ReorderedLines {

    // Finds prime factors of a number
    public static List<Integer> getPrimeFactors(int number) {
        int temp = number;
        List<Integer> factors = new ArrayList<>();
        for (int factor = 2; factor <= temp; factor++) {
            while (temp % factor == 0) {
                factors.add(factor);
                // CHANGED: structurally different from temp = temp / factor;
                temp /= factor; 
            }
        }
        return factors;
    }

    // Checks whether a given number is prime
    public static boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    // Finds all prime numbers up to a specified limit
    public static List<Integer> findPrimes(int limit) {
        int count = 0;
        List<Integer> primes = new ArrayList<>();
        for (int num = 2; num <= limit; num++) {
            if (isPrime(num)) {
                // CHANGED: structurally different from count++;
                count += 1; 
                primes.add(num);
            }
        }
        System.out.println("Total primes found: " + count);
        return primes;
    }

    public static void main(String[] args) {
        int targetVal = 84;
        int maxRange = 50;

        List<Integer> factorList = getPrimeFactors(targetVal);
        System.out.println("Factors of " + targetVal + ": " + factorList);

        List<Integer> resultList = findPrimes(maxRange);
        System.out.println("Primes up to " + maxRange + ": " + resultList);
    }
}