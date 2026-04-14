# Реализация протокола Диффи-Хеллмана для генерации совместного ключа

## Метод решения

Задача решается написанием протокола Диффи-Хеллмана на языке Kotlin. Для генерации простых чисел используется тест Миллера-Рабина. Для нахождения первообразного корня используется факторизация функции Эйлера от простого числа.

Протокол Диффи-Хеллмана позволяет двум сторонам согласовать общий секретный ключ, не передавая его явно по незащищенному каналу. Безопасность протокола основана на трудности вычисления дискретного логарифма.

### Генерация общих параметров

1. Генерируется случайное простое число $p$.
2. Выбирается целое число $g$ — первообразный корень по модулю $p$.

### Генерация ключей участниками

1. Каждый участник выбирает случайное секретное число $x$ ($1 < x < p-1$).
2. Каждый участник вычисляет открытый ключ $y = g^x \mod p$.
3. Участники обмениваются открытыми ключами.

### Вычисление общего секрета

1. Алиса вычисляет общий секрет: $K = (y_B)^{x_A} \mod p$
2. Боб вычисляет общий секрет: $K = (y_A)^{x_B} \mod p$

Математически доказывается, что оба участника получают одинаковый результат: $K = g^{x_A \cdot x_B} \mod p$.

## Код решения

