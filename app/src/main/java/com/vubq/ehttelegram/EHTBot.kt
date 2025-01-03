package com.vubq.ehttelegram

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vubq.ehttelegram.enums.AutoType
import com.vubq.ehttelegram.enums.EquipmentType
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.tasks.await

class EHTBot(private val telegramBot: TelegramBot) {

    private var pathData: String = "/storage/emulated/0/AutoEHT/"

    private var auto: Boolean = false

    private var autoType: AutoType = AutoType.NULL;

    private var equipmentType: EquipmentType = EquipmentType.NULL

    private var presetB: Boolean = false;

    private var strengthenPlace: Int? = null;

    private var eraseAttributePlace: Int? = null;

    fun setAuto(auto: Boolean) {
        this.auto = auto
        telegramBot.sendMessage("Stop!")
    }

    fun setAutoType(autoType: AutoType) {
        this.autoType = autoType
    }

    fun setEquipmentType(equipmentType: EquipmentType) {
        this.equipmentType = equipmentType
    }

    fun setPresetB(presetB: Boolean) {
        this.presetB = presetB
    }

    fun setStrengthenPlace(strengthenPlace: Int?) {
        this.strengthenPlace = strengthenPlace
    }

    fun setEraseAttributePlace(eraseAttributePlace: Int?) {
        this.eraseAttributePlace = eraseAttributePlace
    }

    private fun String.adbExecution(delay: Long) {
        if (!auto) return
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", this))
        process.waitFor()
        Thread.sleep(delay)
    }

    private fun String.openApp(delay: Long) {
        "monkey -p $this -c android.intent.category.LAUNCHER 1".adbExecution(delay)
    }

    private fun click(x: Int, y: Int, delay: Long) {
        "input tap $x $y".adbExecution(delay)
    }

