# Задание 3
При помощи функций криптографической библиотеки .NET реализуйте гибридную криптосистему, включающую:
1) генерацию ключевой пары RSA;
2) шифрование и расшифрование документа симметричным криптоалгоритмом;
3) шифрование и расшифрование сеансового ключа симметричного алгоритма при помощи ключей RSA;
4) формирование и проверку цифровой подписи документа.
Полученный шифротекст, открытые ключи должны сохраняться и передаваться через файлы.

## Метод решения

Данная программа реализует защищенную передачу сообщения от Алисы к Бобу с использованием:
- Асимметричного шифрования (RSA) для безопасной передачи ключа
- Симметричного шифрования (AES) для шифрования самого сообщения
- Цифровой подписи (RSA) для подтверждения авторства и целостности

### Работа программы

АЛИСА                                 БОБ
  |                                    |
  | 1. Генерирует RSA ключи            | 1. Генерирует RSA ключи
  | 2. Пишет сообщение в message.txt   |
  |                                    |
  |<---- передача открытого ключа -----|
  |                                    |
  | 3. Генерирует AES ключ             |
  | 4. Шифрует сообщение AES           |
  | 5. Шифрует AES ключ RSA (ключ Боба)|
  | 6. Подписывает сообщение RSA       |
  |    (своим приватным ключом)        |
  |                                    |
  | 7. Сохраняет 3 файла:              |
  |    - encoded (AES шифротекст)      |
  |    - message.key (RSA шифр. ключ)  |
  |    - certificate (подпись)         |
  |                                    |
  |-------- передача файлов ---------->|
  |                                    |
  |                                    | 8. Расшифровывает AES ключ
  |                                    |    (своим приватным RSA)
  |                                    | 9. Расшифровывает сообщение
  |                                    |    (AES ключом)
  |                                    | 10. Проверяет подпись
  |                                    |     (публичным ключом Алисы)
  |                                    | 11. Сохраняет decoded.txt
  |                                    | 12. Выводит результат проверки