```kotlin
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Бинарное возведение числа в степень по модулю
 * Вычисляет (base^exponent) mod modulus
 */
fun powerMod(base: BigInteger, exponent: BigInteger, modulus: BigInteger): BigInteger {
    var result = BigInteger.ONE
    var b = base.mod(modulus)
    var e = exponent
    
    while (e > BigInteger.ZERO) {
        if (e.and(BigInteger.ONE) == BigInteger.ONE) {
            result = (result * b).mod(modulus)
        }
        b = (b * b).mod(modulus)
        e = e.shiftRight(1)
    }
    return result
}

/**
 * Генерация случайного BigInteger в диапазоне [min, max)
 */
fun randomBigInteger(min: BigInteger, max: BigInteger): BigInteger {
    val random = SecureRandom()
    val range = max.subtract(min)
    val bytes = range.toByteArray()
    var result: BigInteger
    
    do {
        random.nextBytes(bytes)
        result = BigInteger(1, bytes)
    } while (result >= range)
    
    return result.add(min)
}

/**
 * Проверка числа на простоту с помощью теста Миллера-Рабина
 */
fun isPrime(n: BigInteger, rounds: Int = 40): Boolean {
    // Проверка тривиальных случаев
    if (n < BigInteger.TWO) return false
    if (n == BigInteger.TWO || n == BigInteger.valueOf(3)) return true
    if (n.mod(BigInteger.TWO) == BigInteger.ZERO) return false
    
    // Представление n-1 как d * 2^s
    var d = n.subtract(BigInteger.ONE)
    var s = 0
    while (d.mod(BigInteger.TWO) == BigInteger.ZERO) {
        d = d.divide(BigInteger.TWO)
        s++
    }
    
    // Проверка нескольких случайных свидетелей простоты
    for (i in 0 until rounds) {
        val a = randomBigInteger(BigInteger.TWO, n.subtract(BigInteger.ONE))
        var x = powerMod(a, d, n)
        if (x == BigInteger.ONE || x == n.subtract(BigInteger.ONE)) continue
        
        var continueLoop = false
        for (r in 0 until s - 1) {
            x = powerMod(x, BigInteger.TWO, n)
            if (x == n.subtract(BigInteger.ONE)) {
                continueLoop = true
                break
            }
        }
        if (continueLoop) continue
        return false
    }
    return true
}

/**
 * Генерация случайного простого числа заданной битности
 */
fun generateRandomPrime(bits: Int): BigInteger {
    val random = SecureRandom()
    
    while (true) {
        var candidate = BigInteger(bits, random)
        candidate = candidate.setBit(bits - 1)  // Старший бит = 1
        candidate = candidate.setBit(0)          // Младший бит = 1 (нечётное)
        
        if (isPrime(candidate)) {
            return candidate
        }
    }
}

/**
 * Разложение числа на простые множители
 */
fun factorize(n: BigInteger): MutableList<BigInteger> {
    var num = n
    val factors = mutableListOf<BigInteger>()
    var i = BigInteger.TWO
    
    while (i * i <= num) {
        if (num.mod(i) == BigInteger.ZERO) {
            factors.add(i)
            while (num.mod(i) == BigInteger.ZERO) {
                num = num.divide(i)
            }
        }
        i = if (i == BigInteger.TWO) BigInteger.valueOf(3) else i.add(BigInteger.TWO)
    }
    
    if (num > BigInteger.ONE) {
        factors.add(num)
    }
    return factors
}

/**
 * Поиск первообразного корня по модулю p
 * Используется факторизация φ(p) = p-1
 */
fun findPrimitiveRoot(p: BigInteger): BigInteger {
    val phi = p.subtract(BigInteger.ONE)
    val factors = factorize(phi)
    
    var g = BigInteger.TWO
    while (g < p) {
        var isRoot = true
        
        // Проверка: g^(phi/qi) mod p != 1 для всех простых делителей qi
        for (factor in factors) {
            if (powerMod(g, phi.divide(factor), p) == BigInteger.ONE) {
                isRoot = false
                break
            }
        }
        
        if (isRoot) {
            return g
        }
        g = g.add(BigInteger.ONE)
    }
    
    return BigInteger.TWO
}

/**
 * Класс, представляющий пользователя в протоколе Диффи-Хеллмана
 */
class User(val name: String) {
    var privateKey: BigInteger = BigInteger.ZERO   // Закрытый ключ
    var publicKey: BigInteger = BigInteger.ZERO    // Открытый ключ
    var sharedSecret: BigInteger = BigInteger.ZERO // Общий секрет
    
    /**
     * Генерация пары ключей (закрытого и открытого)
     */
    fun generateKeys(p: BigInteger, g: BigInteger) {
        // Генерация случайного секретного ключа в диапазоне (1, p-1)
        privateKey = randomBigInteger(BigInteger.ONE.add(BigInteger.ONE), p.subtract(BigInteger.ONE))
        
        // Вычисление открытого ключа y = g^x mod p
        publicKey = powerMod(g, privateKey, p)
        
        println("[$name] Закрытый ключ x: $privateKey")
        println("[$name] Открытый ключ y: $publicKey")
    }
    
    /**
     * Получение открытого ключа от другого пользователя и вычисление общего секрета
     */
    fun receivePublicKeyAndComputeSecret(otherPublicKey: BigInteger, p: BigInteger) {
        println("[$name] Получен открытый ключ собеседника: $otherPublicKey")
        // Вычисление общего секрета: K = (y_собеседника)^x mod p
        sharedSecret = powerMod(otherPublicKey, privateKey, p)
        println("[$name] Вычислен общий секрет K: $sharedSecret")
    }
}

/**
 * Генерация общих параметров p и g для протокола
 */
fun generateSharedParameters(bits: Int = 256): Pair<BigInteger, BigInteger> {
    val p = generateRandomPrime(bits)  // Генерация простого числа p
    val g = findPrimitiveRoot(p)        // Поиск первообразного корня g
    return Pair(p, g)
}

/**
 * Демонстрация протокола Диффи-Хеллмана с малыми числами
 */
fun demonstrateWithSmallNumbers() {
    println("=".repeat(70))
    println("ДЕМОНСТРАЦИЯ ПРОТОКОЛА ДИФФИ-ХЕЛЛМАНА С МАЛЫМИ ЧИСЛАМИ")
    println("=".repeat(70))
    println()
    
    // Использование небольшого простого числа для наглядности
    val p = BigInteger.valueOf(23)
    val g = BigInteger.valueOf(5)
    
    println("Общие параметры:")
    println("  p = $p (простое число)")
    println("  g = $g (первообразный корень по модулю $p)")
    println()
    
    // Секретные ключи участников (для демонстрации используются фиксированные значения)
    val xAlice = BigInteger.valueOf(6)
    val xBob = BigInteger.valueOf(15)
    
    println("Закрытые ключи:")
    println("  xA = $xAlice (Алиса)")
    println("  xB = $xBob (Боб)")
    println()
    
    // Вычисление открытых ключей
    val yAlice = powerMod(g, xAlice, p)
    val yBob = powerMod(g, xBob, p)
    
    println("Открытые ключи:")
    println("  yA = g^xA mod p = 5^$xAlice mod $p = $yAlice")
    println("  yB = g^xB mod p = 5^$xBob mod $p = $yBob")
    println()
    
    // Вычисление общего секрета
    val secretAlice = powerMod(yBob, xAlice, p)
    val secretBob = powerMod(yAlice, xBob, p)
    
    println("Вычисление общего секрета:")
    println("  Алиса: K = yB^xA mod p = $yBob^$xAlice mod $p = $secretAlice")
    println("  Боб: K = yA^xB mod p = $yAlice^$xBob mod $p = $secretBob")
    println()
    
    if (secretAlice == secretBob) {
        println("✓ УСПЕХ! Общий секретный ключ: $secretAlice")
        println("  (математически: g^(xA·xB) mod p = 5^(6·15) mod 23 = 5^90 mod 23 = 2)")
    }
    println()
}

/**
 * Основная демонстрация работы протокола
 */
fun demonstrateDiffieHellman() {
    println("=".repeat(70))
    println("РЕАЛИЗАЦИЯ ПРОТОКОЛА ДИФФИ-ХЕЛЛМАНА")
    println("=".repeat(70))
    println()
    
    // ЭТАП 1: Генерация общих параметров
    println("ЭТАП 1: Генерация общих параметров")
    println("-".repeat(50))
    
    val BIT_LENGTH = 64
    val (p, g) = generateSharedParameters(BIT_LENGTH)
    
    println("Сгенерировано общее простое число p: $p")
    println("Найден первообразный корень g: $g")
    println()
    
    // ЭТАП 2: Генерация ключей участниками
    println("ЭТАП 2: Генерация ключей")
    println("-".repeat(50))
    
    val alice = User("Алиса")
    val bob = User("Боб")
    
    println("--- Генерация ключей Алисы ---")
    alice.generateKeys(p, g)
    println()
    
    println("--- Генерация ключей Боба ---")
    bob.generateKeys(p, g)
    println()
    
    // ЭТАП 3: Имитация обмена открытыми ключами
    println("ЭТАП 3: Обмен открытыми ключами")
    println("-".repeat(50))
    println("        НЕЗАЩИЩЁННЫЙ КАНАЛ СВЯЗИ")
    println("   ┌─────────────┐          ┌─────────────┐")
    println("   │   АЛИСА     │          │     БОБ     │")
    println("   │  yA = ${alice.publicKey} │          │  yB = ${bob.publicKey} │")
    println("   └──────┬──────┘          └──────┬──────┘")
    println("          │                        │")
    println("          │    1) yA → Боб         │")
    println("          │───────────────────────>│")
    println("          │                        │")
    println("          │    2) yB → Алиса       │")
    println("          │<───────────────────────│")
    println()
    
    println("Алиса отправляет Бобу открытый ключ: ${alice.publicKey}")
    println("Боб отправляет Алисе открытый ключ: ${bob.publicKey}")
    println()
    
    // ЭТАП 4: Вычисление общего секрета
    println("ЭТАП 4: Вычисление общего секрета")
    println("-".repeat(50))
    
    println("--- Вычисления Алисы ---")
    alice.receivePublicKeyAndComputeSecret(bob.publicKey, p)
    println()
    
    println("--- Вычисления Боба ---")
    bob.receivePublicKeyAndComputeSecret(alice.publicKey, p)
    println()
    
    // ЭТАП 5: Проверка совпадения
    println("ЭТАП 5: Проверка результата")
    println("-".repeat(50))
    
    if (alice.sharedSecret == bob.sharedSecret) {
        println("✓ УСПЕХ! Общий секрет успешно согласован!")
        println("  Общий секретный ключ K: ${alice.sharedSecret}")
        println()
        println("  Математическое подтверждение:")
        println("  K_A = (yB)^xA mod p = g^(xB·xA) mod p")
        println("  K_B = (yA)^xB mod p = g^(xA·xB) mod p")
        println("  K_A = K_B = g^(xA·xB) mod p")
    } else {
        println("✗ ОШИБКА! Общие секреты не совпадают!")
    }
    
    println()
    println("=".repeat(70))
}

/**
 * Вспомогательная функция для повторения строки
 */
fun String.repeat(count: Int): String {
    val result = StringBuilder()
    for (i in 0 until count) {
        result.append(this)
    }
    return result.toString()
}

/**
 * Главная функция программы
 */
fun main() {
    // Демонстрация с малыми числами для понимания алгоритма
    demonstrateWithSmallNumbers()
    
    println()
    
    // Демонстрация реальной работы протокола
    demonstrateDiffieHellman()
}
```

