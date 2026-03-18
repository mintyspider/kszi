import java.math.BigInteger
import java.security.SecureRandom

class RSA {
    private val random = SecureRandom()
    private val smallPrimes = generateSmallPrimes(2000)

    // Генерация списка маленьких простых чисел для быстрой проверки делимости
    private fun generateSmallPrimes(limit: Int): List<Int> {
        val sieve = BooleanArray(limit + 1)
        val primes = mutableListOf<Int>()
        for (i in 2..limit) {
            if (!sieve[i]) {
                primes.add(i)
                var j = i * i
                while (j <= limit) {
                    sieve[j] = true
                    j += i
                }
            }
        }
        return primes
    }

    // Быстрое возведение в степень по модулю с использованием рекурсии
    private fun modPow(base: BigInteger, exponent: BigInteger, mod: BigInteger): BigInteger {
        return when {
            exponent == BigInteger.ZERO -> BigInteger.ONE
            exponent == BigInteger.ONE -> base.mod(mod)
            else -> {
                val half = modPow(base, exponent.divide(BigInteger.TWO), mod)
                var result = half.multiply(half).mod(mod)
                if (exponent.mod(BigInteger.TWO) == BigInteger.ONE) {
                    result = result.multiply(base).mod(mod)
                }
                result
            }
        }
    }

    // Проверка на простое число с использованием теста Миллера-Рабина
    private fun isPrime(n: BigInteger, iterations: Int = 256): Boolean {
        if (n < BigInteger.TWO) return false
        if (n == BigInteger.TWO || n == BigInteger.valueOf(3)) return true

        for (p in smallPrimes) {
            val prime = BigInteger.valueOf(p.toLong())
            if (n == prime) return true
            if (n.mod(prime) == BigInteger.ZERO) return false
        }

        var d = n.subtract(BigInteger.ONE)
        var s = 0
        while (d.mod(BigInteger.TWO) == BigInteger.ZERO) {
            d = d.divide(BigInteger.TWO)
            s++
        }

        repeat(iterations) {
            val a = BigInteger(n.bitLength() - 1, random).add(BigInteger.TWO)
            var x = modPow(a, d, n)
            if (x == BigInteger.ONE || x == n.subtract(BigInteger.ONE)) return@repeat

            var continueLoop = false
            for (r in 1 until s) {
                x = modPow(x, BigInteger.TWO, n)
                if (x == BigInteger.ONE) return false
                if (x == n.subtract(BigInteger.ONE)) {
                    continueLoop = true
                    break
                }
            }
            if (!continueLoop) return false
        }

        return true
    }

    // Генерация случайного простого числа указанной длины в битах
    private fun generateRandomPrime(bits: Int): BigInteger {
        while (true) {
            var candidate = BigInteger(bits, random)
            if (candidate.mod(BigInteger.TWO) == BigInteger.ZERO) candidate += BigInteger.ONE
            if (isPrime(candidate)) return candidate
        }
    }

    // Вычисление наибольшего общего делителя (НОД)
    private fun gcd(a: BigInteger, b: BigInteger): BigInteger {
        var x = a
        var y = b
        while (y != BigInteger.ZERO) {
            val temp = y
            y = x.mod(y)
            x = temp
        }
        return x
    }

    // Расширенный алгоритм Евклида для вычисления обратного элемента по модулю
    private fun extendedGCD(a: BigInteger, b: BigInteger): Triple<BigInteger, BigInteger, BigInteger> {
        if (a == BigInteger.ZERO) return Triple(b, BigInteger.ZERO, BigInteger.ONE)
        val (gcd, x1, y1) = extendedGCD(b.mod(a), a)
        val x = y1.subtract(b.divide(a).multiply(x1))
        val y = x1
        return Triple(gcd, x, y)
    }