    private fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, speed: Int = 500, delay: Long) {
        "input swipe $x1 $y1 $x2 $y2 $speed".adbExecution(delay)
    }

    private fun String.screenCapture(delay: Long) {
        "screencap -p $pathData$this.png".adbExecution(delay)
    }

    private fun adjustBrightness(i: Int, delay: Long) {
        "shell settings put system screen_brightness $i".adbExecution(delay)
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun unicode(text: String): String {
        val diacriticMap = mapOf(
            'á' to 'a', 'à' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
            'ă' to 'a', 'ắ' to 'a', 'ằ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a', 'ặ' to 'a',
            'â' to 'a', 'ấ' to 'a', 'ầ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a', 'ậ' to 'a',
            'é' to 'e', 'è' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
            'ê' to 'e', 'ế' to 'e', 'ề' to 'e', 'ể' to 'e', 'ễ' to 'e', 'ệ' to 'e',
            'í' to 'i', 'ì' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
            'ó' to 'o', 'ò' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
            'ô' to 'o', 'ố' to 'o', 'ồ' to 'o', 'ổ' to 'o', 'ỗ' to 'o', 'ộ' to 'o',
            'ơ' to 'o', 'ớ' to 'o', 'ờ' to 'o', 'ở' to 'o', 'ỡ' to 'o', 'ợ' to 'o',
            'ú' to 'u', 'ù' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
            'ư' to 'u', 'ứ' to 'u', 'ừ' to 'u', 'ử' to 'u', 'ữ' to 'u', 'ự' to 'u',
            'ý' to 'y', 'ỳ' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',
            'Đ' to 'D', 'đ' to 'd'
        )
        return text.map { diacriticMap[it] ?: it }.joinToString("")
    }

    private suspend fun recognizeText(image: String): String {
        val inputStream: InputStream = File(image).inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val visionText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return unicode(visionText.text)
    }

    private fun getTextFromImage(
        fileName: String,
        comparativeWords: List<String>,
        bout: Int,
    ): Boolean = runBlocking {
        val text: String = recognizeText("$pathData$fileName.png")

        if (text.isEmpty()) return@runBlocking false

        val exist = comparativeWords.any { text.contains(it, ignoreCase = true) }

//        telegramBot.sendMessage("$text - $exist")

        val textToAppend = "Lần $bout: $text - $exist" + " - " + getCurrentDateTime()

        BufferedWriter(FileWriter("$pathData$fileName.txt", true)).use { writer ->
            writer.write(textToAppend)
            writer.newLine()
        }

        return@runBlocking exist
    }

    private fun cropImage(fileName: String, x: Int, y: Int, width: Int, height: Int) {
        val filePath = "$pathData$fileName.png"
        val inputStream: InputStream = File(filePath).inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)

        val outputFile = File(filePath)
        val outputStream = FileOutputStream(outputFile)
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    fun screenCapture() {
        "test".screenCapture(0)
    }

    private fun initAuto() {
        //Mở App Backup
        "com.machiav3lli.backup".openApp(500)

        //Nhấn khôi phục
        click(406, 776, 500)

        //Nhấn OK
        click(460, 688, 4000)

        //Mở EHT
        "com.superplanet.evilhunter".openApp(14000)

        //Nhấn Touch To Start
        click(257, 817, 25000)

        //Nhấn đóng
        click(265, 727, 500)
    }

    private fun backup() {
        //Mở App Backup
        "com.machiav3lli.backup".openApp(500)

        //Nhấn sao lưu
        click(248, 1604, 500)

        //Nhấn bỏ APK
        click(121, 839, 500)

        //Nhấn OK
        click(939, 1643, 8000)
    }

    fun equip() {
        if (autoType == AutoType.NULL && equipmentType == EquipmentType.NULL) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                //Nhấn chọn lò rèn hoặc kim hoàn
                if (equipmentType == EquipmentType.NECKLACE || equipmentType == EquipmentType.RING) {
                    //Kim hoàn
                    click(351, 586, 500)
                } else {
                    //Lò rèn
                    click(226, 521, 500)
                }

                //Nhấn chọn loại đồ
                if (equipmentType == EquipmentType.ARMOR || equipmentType == EquipmentType.NECKLACE) {
                    //Giáp or dây chuyền
                    click(153, 332, 500)
                }
                if (equipmentType == EquipmentType.GLOVES) {
                    //Găng
                    click(201, 331, 500)
                }
                if (equipmentType == EquipmentType.SHOE) {
                    //Giày
                    click(247, 329, 500)
                }

                //Nhấn chọn đồ
                if (equipmentType == EquipmentType.WEAPON) {
                    swipe(201, 597, 201, 357, 500, 0)
                    swipe(201, 597, 201, 357, 500, 0)
                    swipe(201, 597, 201, 357, 500, 0)
                    swipe(201, 597, 201, 357, 500, 500)

                    //Vũ khí
                    click(263, 578, 500)
                } else {
                    //Các đồ khác
                    click(391, 472, 500)
                }

                //Kéo đầy thanh
                swipe(133, 735, 502, 735, 500, 500)

                //Nhấn điều chế
                click(192 ,825, 5000)

                //Nhấn tìm thuộc tính
                click(268, 323, 500)

                //Nhấn thiết lập sẵn A
                click(112, 152, 500)

                //Nhấn tìm kiếm
                click(177, 846, 1000)

                "Equip".screenCapture(0)

                if (!auto) break
                cropImage("Equip", 57, 301, 344 - 57, 352 - 301)

                if (!auto) break
                val comparativeWords = listOf("4 thuoc tinh co hieu luc")
                val isTrue = getTextFromImage("Equip", comparativeWords, 1)

                if (!auto) break
                if (isTrue) {
                    auto = false
                    telegramBot.sendMessage("Đã tìm thấy trang bị")
                    break
                }

                if (!presetB) continue

                //Nhấn xác nhận
                click(266, 864, 500)

                //Nhấn tìm thuộc tính
                click(268, 323, 500)

                //Nhấn thiết lập sẵn B
                click(231, 150, 500)

                //Nhấn tìm kiếm
                click(177, 846, 1000)

                "Equip".screenCapture(0)

                if (!auto) break
                cropImage("Equip", 57, 301, 344 - 57, 352 - 301)

                if (!auto) break
                val isTrue2 = getTextFromImage("Equip", comparativeWords, 2)

                if (!auto) break
                if (isTrue2) {
                    auto = false
                    telegramBot.sendMessage("Đã tìm thấy trang bị!")
                    break
                }
            }
        }.start()
    }

    fun strengthen() {
        if (autoType == AutoType.NULL && strengthenPlace == null) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                //Nhấn chọn cường hóa thần
                click(535, 990, 500)

                //Nhấn chọn ô
                if (strengthenPlace == 1) click(198, 1746, 500)
                if (strengthenPlace == 2) click(292, 1746, 500)
                if (strengthenPlace == 3) click(389, 1746, 500)
                if (strengthenPlace == 4) click(483, 1746, 500)
                if (strengthenPlace == 5) click(584, 1746, 500)
                if (strengthenPlace == 6) click(678, 1746, 500)
                if (strengthenPlace == 7) click(779, 1746, 500)
                if (strengthenPlace == 8) click(873, 1746, 500)

                "StrengthenMax".screenCapture(0)

                if (!auto) break
                cropImage("StrengthenMax", 109, 1262, 966 - 109, 1360 - 1262)

                if (!auto) break
                val isTrue =
                    getTextFromImage(
                        "StrengthenMax",
                        listOf("Khong the cuong hoa than them nua"),
                        1
                    )

                if (!auto) break
                if (isTrue) {
                    auto = false
                    telegramBot.sendMessage("Cường hóa max!")
                    break
                }

                //Nhấn cường hóa
                click(303, 2002, 7000)

                "Strengthen".screenCapture(0)

                if (!auto) break
                cropImage("Strengthen", 186, 762, 881 - 186, 876 - 762)

                if (!auto) break
                val isTrue2 = getTextFromImage("Strengthen", listOf("Cuong Hoa Thanh Cong"), 1)

                if (!auto) break
                if (isTrue2) backup()
            }
        }.start()
    }

    fun eraseAttribute() {
        if (autoType == AutoType.NULL && eraseAttributePlace == null) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                //Nhấn chọn loại bỏ thuộc tính
                click(535, 990, 500)

                //Nhấn chọn ô
                if (eraseAttributePlace == 1) click(198, 1746, 500)
                if (eraseAttributePlace == 2) click(292, 1746, 500)
                if (eraseAttributePlace == 3) click(389, 1746, 500)
                if (eraseAttributePlace == 4) click(483, 1746, 500)
                if (eraseAttributePlace == 5) click(584, 1746, 500)
                if (eraseAttributePlace == 6) click(678, 1746, 500)
                if (eraseAttributePlace == 7) click(779, 1746, 500)
                if (eraseAttributePlace == 8) click(873, 1746, 500)

                "EraseAttributeMax".screenCapture(0)

                if (!auto) break
                cropImage("EraseAttributeMax", 125, 1490, 817, 87)

                if (!auto) break
                val isTrue =
                    getTextFromImage(
                        "EraseAttributeMax",
                        listOf("Khong co thuoc tinh am de loai bo"),
                        1
                    )

                if (!auto) break
                if (isTrue) {
                    auto = false
                    telegramBot.sendMessage("Loại bỏ hết thuộc tính!")
                    break
                }

                //Nhấn loại bỏ
                click(303, 2002, 7000)

                "EraseAttribute".screenCapture(0)

                if (!auto) break
                cropImage("EraseAttribute", 206, 783, 663, 85)

                if (!auto) break
                val isTrue2 =
                    getTextFromImage(
                        "EraseAttribute",
                        listOf("Da loai bo"),
                        1
                    )

                if (!auto) break
                if (isTrue2) backup()
            }
        }.start()
    }

    fun ridingAnimal() {
        if (autoType == AutoType.NULL) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                click(1005, 910, 5000)

                click(190, 2265, 500)

                click(377, 1672, 500)

                click(274, 1517, 2000)

                "RidingAnimal".screenCapture(0)

                if (!auto) break
                cropImage("RidingAnimal", 98, 754, 266, 68)

                if (!auto) break
                val isTrue2 =
                    getTextFromImage(
                        "RidingAnimal",
                        listOf("LEO S", "BLUBEE S", "PINIA S", "INFERNO S"),
                        1
                    )

                if (!auto) break
                if (isTrue2) backup()
            }
        }.start()
    }

    fun bossChest() {
        if (autoType == AutoType.NULL) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                //Nhấn mở rương
                click(751, 2315, 500)

                //Nhấn tab đồ đặc biệt
                click(666, 1368, 500)

                //Kéo
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)

                //Nhấn vào rương boss
                click(938, 1701, 500)

                //Nhấn sử dụng