## Пример вывода программы

```
======================================================================
ДЕМОНСТРАЦИЯ ПРОТОКОЛА ДИФФИ-ХЕЛЛМАНА С МАЛЫМИ ЧИСЛАМИ
======================================================================

Общие параметры:
  p = 23 (простое число)
  g = 5 (первообразный корень по модулю 23)

Закрытые ключи:
  xA = 6 (Алиса)
  xB = 15 (Боб)

Открытые ключи:
  yA = g^xA mod p = 5^6 mod 23 = 8
  yB = g^xB mod p = 5^15 mod 23 = 19

Вычисление общего секрета:
  Алиса: K = yB^xA mod p = 19^6 mod 23 = 2
  Боб: K = yA^xB mod p = 8^15 mod 23 = 2

✓ УСПЕХ! Общий секретный ключ: 2
  (математически: g^(xA·xB) mod p = 5^(6·15) mod 23 = 5^90 mod 23 = 2)

======================================================================
РЕАЛИЗАЦИЯ ПРОТОКОЛА ДИФФИ-ХЕЛЛМАНА
======================================================================

ЭТАП 1: Генерация общих параметров
--------------------------------------------------
Сгенерировано общее простое число p: 14764535671481760629
Найден первообразный корень g: 2

ЭТАП 2: Генерация ключей
--------------------------------------------------
--- Генерация ключей Алисы ---
[Алиса] Закрытый ключ x: 7042815958685837015
[Алиса] Открытый ключ y: 8018539707663436038

--- Генерация ключей Боба ---
[Боб] Закрытый ключ x: 9830741660784331338
[Боб] Открытый ключ y: 12651674669916506696

ЭТАП 3: Обмен открытыми ключами
--------------------------------------------------
        НЕЗАЩИЩЁННЫЙ КАНАЛ СВЯЗИ
   ┌─────────────┐          ┌─────────────┐
   │   АЛИСА     │          │     БОБ     │
   │  yA = 8018539707663436038 │          │  yB = 12651674669916506696 │
   └──────┬──────┘          └──────┬──────┘
          │                        │
          │    1) yA → Боб         │
          │───────────────────────>│
          │                        │
          │    2) yB → Алиса       │
          │<───────────────────────│

Алиса отправляет Бобу открытый ключ: 8018539707663436038
Боб отправляет Алисе открытый ключ: 12651674669916506696

ЭТАП 4: Вычисление общего секрета
--------------------------------------------------
--- Вычисления Алисы ---
[Алиса] Получен открытый ключ собеседника: 12651674669916506696
[Алиса] Вычислен общий секрет K: 11877259839301704

--- Вычисления Боба ---
[Боб] Получен открытый ключ собеседника: 8018539707663436038
[Боб] Вычислен общий секрет K: 11877259839301704

ЭТАП 5: Проверка результата
--------------------------------------------------
✓ УСПЕХ! Общий секрет успешно согласован!
  Общий секретный ключ K: 11877259839301704

  Математическое подтверждение:
  K_A = (yB)^xA mod p = g^(xB·xA) mod p
  K_B = (yA)^xB mod p = g^(xA·xB) mod p
  K_A = K_B = g^(xA·xB) mod p

======================================================================