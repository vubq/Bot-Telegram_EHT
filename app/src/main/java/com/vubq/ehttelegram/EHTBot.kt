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

class EHTBot {

    private var pathData: String = "/storage/emulated/0/AutoEHT/"

    private var isAuto: Boolean = false

    private var autoType: AutoType = AutoType.NULL;

    private var equipmentType: EquipmentType = EquipmentType.NULL

    private var presetB: Boolean = false;

    fun setAutoType(autoType: AutoType) {
        this.autoType = autoType
    }

    fun setEquipmentType(equipmentType: EquipmentType) {
        this.equipmentType = equipmentType
    }

    fun setPresetB(presetB: Boolean) {
        this.presetB = presetB
    }

    private fun String.adbExecution() {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", this))
        process.waitFor()
    }

    private fun String.openApp() {
        "monkey -p $this -c android.intent.category.LAUNCHER 1".adbExecution()
    }

    private fun click(x: Int, y: Int) {
        "input tap $x $y".adbExecution()
    }

    private fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, speed: Int = 500) {
        "input swipe $x1 $y1 $x2 $y2 $speed".adbExecution()
    }

    private fun String.screenCapture() {
        "screencap -p $pathData$this.png".adbExecution()
    }

    private fun adjustBrightness(i: Int) {
        "shell settings put system screen_brightness $i".adbExecution()
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
        "test".screenCapture()
    }

    private fun initAuto() {
        //Mở App Backup
        if (!isAuto) return
        "com.machiav3lli.backup".openApp()

        //Nhấn khôi phục
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(409, 895)

        //Nhấn OK
        if (!isAuto) return
        Thread.sleep(500)
        if (!isAuto) return
        click(460, 656)

        //Mở EHT
        if (!isAuto) return
        Thread.sleep(2000)
        if (!isAuto) return
        "com.superplanet.evilhunter".openApp()

        //Nhấn Touch To Start
        if (!isAuto) return
        Thread.sleep(15000)
        if (!isAuto) return
        click(262, 817)

        //Nhấn đóng
        if (!isAuto) return
        Thread.sleep(27000)
        if (!isAuto) return
        click(262, 729)
    }

    private fun equip(selection: Int, optionB: Boolean) {
        isAuto = true
        Thread {
            while (isAuto) {
                initAuto()

                //Nhấn chọn lò rèn hoặc kim hoàn
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 3 || selection == 4) {
                    //Kim hoàn
                    click(340, 557)
                } else {
                    //Lò rèn
                    click(210, 502)
                }

                //Nhấn chọn loại đồ
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 0 || selection == 4) {
                    //Giáp or dây chuyền
                    click(153, 331)
                }
                if (selection == 1) {
                    //Găng
                    click(202, 334)
                }
                if (selection == 2) {
                    //Giày
                    click(247, 333)
                }

                //Nhấn chọn đồ
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                if (selection == 5) {
                    if (!isAuto) break
                    swipe(203, 594, 203, 359, 500)
                    if (!isAuto) break
                    swipe(203, 594, 203, 359, 500)
                    if (!isAuto) break
                    swipe(203, 594, 203, 359, 500)
                    if (!isAuto) break
                    swipe(203, 594, 203, 359, 500)
                    if (!isAuto) break
                    Thread.sleep(500)
                    if (!isAuto) break
                    //Vũ khí
                    click(265, 580)
                } else {
                    if (!isAuto) break
                    //Các đồ khác
                    click(392, 473)
                }

                //Kéo đầy thanh
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                swipe(135, 734, 485, 734)

                //Nhấn điều chế
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(200, 828)

                //Nhấn tìm thuộc tính
                if (!isAuto) break
                Thread.sleep(7000)
                if (!isAuto) break
                click(270, 326)

                //Nhấn thiết lập sẵn A
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(109, 152)

                //Nhấn tìm kiếm
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(182, 843)

                if (!isAuto) break
                Thread.sleep(2000)
                if (!isAuto) break
                "equip".screenCapture()

                if (!isAuto) break
                cropImage("equip", 59, 307, 348 - 59, 349 - 307)

                if (!isAuto) break
                val comparativeWords = listOf("4 thuoc tinh co hieu luc")
                val isTrue = getTextFromImage("equip", comparativeWords, 1)

                if (!isAuto) break
                if (isTrue) {
                    isAuto = false
                    break
                }

                if (!isAuto) break
                if (!optionB) continue

                //Nhấn xác nhận
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(265, 863)

                //if (!isAuto) break
                //Thread.sleep(500)
                //if (!isAuto) break
                ////Nhấn xác nhận
                //click(527, 2084)

                //Nhấn tìm thuộc tính
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(270, 326)

                //Nhấn thiết lập sẵn B
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(228, 154)

                //Nhấn tìm kiếm
                if (!isAuto) break
                Thread.sleep(500)
                if (!isAuto) break
                click(182, 843)

                if (!isAuto) break
                Thread.sleep(2000)
                if (!isAuto) break
                "equip".screenCapture()

                if (!isAuto) break
                cropImage("equip", 59, 307, 348 - 59, 349 - 307)

                if (!isAuto) break
                val isTrue2 = getTextFromImage("equip", comparativeWords, 2)

                if (!isAuto) break
                if (isTrue2) {
                    isAuto = false
                    break
                }
            }
        }.start()
    }
}