## Код решения
```kotlin
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

// пути к папкам и файлам
val mainDirectory = "${System.getProperty("user.dir")}${File.separator}"
val aliceDirectory = "${mainDirectory}Alice${File.separator}"
val bobDirectory = "${mainDirectory}Bob${File.separator}"
val alicePublicKey = "${aliceDirectory}PublicRSA.key"
val alicePrivateKey = "${aliceDirectory}PrivateRSA.key"
val bobPublicKey = "${bobDirectory}PublicRSA.key"
val bobPrivateKey = "${bobDirectory}PrivateRSA.key"
val aliceMessageFile = "${aliceDirectory}message.txt"
val aliceCertificate = "${aliceDirectory}message.certificateToDecode"
val aliceMessageKey = "${aliceDirectory}message.key"
val aliceEncoded = "${aliceDirectory}encoded"
val bobDecoded = "${bobDirectory}decoded.txt"

// функция для записи байтового массива в файл
fun writeBytes(filePath: String, bytes: ByteArray) {
    File(filePath).writeBytes(bytes)
}

// функция для записи строки в файл
fun writeString(filePath: String, str: String) {
    File(filePath).writeText(str)
}

// функция для генерации RSA ключей и записи их в файл
fun generateAndWriteRSAKeys(publicKeyPath: String, privateKeyPath: String) {
    val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()
    
    writeBytes(publicKeyPath, keyPair.public.encoded)
    writeBytes(privateKeyPath, keyPair.private.encoded)
}

// данные классы для хранения ключей
data class RSAKeys(val publicKey: PublicKey, val privateKey: PrivateKey)

// функция для прочтения ключей из файла
fun readRSA(publicKeyPath: String, privateKeyPath: String): RSAKeys {
    val publicBytes = File(publicKeyPath).readBytes()
    val privateBytes = File(privateKeyPath).readBytes()
    
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
    
    return RSAKeys(publicKey, privateKey)
}

// функция для генерации AES ключа
fun generateAes(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)
    return keyGenerator.generateKey()
}

// функция для чтения AES ключа из файла
fun readAes(keyPath: String): SecretKey {
    val keyBytes = File(keyPath).readBytes()
    return SecretKeySpec(keyBytes, "AES")
}

// расширение для шифрования AES в режиме ECB
fun SecretKey.encryptEcb(data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return cipher.doFinal(data)
}

// расширение для расшифровки AES в режиме ECB
fun SecretKey.decryptEcb(encryptedData: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return cipher.doFinal(encryptedData)
}

// расширение для шифрования RSA
fun PublicKey.encrypt(data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return cipher.doFinal(data)
}

// расширение для расшифровки RSA
fun PrivateKey.decrypt(encryptedData: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return cipher.doFinal(encryptedData)
}

// функция для создания цифровой подписи
fun PrivateKey.sign(data: ByteArray): ByteArray {
    Обычная RSA подпись (PKCS#1 v1.5)
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(this)
    signature.update(data)
    return signature.sign()
}

// функция для проверки цифровой подписи
fun PublicKey.verify(data: ByteArray, signatureBytes: ByteArray): Boolean {
    // Обычная RSA подпись (PKCS#1 v1.5)
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(this)
    signature.update(data)
    return signature.verify(signatureBytes)
}

// функция для шифрования сообщения
fun encodeMessage(
    message: String,
    aes: SecretKey,
    alicePrivateKey: PrivateKey,
    bobPublicKey: PublicKey
): Triple<ByteArray, ByteArray, ByteArray> {
    // шифрует сообщение с помощью AES
    val encodedMessage = aes.encryptEcb(message.toByteArray(Charsets.UTF_8))
    // шифрует AES ключ с помощью RSA (публичным ключом Боба)
    val encodedKey = bobPublicKey.encrypt(aes.encoded)
    // создает цифровую подпись (приватным ключом Алисы)
    val cert = alicePrivateKey.sign(message.toByteArray(Charsets.UTF_8))
    
    return Triple(encodedKey, encodedMessage, cert)
}

// функция для расшифровки сообщения
fun decodeMessage(
    alicePublicKey: PublicKey,
    bobPrivateKey: PrivateKey,
    encodedMessage: ByteArray,
    encodedKey: ByteArray
): Pair<ByteArray, Boolean> {
    // расшифровка AES ключа с помощью RSA (приватным ключом Боба)
    val aesKeyBytes = bobPrivateKey.decrypt(encodedKey)
    val aes = SecretKeySpec(aesKeyBytes, "AES")
    
    // расшифровка сообщения с помощью AES
    val originalMessage = aes.decryptEcb(encodedMessage)
    
    return Pair(originalMessage, true)
}

// функция проверки цифровой подписи
fun verifyMessage(alicePublicKey: PublicKey, message: ByteArray, cert: ByteArray): Boolean {
    return alicePublicKey.verify(message, cert)
}

fun main() {
    // создание директорий если они не существуют
    File(aliceDirectory).mkdirs()
    File(bobDirectory).mkdirs()
    
    // генерация RSA ключей
    if (!File(alicePublicKey).exists() || !File(alicePrivateKey).exists()) {
        generateAndWriteRSAKeys(alicePublicKey, alicePrivateKey)
    }
    
    if (!File(bobPublicKey).exists() || !File(bobPrivateKey).exists()) {
        generateAndWriteRSAKeys(bobPublicKey, bobPrivateKey)
    }
    
    // чтение RSA ключей из файлов
    val aliceRSA = readRSA(alicePublicKey, alicePrivateKey)
    val bobRSA = readRSA(bobPublicKey, bobPrivateKey)
    
    // создает тестовое сообщение, если файл не существует
    if (!File(aliceMessageFile).exists()) {
        writeString(aliceMessageFile, "Привет, Боб! Это секретное сообщение.")
    }
    
    // чтение сообщения из файла
    val aliceMessage = File(aliceMessageFile).readText()
    
    println("Оригинальное сообщение: $aliceMessage")
    
    // шифрование и подпись
    val aes = generateAes()
    val (encodedKey, encodedMessage, certificate) = encodeMessage(
        aliceMessage, 
        aes, 
        aliceRSA.privateKey, 
        bobRSA.publicKey
    )
    
    writeBytes(aliceEncoded, encodedMessage)
    writeBytes(aliceMessageKey, encodedKey)
    writeBytes(aliceCertificate, certificate)
    
    println("Сообщение зашифровано и подписано")
    
    // расшифровка и проверка подписи
    val encodedMessageToDecode = File(aliceEncoded).readBytes()
    val encodedKeyToDecode = File(aliceMessageKey).readBytes()
    val certificateToDecode = File(aliceCertificate).readBytes()
    
    val (originalMessage, _) = decodeMessage(
        aliceRSA.publicKey,
        bobRSA.privateKey,
        encodedMessageToDecode,
        encodedKeyToDecode
    )
    
    writeString(bobDecoded, originalMessage.toString(Charsets.UTF_8))
    
    println("Расшифрованное сообщение: ${originalMessage.toString(Charsets.UTF_8)}")
    
    val dataVerified = verifyMessage(
        aliceRSA.publicKey,
        originalMessage,
        certificateToDecode
    )
    
    println("Подпись верна: $dataVerified")
}
```

## Пример вывода
```
Оригинальное сообщение: You are good. See you tomorrow
Сообщение зашифровано и подписано
Расшифрованное сообщение: You are good. See you tomorrow
Подпись верна: true
```