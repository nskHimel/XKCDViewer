package com.himel.xkcdviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.himel.xkcdviewer.downloader.*
import com.himel.xkcdviewer.zoomable.ZoomableImage
import kotlinx.coroutines.*
import kotlin.random.Random

@ExperimentalMaterialApi
class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val emptyComic = Comic(
            "",
            0,
            "",
            "",
            "",
            false
        )

        val emptyImage = ImageBitmap(1, 1)

        setContent {
            var firstLoaded by remember { mutableStateOf(false) }
            var loading by remember { mutableStateOf(true) }

            val latest: MutableState<Int?> = remember { mutableStateOf(null) }
            val comicNum: MutableState<Int?> = remember { mutableStateOf(null) }

            var comic by remember { mutableStateOf(emptyComic) }
            var bitmap by remember { mutableStateOf(emptyImage) }

            val composableScope = rememberCoroutineScope()
            val job: MutableState<Job?> = remember { mutableStateOf(null) }

            val bottomSheetPeekHeight = 90.dp

            val loader = { index: Int? ->
                job.value?.cancel()

                comic = emptyComic
                bitmap = emptyImage
                loading = true

                job.value = composableScope.launch(Dispatchers.IO) {
                    val fetchedComic = ComicDownloader.fetch(index)

                    if (fetchedComic != null) {
                        val fetchedImage = ImageDownloader.fetch(fetchedComic.imgURL)

                        withContext(Dispatchers.Main) {
                            if (fetchedImage != null) {
                                if (comicNum.value == null) {
                                    comicNum.value = fetchedComic.num
                                    latest.value = fetchedComic.num
                                }

                                comic = fetchedComic
                                bitmap = fetchedImage.asImageBitmap()

                                loading = false
                            }
                        }
                    }
                }
            }

            MaterialTheme {
                BottomSheetScaffold(
                    sheetContent = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Image(
                                painter = painterResource(id = R.drawable.ic_bottom_sheet_bar),
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        comicNum.value = 1
                                        loader(1)
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_first),
                                        contentDescription = "First"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val value = comicNum.value
                                        val prevValue = if (value != null && value > 1) {
                                            value - 1
                                        } else {
                                            null
                                        }

                                        comicNum.value = prevValue
                                        loader(prevValue)
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_prev),
                                        contentDescription = "Previous"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val latestNum = latest.value
                                        val randValue = if (latestNum != null) {
                                            Random.nextInt(1, latestNum + 1)
                                        } else {
                                            null
                                        }

                                        comicNum.value = randValue
                                        loader(randValue)
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_random),
                                        contentDescription = "Random"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val value = comicNum.value
                                        val latestNum = latest.value
                                        val nextValue = if (value != null && latestNum != null && value < latestNum) {
                                            value + 1
                                        } else {
                                            null
                                        }

                                        comicNum.value = nextValue
                                        loader(nextValue)
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_next),
                                        contentDescription = "Next"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        comicNum.value = null
                                        loader(null)
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_last),
                                        contentDescription = "Last"
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "https://xkcd.com/${comicNum.value?.toString() ?: ""}")
                                            type = "text/plain"
                                        }

                                        val shareIntent = Intent.createChooser(sendIntent, "Share Comic")
                                        if (shareIntent.resolveActivity(packageManager) != null) {
                                            startActivity(shareIntent)
                                        }
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share),
                                        contentDescription = "Share Comic"
                                    )
                                }

                                Spacer(modifier = Modifier.width(48.dp))

                                IconButton(
                                    onClick = {
                                        val viewIntent = Intent().apply {
                                            action = Intent.ACTION_VIEW
                                            data = Uri.parse("https://explainxkcd.com/${comicNum.value?.toString() ?: ""}")
                                        }

                                        val shareIntent = Intent.createChooser(viewIntent, "Explain XKCD")
                                        if (shareIntent.resolveActivity(packageManager) != null) {
                                            startActivity(shareIntent)
                                        }
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_explain),
                                        contentDescription = "Explain Comic (explainxkcd.com)"
                                    )
                                }
                            }

                            Text(
                                text = comic.alt,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                            )
                        }
                    },
                    sheetPeekHeight = bottomSheetPeekHeight,
                    drawerGesturesEnabled = true,
                    sheetElevation = 15.dp
                ) {
                    Surface {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!loading) {
                                Text(
                                    text = comic.title,
                                    style = MaterialTheme.typography.h4,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )

                                Surface(modifier = Modifier.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = bottomSheetPeekHeight)) {
                                    ZoomableImage(
                                        bitmap = bitmap,
                                        contentDescription = comic.transcript
                                    )
                                }
                            } else {
                                Spacer(
                                    Modifier.weight(1f)
                                )
                                CircularProgressIndicator()
                                Spacer(
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (!firstLoaded) {
                        loader(null)
                        firstLoaded = true
                    }
                }
            }
        }
    }
}
