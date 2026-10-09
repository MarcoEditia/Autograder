import java.util.ArrayList;
import java.util.List;

public class Baseline {

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
        List<Integer> primes = new ArrayList<>();
        int count = 0;
        for (int num = 2; num <= limit; num++) {
            if (isPrime(num)) {
                primes.add(num);
                count++;
            }
        }
        System.out.println("Total primes found: " + count);
        return primes;
    }

    // Finds prime factors of a number
    public static List<Integer> getPrimeFactors(int number) {
        List<Integer> factors = new ArrayList<>();
        int temp = number;
        for (int factor = 2; factor <= temp; factor++) {
            while (temp % factor == 0) {
                factors.add(factor);
                temp = temp / factor;
            }
        }
        return factors;
    }

    public static void main(String[] args) {
        int maxRange = 50;
        List<Integer> resultList = findPrimes(maxRange);
        System.out.println("Primes up to " + maxRange + ": " + resultList);

        int targetVal = 84;
        List<Integer> factorList = getPrimeFactors(targetVal);
        System.out.println("Factors of " + targetVal + ": " + factorList);
    }
}