package com.eagle.imagetovideo

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.eagle.audiomuxer.*
import com.eagle.audiopicker.audios.AudioModel
import com.eagle.audiopicker.pickers.SingleAudioPicker
import com.eagle.core.modifiers.TitleTextModifier
import com.eagle.core.modifiers.base.BaseMultiPickerModifier
import com.eagle.imagepicker.images.ImageModel
import com.eagle.imagepicker.pickers.MultiImagePicker
import com.eagle.imagetovideo.databinding.ActivityMainBinding
import com.eagle.videopicker.pickers.MultiVideoPicker
import com.eagle.videopicker.pickers.SingleVideoPicker
import com.eagle.videopicker.videos.VideoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import java.io.File
import java.text.SimpleDateFormat

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private val scope: CoroutineScope= CoroutineScope(Dispatchers.Default)
    private var clickedID = R.id.imageBottomSheetMultiPick
    private var videoFile:File?=null
    private var muxerConfig:MuxerConfiguration?=null
    private var mimeType=MediaFormat.MIMETYPE_VIDEO_AVC
    private var imageList:List<ImageModel>?=null
    private var audioList:Int?=null
    private val videoList:List<VideoModel>?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
        }
        //images

        binding.imageBottomSheetMultiPick.setOnClickListener(this)

        //videos
        binding.singleVideoBottomSheetPick.setOnClickListener(this)
        //audios
        binding.singleAudioBottomSheetPick.setOnClickListener(this)
        //play video
        binding.playVideo.setOnClickListener(this)
        // create video
        binding.makeVideo.setOnClickListener(this)
    }
 private val askForStoragePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    when (clickedID) {
                        //images

                        R.id.imageBottomSheetMultiPick -> {
                            showImageMultiBottomSheetPicker()
                        }


                        //videos
                        R.id.singleVideoBottomSheetPick -> {
                            showSingleVideoBottomSheetPicker()
                        }

                        //audios
                        R.id.singleAudioBottomSheetPick -> {
                            showSingleAudioBottomSheetPicker()
                        }

                        R.id.playVideo ->
                        {
                            videoFile?.run {
                                binding.player.setVideoPath(filesDir.absolutePath)
                                binding.player.start()

                            }
                        }
                        R.id.makeVideo->
                        {
                            basicVideoCreation()
                            setCode(mimeType)
//                            videoFile=initVideoFile()
                        }

                    }
                } else {
                    Log.e("PERMISSION", "NOT ALLOWED!")
                }
            }


    // media Format supporter
    private fun  setCode(codec: String)
    {
        if (isCodecSupported(codec))
        {
            mimeType=codec
            muxerConfig?.mimeType=mimeType
        }else{
            Toast.makeText(this,"AVC Codec Not Supported",Toast.LENGTH_SHORT).show()
        }
    }

    // single audios
    private fun showSingleAudioBottomSheetPicker() {
        SingleAudioPicker.showPicker(this, {
            setupViewHolderTitleText {
                textColor = Color.BLACK
                textPadding = 10 // use dp or sp this is only for demonstration purposes
            }
            setupBaseModifier(
                    loadingIndicatorColor = R.color.minusColor,
                    titleTextModifications = {
                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                        textStyle = TitleTextModifier.TextStyle.ITALIC
                        textColor = Color.BLACK
                        marginBottom = 30 // use dp or sp this is only for demonstration purposes
                        textPadding = 5 // use dp or sp this is only for demonstration purposes
                        textSize = 30f  // use sp this is only for demonstration purposes
                        textString = "Pick an audio"
                    },
                    placeHolderModifications = {
                        resID = R.drawable.ic_image
                    }
            )
        }){
            audioList=it.id.toInt()
        }

    }


    private fun loadAudio(audioModel: AudioModel) {
        Glide.with(this)
                .load(audioModel.loadThumbnail(contentResolver))
                .error(R.drawable.ic_album)
                .into(binding.audio)
        binding.audioTitle.text = audioModel.displayName
        Log.d("AUDIO_PICKED ${audioModel.thumbnail?.isRecycled}", audioModel.toString())
    }

    private fun doSomethingWithAudioList(list: List<AudioModel>) {
        list.forEach {
            Log.d("AUDIO LIST size ${list.size}", it.toString())
        }
    }


    //videos
    private fun showSingleVideoBottomSheetPicker() {
        SingleVideoPicker.showPicker(context = this, onPickedVideo = ::loadVideo)

        SingleVideoPicker.showPicker(this, {
            setupBaseModifier(
                    loadingIndicatorColor = R.color.minusColor,
                    titleTextModifications = {
                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                        textStyle = TitleTextModifier.TextStyle.ITALIC
                        textColor = Color.BLACK
                        marginBottom = 30 // use dp or sp this is only for demonstration purposes
                        textPadding = 5 // use dp or sp this is only for demonstration purposes
                        textSize = 30f  // use sp this is only for demonstration purposes
                        textString = "Pick a video"
                    },
                    placeHolderModifications = {
                        resID = R.drawable.ic_image
                    }
            )
        }, ::loadVideo)

    }

    private fun loadVideo(videoModel: VideoModel) {
        Glide.with(this)
                .load(videoModel.contentUri)
                .into(binding.video)
        Log.d("VIDEO_PICKED", videoModel.toString())
    }

    private fun doSomethingWithVideoList(list: List<VideoModel>) {
        list.forEach {
            Log.d("VIDEO LIST size ${list.size}", it.toString())
        }
    }

    //images

    private fun showImageMultiBottomSheetPicker() {
        MultiImagePicker.showPicker(this, {
            setupBaseMultiPicker(
                    tintForLoadingProgressBar = ContextCompat.getColor(this@MainActivity, R.color.colorPrimaryDark),
                    gravityForSelectAndUnSelectIndicators = BaseMultiPickerModifier.Gravity.TOP_LEFT,
                    titleModifications = {
                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                        textStyle = TitleTextModifier.TextStyle.BOLD_ITALIC
                        textColor = Color.BLACK
                        marginBottom = 30 // use dp or sp this is only for demonstration purposes
                        textPadding = 5 // use dp or sp this is only for demonstration purposes
                        textSize = 30f  // use sp this is only for demonstration purposes
                        textString = "Pick multiple images"
                    },
                    selectIconModifications = {
                        resID = R.drawable.ic_checked
                        tint = Color.BLACK
                    },
                    unSelectIconModifications = {
                        resID = R.drawable.ic_unchecked
                        tint = Color.BLACK
                    },
                    viewHolderPlaceholderModifications = {
                        resID = R.drawable.ic_image
                    }
            )
        }) { list ->
            imageList=list
            doSomethingWithImageList(list)
            //TODO working good

            Log.i("image",list.toString())
        }
    }



    private fun doSomethingWithImageList(list: List<ImageModel>) {
        list.forEach {
            Log.d("IMAGE LIST size ${list.size}", it.toString())
        }
    }



    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //images
        MultiImagePicker.restoreListener(this, ::doSomethingWithImageList)
        //videos
        MultiVideoPicker.restoreListener(this, ::doSomethingWithVideoList)
        //audios
        SingleAudioPicker.restoreListener(this, ::loadAudio)

    }


    // creating the Basic Video
    private fun basicVideoCreation()
    {

        videoFile?.run {
            muxerConfig= MuxerConfiguration(this,600,600,mimeType,3,1F,1500000)
            val muxer=Muxer(applicationContext,muxerConfig!!)

            createVideo(muxer) // usering callbacks 
//            createVideoAsync(muxer) // using Co-routines

        }
    }

    // Coroutine Approach
    private fun createVideoAsync(muxer: Muxer) {
        scope.launch {
            when(val result=muxer.muxAsync(imageList!!,audioList)){
                is MuxingSuccess->{
                    Log.i(TAG, "Video muxed - file path: ${result.file.absolutePath}")
                    onMuxerCompleted() }

                is MuxingError->
                {
                    Log.e(TAG, "There was an error muxing the video")
                }

            }
        }
    }

    private fun onMuxerCompleted() {
        // during the video is prepare what to do .. do here something like download progress bar
        runOnUiThread {
            binding.playVideo.isEnabled=true
        }
    }

    //callback-style approach
    private fun createVideo(muxer: Muxer)
    {
        Log.i("m",muxer.toString())

        muxer.setOnMuxingCompletedListener(object :MuxingCompletionListener{

            override fun onVideoSuccessful(file: File) {

                Log.d(TAG, "Video muxed - file path: ${file.absolutePath}")

                onMuxerCompleted()
            }

            override fun onVideoError(error: Throwable) {
                Log.e(TAG, "There was an error muxing the video")
            }

        })
    }

    override fun onClick(clickedview: View?) {
        clickedview ?: return
        clickedID = clickedview.id
        askForStoragePermission.launch(READ_EXTERNAL_STORAGE)
    }

    private fun initVideoFile():File
    {
        var dir:File =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!dir.exists())
        {
            dir.mkdir()
        }
        if (!dir.exists())
        {
            dir=this.applicationContext.cacheDir
        }
        return File(dir, String.format("photo_movie%s.mp4",SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(System.currentTimeMillis())))
    }
}