//                click(540, 1640, 7000)
                click(277, 1640, 7000)

                //Chụp ảnh
                "BossCheat".screenCapture(0)

                //Cắt ảnh
                if (!auto) break
                cropImage("BossCheat", 389, 1197, 682 - 389, 1299 - 1197)

                if (!auto) break
                val isTrue =
                    getTextFromImage(
                        "BossCheat",
                        listOf("Tinh Chat Vua"),
                        1
                    )

                if (!auto) break
                if (!isTrue) {
                    auto = false
                    telegramBot.sendMessage("Tìm thấy đồ tốt!")
                    break
                }

                for (i in 1..11) {
                    //Item tiếp theo
                    click(877, 1127, 2000)

                    //Chụp ảnh
                    "BossCheat".screenCapture(0)

                    //Cắt ảnh
                    if (!auto) break
                    cropImage("BossCheat", 389, 1197, 682 - 389, 1299 - 1197)

                    if (!auto) break
                    val isTrue2 =
                        getTextFromImage(
                            "BossCheat",
                            listOf("Tinh Chat Vua"),
                            1
                        )

                    if (!auto) break
                    if (!isTrue2) {
                        auto = false
                        telegramBot.sendMessage("Tìm thấy đồ tốt!")
                        break
                    }
                }
            }
        }.start()
    }

    fun character() {
        if (autoType == AutoType.NULL) {
            telegramBot.sendMessage("Command error!")
            return
        }
        auto = true
        telegramBot.sendMessage("Start!")
        Thread {
            while (auto) {
                initAuto()

                //Nhấn mở rương
                click(751, 2315, 500)

                //Nhấn tab đồ đặc biệt
                click(666, 1368, 500)

                //Kéo
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)
                swipe(430, 2181, 430, 1429, 500, 500)

                //Nhấn vào bình
                click(747, 1500, 500)

                //Nhấn sử dụng
                click(355, 1611, 500)

                //Chọn hunter
                click(293, 1011, 500)

                //Nhấn thay đổi
                click(371, 1731, 4000)

                //Nhấn thợ săn
                click(543, 2308, 1000)

                //Chọn thợ săn đầu
                click(121, 1763, 4000)

                //Chụp ảnh
                "Character".screenCapture(0)

                //Cắt ảnh
                if (!auto) break
                cropImage("Character", 202, 404, 867 - 202, 498 - 404)

                if (!auto) break
                val isTrue =
                    getTextFromImage(
                        "Character",
                        listOf("Nhanh Nhen"),
                        1
                    )

                if (!auto) break
                if (isTrue) {
                    auto = false
                    telegramBot.sendMessage("Thuộc tính tốt!")
                    break
                }
            }
        }.start()
    }
}