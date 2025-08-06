
package com.example.sodam_diary.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import android.net.Uri
import android.media.ExifInterface
import android.graphics.Bitmap
import android.graphics.Matrix

@Composable
fun PreviewScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    val bitmap = loadRotatedBitmap(imageFile)?.asImageBitmap()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "촬영된 이미지 미리보기",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .semantics { contentDescription = "미리보기 이미지" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.semantics { contentDescription = "다시 찍기 버튼" }
                ) {
                    Text("↩️ 다시 찍기")
                }
                Button(
                    onClick = {
                        val encodedPath = Uri.encode(imageFile.absolutePath)
                        navController.navigate("voice_input/$encodedPath")
                    },
                    modifier = Modifier.semantics { contentDescription = "확인 버튼" }
                ) {
                    Text("✅ 확인")
                }
            }
        }
    }
}

fun loadRotatedBitmap(imageFile: File): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
    val exif = ExifInterface(imageFile.absolutePath)

    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
