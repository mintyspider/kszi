# Отчет 1

## Задание 1.6

Напишите программу шифрования и расшифрования алгоритмом RSA. Рекомендуется использовать библиотеку для работы с длинными числами. В случае применения этой библиотеки разрешается использовать функции сложения, вычитания, умножения, целочисленного деления, вычисления остатка от деления. Функции возведения числа в степень, нахождения наибольшего общего делителя, обратного элемента в мультипликативной группе вычетов, генерации простого числа реализовать самостоятельно. Для ускорения вычислений использовать китайскую теорему об остатках. Выполняемые функции программы:

1) генерация пары открытый/закрытый ключ, при этом число е задается пользователем;
2) шифрование данных (целого числа);
3) расшифрование шифртекста (целого числа).

## Метод решения

Для решения задачи создана программа на языке Kotlin, использующая алгоритм RSA. Для проверки, является ли число простым, используется тест Миллера-Рабина. Для поиска наибольшего общего делителя применяется расширенный алгоритм Евклида.

Чтобы создать RSA-ключи, нужно выполнить следующие шаги:

1. Выбрать два разных простых числа p и q.
2. Умножить эти числа и получить модуль n = p * q.
3. Найти значение функции Эйлера для числа n: ф(n) = (p-1) * (q-1).
4. Выбрать целое число e (1 < e < ф(n)). Обычно это простое число, которое легко делится на 2, например, 17, 257 или 65537. Это помогает быстрее шифровать сообщения.
5. Найти число d, которое при умножении на e дает 1 по модулю ф(n). Это число d называется секретной экспонентой. Его можно найти с помощью расширенного алгоритма Евклида.
6. Пара чисел (e, n) будет открытым ключом RSA. Ее можно показать другим людям.
7. Пара чисел (d, n) будет закрытым ключом RSA. Ее нужно хранить в секрете.

RSA можно использовать для шифрования и расшифрования сообщений, а также для создания цифровой подписи.

### Шифрование

Чтобы зашифровать сообщение с помощью открытого ключа, нужно выполнить следующие шаги:

1. Взять открытый ключ (e, n).
2. Взять сообщение m.
3. Зашифровать сообщение с помощью формулы: c = E(m) = m^e mod n.

Чтобы расшифровать сообщение, нужно выполнить следующие шаги:

1. Взять закрытый ключ (d, n).
2. Взять зашифрованное сообщение c.
3. Расшифровать сообщение с помощью формулы: m = D(c) = c^d mod n.

## Код решения

```kotlin
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
```

## Пример вывода
```bash
============================================================
RSA ШИФРОВАНИЕ
============================================================

--- Шаг 1: Ввод открытой экспоненты ---
Введи e (например, 65537): 17

--- Шаг 2: Генерация ключей ---
Открытый ключ: e=17, n=15979183283785643807310429090862982811127292670307088193758584200139008720336008311376362544142976227617464669202772255687331755955729474590728727399461
Закрытый ключ: d=2819855873609231260113605133681702849022463412407133210663279564730413303582874809567107267683287520019659088471327978990528462886967783074531778123121, n=15979183283785643807310429090862982811127292670307088193758584200139008720336008311376362544142976227617464669202772255687331755955729474590728727399461
Секретные простые числа: p=32560300535169952933419855427525904174173210467845979368638073406157260937313, q=490756627584741004260425411870597024407164273157820227607297095558057097797

--- Шаг 3: Работа программы ---

Выберите действие:
1) Зашифровать число
2) Расшифровать число
3) Выход
Ваш выбор: 1
Введите число для шифрования: 42
Зашифрованное: 3937657486715347520027492352

Выберите действие:
1) Зашифровать число
2) Расшифровать число
3) Выход
Ваш выбор: 2                           
Введите зашифрованное число: 3937657486715347520027492352
Расшифрованное: 42

Выберите действие:
1) Зашифровать число
2) Расшифровать число
3) Выход
Ваш выбор: 3
============================================================
Программа завершена
============================================================
```