    // Генерация пары ключей RSA (открытый и закрытый ключ)
    fun generateKeys(e: BigInteger): RSAKeyPair {
        val bits = 256
        while (true) {
            val p = generateRandomPrime(bits)
            val q = generateRandomPrime(bits)
            val n = p * q
            val phi = (p - BigInteger.ONE) * (q - BigInteger.ONE)

            if (gcd(e, phi) == BigInteger.ONE) {
                val (_, d, _) = extendedGCD(e, phi)
                val dPositive = d.mod(phi)
                return RSAKeyPair(
                    publicKey = PublicKey(e, n),
                    privateKey = PrivateKey(dPositive, n, p, q)
                )
            }
        }
    }

    // Шифрование числа с использованием открытого ключа
    fun encrypt(message: BigInteger, publicKey: PublicKey): BigInteger {
        return modPow(message, publicKey.e, publicKey.n)
    }

    // Расшифровка числа с использованием закрытого ключа и Китайской теоремы об остатках
    fun decrypt(ciphertext: BigInteger, privateKey: PrivateKey): BigInteger {
        val p = privateKey.p
        val q = privateKey.q
        val d = privateKey.d
        val n = privateKey.n

        val dp = d.mod(p - BigInteger.ONE)
        val dq = d.mod(q - BigInteger.ONE)

        val m1 = modPow(ciphertext.mod(p), dp, p)
        val m2 = modPow(ciphertext.mod(q), dq, q)

        val (_, invQ, _) = extendedGCD(q, p)
        val (_, invP, _) = extendedGCD(p, q)

        val message = m1 * invQ.mod(p) * q + m2 * invP.mod(q) * p
        return message.mod(n)
    }
}

// Структуры для хранения ключей
data class PublicKey(val e: BigInteger, val n: BigInteger)
data class PrivateKey(val d: BigInteger, val n: BigInteger, val p: BigInteger, val q: BigInteger)
data class RSAKeyPair(val publicKey: PublicKey, val privateKey: PrivateKey)

fun main() {
    println("=".repeat(60))
    println("RSA ШИФРОВАНИЕ")
    println("=".repeat(60))

    val rsa = RSA()

    // Ввод открытой экспоненты или использование значения по умолчанию
    println("\n--- Шаг 1: Ввод открытой экспоненты ---")
    print("Введи e (например, 65537): ")
    val e = readlnOrNull()?.toBigIntegerOrNull() ?: run {
        println("Используется значение по умолчанию: 65537")
        BigInteger.valueOf(65537)
    }

    // Генерация пары ключей
    println("\n--- Шаг 2: Генерация ключей ---")
    val keys = rsa.generateKeys(e)
    println("Открытый ключ: e=${keys.publicKey.e}, n=${keys.publicKey.n}")
    println("Закрытый ключ: d=${keys.privateKey.d}, n=${keys.privateKey.n}")
    println("Секретные простые числа: p=${keys.privateKey.p}, q=${keys.privateKey.q}")

    // Ввод сообщения для шифрования
    println("\n--- Шаг 3: Работа программы ---")
    while (true) {
        println("\nВыберите действие:")
        println("1) Зашифровать число")
        println("2) Расшифровать число")
        println("3) Выход")
        print("Ваш выбор: ")
        when (readlnOrNull()?.trim()) {
            "1" -> {
                print("Введите число для шифрования: ")
                val msg = readlnOrNull()?.toBigIntegerOrNull() ?: continue
                val cipher = rsa.encrypt(msg, keys.publicKey)
                println("Зашифрованное: $cipher")
            }
            "2" -> {
                print("Введите зашифрованное число: ")
                val cipher = readlnOrNull()?.toBigIntegerOrNull() ?: continue
                val decrypted = rsa.decrypt(cipher, keys.privateKey)
                println("Расшифрованное: $decrypted")
            }
            "3" -> {
                println("=".repeat(60))
                println("Программа завершена")
                println("=".repeat(60))
                return
                }
            else -> println("Неверный выбор")
        }
    }
}